// Copyright 2026 Picomisu contributors
// SPDX-License-Identifier: Apache-2.0
package com.android.server.backup;

import android.app.backup.IFullBackupRestoreObserver;
import android.os.ParcelFileDescriptor;

import com.pico.util.IExtBase;

import java.util.List;

/**
 * PICO per-user backup manager extension: full backup and restore of one package through
 * {@link android.app.backup.IBackupManager#backup} and
 * {@link android.app.backup.IBackupManager#restore}.
 * @hide
 */
public interface IExtUserBackupManagerService extends IExtBase {
    void backup(ParcelFileDescriptor fd, String packageNames, List<String> includePaths,
            List<String> excludePaths, IFullBackupRestoreObserver observer, long timeout,
            boolean notKill);
    void restore(ParcelFileDescriptor fd, IFullBackupRestoreObserver observer, long timeout);
}
