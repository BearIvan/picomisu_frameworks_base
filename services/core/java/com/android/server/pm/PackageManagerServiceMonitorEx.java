// Copyright 2026 Picomisu contributors
// SPDX-License-Identifier: Apache-2.0

package com.android.server.pm;

import android.os.Binder;

import com.android.server.pm.permission.PermissionManagerServiceInternal;

/**
 * Smartisan system monitor extension of the {@link PackageManagerService}. Reconstructed from
 * the PICO OS 5.13.7 factory services; only the members reached by
 * {@code IPackageManagerSmtEx.isTaskPersist} are present.
 *
 * @hide
 */
public class PackageManagerServiceMonitorEx {
    private PackageManagerService mPackageManagerService;
    /** Never assigned in the factory services either. */
    protected PermissionManagerServiceInternal mPermissionManager;

    public PackageManagerServiceMonitorEx(PackageManagerService packageManagerService) {
        mPackageManagerService = packageManagerService;
    }

    public boolean isTaskPersist(String packageName, int userId) {
        if (packageName == null) {
            return false;
        }
        int callingUid = Binder.getCallingUid();
        mPermissionManager.enforceCrossUserPermission(callingUid, userId, true, false,
                "persist task");
        return mPackageManagerService.mSettings.getSmtEx().isTaskPersist(packageName, userId);
    }
}
