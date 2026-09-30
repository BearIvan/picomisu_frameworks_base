// Copyright 2026 Picomisu contributors
// SPDX-License-Identifier: Apache-2.0
package com.android.server.backup;

import android.app.backup.IFullBackupRestoreObserver;
import android.os.ParcelFileDescriptor;
import android.util.Slog;

import com.android.server.backup.fullbackup.PicoFullBackupTask;
import com.android.server.backup.restore.PicoFullRestoreTask;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * PICO per-user backup manager extension.
 * @hide
 */
public class ExtUserBackupManagerServiceImpl implements IExtUserBackupManagerService {
    public static final String TAG = "BackupManagerService";
    private UserBackupManagerService mBase;

    public ExtUserBackupManagerServiceImpl(UserBackupManagerService base) {
        mBase = base;
    }

    /**
     * Starts a full backup of package {@code packageNames} into {@code fd} on a new thread,
     * holding the backup wake lock until the task finishes.
     */
    @Override
    public void backup(ParcelFileDescriptor fd, String packageNames, List<String> includePaths,
            List<String> excludePaths, IFullBackupRestoreObserver observer, long timeout,
            boolean notKill) {
        mBase.getContext().enforceCallingOrSelfPermission(android.Manifest.permission.BACKUP,
                "pico backup");
        Slog.w(TAG, "Backup ... PicoPerformFullBackupTask");
        PicoFullBackupTask task = new PicoFullBackupTask(mBase, includePaths, excludePaths, fd,
                observer, packageNames, new AtomicBoolean(false), timeout, notKill);
        mBase.getWakelock().acquire();
        new Thread(task).start();
    }

    /**
     * Starts a full restore of the backup stream {@code fd} on a new thread, holding the
     * backup wake lock until the task finishes.
     */
    @Override
    public void restore(ParcelFileDescriptor fd, IFullBackupRestoreObserver observer,
            long timeout) {
        mBase.getContext().enforceCallingOrSelfPermission(android.Manifest.permission.BACKUP,
                "pico restore");
        Slog.w(TAG, "Restore ... PicoFullRestoreTask");
        PicoFullRestoreTask task = new PicoFullRestoreTask(mBase, fd, observer,
                new AtomicBoolean(false), timeout);
        mBase.getWakelock().acquire();
        new Thread(task).start();
    }
}
