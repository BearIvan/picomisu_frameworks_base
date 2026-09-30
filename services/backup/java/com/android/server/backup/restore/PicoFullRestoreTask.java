// Copyright 2026 Picomisu contributors
// SPDX-License-Identifier: Apache-2.0
package com.android.server.backup.restore;

import static com.android.server.backup.BackupManagerService.MORE_DEBUG;
import static com.android.server.backup.BackupManagerService.TAG;

import android.app.IBackupAgent;
import android.app.backup.BackupAgent;
import android.app.backup.IFullBackupRestoreObserver;
import android.content.pm.ApplicationInfo;
import android.os.ParcelFileDescriptor;
import android.os.RemoteException;
import android.util.Slog;

import com.android.server.backup.BackupRestoreTask;
import com.android.server.backup.UserBackupManagerService;
import com.android.server.backup.fullbackup.FullBackupObbConnection;
import com.android.server.backup.utils.FullBackupRestoreObserverUtils;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * PICO full restore of an unencrypted adb backup stream, requested through
 * {@link android.app.backup.IBackupManager#restore}. Signature checks are skipped, the agent
 * timeout is taken from the request, and failures and timeouts are reported to the observer.
 */
public class PicoFullRestoreTask extends PerformAdbRestoreTask implements BackupRestoreTask {

    private UserBackupManagerService mBackupManagerService;
    private ParcelFileDescriptor mInputFile;
    private AtomicBoolean mLatchObject;
    private BackupAgent mPackageManagerBackupAgent;

    private IFullBackupRestoreObserver mObserver;
    private IBackupAgent mAgent;
    private String mAgentPackage;
    private ApplicationInfo mTargetApp;
    private FullBackupObbConnection mObbConnection = null;
    private FullRestoreEngineThread mEngineThread;
    private long mTimeout;

    public PicoFullRestoreTask(UserBackupManagerService backupManagerService,
            ParcelFileDescriptor fd, IFullBackupRestoreObserver observer, AtomicBoolean latch,
            long timeout) {
        super(backupManagerService, fd, "", "", observer, latch);
        mBackupManagerService = backupManagerService;
        mInputFile = fd;
        mObserver = observer;
        mLatchObject = latch;
        mAgent = null;
        mPackageManagerBackupAgent = backupManagerService.makeMetadataAgent();
        mAgentPackage = null;
        mTargetApp = null;
        mObbConnection = new FullBackupObbConnection(backupManagerService);
        mTimeout = timeout;
    }

    @Override
    public void run() {
        Slog.i(TAG, "--- Performing full-dataset restore ---");
        mObbConnection.establish();
        mObserver = FullBackupRestoreObserverUtils.sendStartRestore(mObserver);

        FileInputStream rawInStream = null;
        try {
            rawInStream = new FileInputStream(mInputFile.getFileDescriptor());

            InputStream tarInputStream = parseBackupFileHeaderAndReturnTarStream(rawInStream,
                    null);
            if (tarInputStream == null) {
                // There was an error reading the backup file, which is already handled and logged.
                // Just abort.
                return;
            }

            FullRestoreEngine mEngine = new FullRestoreEngine(mBackupManagerService,
                    mTimeout > 0 ? this : null, mObserver, null, null, true, true/*unused*/,
                    0 /*unused*/, true);
            mEngine.mExt.setTimeout(mTimeout);
            mEngine.mExt.setIgnoreSignatureAndAllowFlag(true);
            mEngineThread = new FullRestoreEngineThread(mEngine, tarInputStream);
            mEngineThread.run();

            if (MORE_DEBUG) {
                Slog.v(TAG, "Done consuming input tarfile.");
            }
            if (mEngine.getResult() != 0) {
                sendRestoreErr("restore err, result " + mEngine.getResult());
            }
        } catch (Exception e) {
            Slog.e(TAG, "Unable to read restore input");
            sendRestoreErr(e.getMessage());
        } finally {
            try {
                if (rawInStream != null) {
                    rawInStream.close();
                }
                mInputFile.close();
            } catch (IOException e) {
                Slog.w(TAG, "Close of restore data pipe threw", e);
                /* nothing we can do about this */
            }
            synchronized (mLatchObject) {
                mLatchObject.set(true);
                mLatchObject.notifyAll();
            }
            mObbConnection.tearDown();
            mObserver = FullBackupRestoreObserverUtils.sendEndRestore(mObserver);
            Slog.d(TAG, "Full restore pass complete.");
            mBackupManagerService.getWakelock().release();
        }
    }

    private void sendBackupTimeout() {
        if (mObserver != null) {
            try {
                Slog.w(TAG, "sendBackupTimeout");
                mObserver.onTimeout();
            } catch (RemoteException e) {
                Slog.w(TAG, "full backup observer went away: timeout backup");
                mObserver = null;
            }
        }
    }

    private void sendRestoreErr(String message) {
        if (mObserver != null) {
            try {
                mObserver.onBackupRestoreErr(1, message);
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
        sendBackupTimeout();
        if (mEngineThread != null) {
            mEngineThread.handleTimeout();
        }
    }
}
