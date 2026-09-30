// Copyright 2026 Picomisu contributors
// SPDX-License-Identifier: Apache-2.0
package com.android.server.backup.fullbackup;

import static com.android.server.backup.BackupManagerService.DEBUG;
import static com.android.server.backup.BackupManagerService.TAG;
import static com.android.server.backup.UserBackupManagerService.BACKUP_FILE_HEADER_MAGIC;
import static com.android.server.backup.UserBackupManagerService.BACKUP_FILE_VERSION;
import static com.android.server.backup.UserBackupManagerService.SHARED_BACKUP_AGENT_PACKAGE;

import android.app.backup.BackupTransport;
import android.app.backup.IFullBackupRestoreObserver;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.ParcelFileDescriptor;
import android.os.RemoteException;
import android.text.TextUtils;
import android.util.Slog;

import com.android.internal.util.Preconditions;
import com.android.server.backup.BackupAgentTimeoutParameters;
import com.android.server.backup.BackupRestoreTask;
import com.android.server.backup.UserBackupManagerService;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.zip.DeflaterOutputStream;

/**
 * PICO full backup of one package into an unencrypted, uncompressed adb backup stream,
 * requested through {@link android.app.backup.IBackupManager#backup}. The manifest is written
 * without signatures, the agent timeout, the include/exclude paths and keeping the app alive
 * afterwards are taken from the request, and failures and timeouts are reported to the
 * observer.
 */
public class PicoFullBackupTask extends FullBackupTask implements BackupRestoreTask {

    private UserBackupManagerService backupManagerService;
    final AtomicBoolean mLatch;

    ParcelFileDescriptor mOutputFile;
    DeflaterOutputStream mDeflater;
    ArrayList<String> mPackages = new ArrayList<>();
    PackageInfo mCurrentTarget;
    private final int mCurrentOpToken;
    private final BackupAgentTimeoutParameters mAgentTimeoutParameters;
    FullBackupEngine mBackupEngine;
    private long mTimeout;
    private List<String> mIncludePaths;
    private List<String> mExcludePaths;
    private boolean mNotKill;

    public PicoFullBackupTask(UserBackupManagerService backupManagerService,
            List<String> includePaths, List<String> excludePaths, ParcelFileDescriptor fd,
            IFullBackupRestoreObserver observer, String packageName, AtomicBoolean latch,
            long timeout, boolean notKill) {
        super(observer);
        this.backupManagerService = backupManagerService;
        mCurrentOpToken = backupManagerService.generateRandomIntegerToken();
        mIncludePaths = includePaths;
        mExcludePaths = excludePaths;
        mLatch = latch;

        mOutputFile = fd;
        if (!TextUtils.isEmpty(packageName)) {
            mPackages.add(packageName);
        }
        mAgentTimeoutParameters = Preconditions.checkNotNull(
                backupManagerService.getAgentTimeoutParameters(),
                "Timeout parameters cannot be null");
        mTimeout = timeout;
        mNotKill = notKill;
    }

    void addPackagesToSet(TreeMap<String, PackageInfo> set, List<String> pkgNames) {
        for (String pkgName : pkgNames) {
            if (!set.containsKey(pkgName)) {
                try {
                    PackageInfo info = backupManagerService.getPackageManager().getPackageInfo(
                            pkgName,
                            PackageManager.GET_SIGNATURES
                                    | PackageManager.GET_SIGNING_CERTIFICATES);
                    set.put(pkgName, info);
                } catch (NameNotFoundException e) {
                    Slog.w(TAG, "Unknown package " + pkgName + ", skipping");
                }
            }
        }
    }

    private void finalizeBackup(OutputStream out) {
        try {
            // A standard 'tar' EOF sequence: two 512-byte blocks of all zeroes.
            byte[] eof = new byte[512 * 2]; // newly allocated == zero filled
            out.write(eof);
        } catch (IOException e) {
            Slog.w(TAG, "Error attempting to finalize backup stream");
        }
    }

    @Override
    public void run() {
        Slog.i(TAG, "--- Performing full-dataset adb backup ---");

        TreeMap<String, PackageInfo> packagesToBackup = new TreeMap<>();
        FullBackupObbConnection obbConnection = new FullBackupObbConnection(
                backupManagerService);
        obbConnection.establish();  // we'll want this later

        sendStartBackup();

        if (mPackages != null) {
            addPackagesToSet(packagesToBackup, mPackages);
        }

        // flatten the set of packages now so we can explicitly control the ordering
        ArrayList<PackageInfo> backupQueue =
                new ArrayList<>(packagesToBackup.values());
        FileOutputStream ofstream = new FileOutputStream(mOutputFile.getFileDescriptor());
        OutputStream out = null;

        PackageInfo pkg = null;
        try {
            OutputStream finalOutput = ofstream;

            // Write the global file header of an unencrypted, uncompressed adb backup.
            StringBuilder headerbuf = new StringBuilder(1024);

            headerbuf.append(BACKUP_FILE_HEADER_MAGIC);
            headerbuf.append(BACKUP_FILE_VERSION); // integer, no trailing \n
            headerbuf.append("\n0\n");

            try {
                headerbuf.append("none\n");

                byte[] header = headerbuf.toString().getBytes("UTF-8");
                ofstream.write(header);

                out = finalOutput;
            } catch (Exception e) {
                // Should never happen!
                Slog.e(TAG, "Unable to emit archive header", e);
                return;
            }

            Slog.i(TAG, "prepare backup timeout : " + mTimeout);

            int result = BackupTransport.TRANSPORT_ERROR;
            int N = backupQueue.size();
            for (int i = 0; i < N; i++) {
                pkg = backupQueue.get(i);
                final boolean isSharedStorage =
                        pkg.packageName.equals(SHARED_BACKUP_AGENT_PACKAGE);

                mBackupEngine =
                        new FullBackupEngine(
                                backupManagerService,
                                out,
                                null,
                                pkg,
                                false,
                                mTimeout > 0 ? this : null,
                                Long.MAX_VALUE,
                                mCurrentOpToken,
                                /*transportFlags=*/ 0);
                mBackupEngine.mExt.setIgnoreSignature(true);
                mBackupEngine.mExt.setTimeout(mTimeout);
                mBackupEngine.mExt.setBackupPaths(mIncludePaths, mExcludePaths);
                mBackupEngine.mExt.setBackupEndNotKill(mNotKill);
                sendOnBackupPackage(isSharedStorage ? "Shared storage" : pkg.packageName);

                // Don't need to check preflight result as there is no preflight hook.
                mCurrentTarget = pkg;
                result = mBackupEngine.backupOnePackage();
            }

            // Done!
            finalizeBackup(out);
            if (result != BackupTransport.TRANSPORT_OK) {
                sendBackupErr("backup err result is " + result);
            }
        } catch (Exception e) {
            Slog.e(TAG, "Internal exception during full backup", e);
            sendBackupErr("backup err " + e.getMessage());
        } finally {
            try {
                if (out != null) {
                    out.flush();
                    out.close();
                }
                mOutputFile.close();
            } catch (IOException e) {
                /* nothing we can do about this */
            }
            synchronized (backupManagerService.getCurrentOpLock()) {
                backupManagerService.getCurrentOperations().clear();
            }
            synchronized (mLatch) {
                mLatch.set(true);
                mLatch.notifyAll();
            }
            sendEndBackup();
            obbConnection.tearDown();
            if (DEBUG) {
                Slog.d(TAG, "Full backup pass complete.");
            }
            backupManagerService.getWakelock().release();
        }
    }

    private void sendBackupErr(String message) {
        if (mObserver != null) {
            try {
                mObserver.onBackupRestoreErr(0, message);
            } catch (RemoteException e) {
                Slog.w(TAG, "full backup observer went away: timeout backup");
                mObserver = null;
            }
        }
    }

    private void sendBackupTimeout() {
        if (mObserver != null) {
            try {
                mObserver.onTimeout();
            } catch (RemoteException e) {
                Slog.w(TAG, "full backup observer went away: timeout backup");
                mObserver = null;
            }
        }
    }

    // BackupRestoreTask methods, used for timeout handling
    @Override
    public void execute() {
        // Unused
    }

    @Override
    public void operationComplete(long result) {
        // Unused
    }

    @Override
    public void handleCancel(boolean cancelAll) {
        Slog.w(TAG, "handle backup handleCancel  " + cancelAll);
        sendBackupTimeout();
        if (mCurrentTarget != null) {
            backupManagerService.tearDownAgentAndKill(mCurrentTarget.applicationInfo);
        }
        backupManagerService.removeOperation(mCurrentOpToken);
    }
}
