// Copyright 2026 Picomisu contributors
// SPDX-License-Identifier: Apache-2.0
package com.android.server.backup;

import android.app.backup.IFullBackupRestoreObserver;
import android.os.ParcelFileDescriptor;
import android.os.UserHandle;

import java.util.List;

/**
 * PICO backup manager trampoline extension. The PICO backup and restore always run for the
 * system user.
 * @hide
 */
public class ExtTrampolineImpl implements IExtTrampoline {
    private Trampoline mBase;

    public ExtTrampolineImpl(Trampoline base) {
        mBase = base;
    }

    @Override
    public void backup(ParcelFileDescriptor fd, String packageNames, List<String> includePaths,
            List<String> excludePaths, IFullBackupRestoreObserver observer, long timeout,
            boolean notKill) {
        UserBackupManagerService userBackupManagerService =
                mBase.mService.getServiceForUserIfCallerHasPermission(UserHandle.USER_SYSTEM,
                        "isAppEligibleForBackup()");
        if (userBackupManagerService != null) {
            userBackupManagerService.mExt.backup(fd, packageNames, includePaths, excludePaths,
                    observer, timeout, notKill);
        }
    }

    @Override
    public void restore(ParcelFileDescriptor fd, IFullBackupRestoreObserver observer,
            long timeout) {
        UserBackupManagerService userBackupManagerService =
                mBase.mService.getServiceForUserIfCallerHasPermission(UserHandle.USER_SYSTEM,
                        "isAppEligibleForBackup()");
        if (userBackupManagerService != null) {
            userBackupManagerService.mExt.restore(fd, observer, timeout);
        }
    }
}
