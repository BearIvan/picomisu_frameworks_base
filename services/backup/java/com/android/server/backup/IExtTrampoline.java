// Copyright 2026 Picomisu contributors
// SPDX-License-Identifier: Apache-2.0
package com.android.server.backup;

import android.app.backup.IFullBackupRestoreObserver;
import android.os.ParcelFileDescriptor;

import com.pico.util.IExtBase;

import java.util.List;

/**
 * PICO backup manager trampoline extension: binder entry points of the PICO full backup and
 * restore.
 * @hide
 */
public interface IExtTrampoline extends IExtBase {
    void backup(ParcelFileDescriptor fd, String packageNames, List<String> includePaths,
            List<String> excludePaths, IFullBackupRestoreObserver observer, long timeout,
            boolean notKill);
    void restore(ParcelFileDescriptor fd, IFullBackupRestoreObserver observer, long timeout);
}
