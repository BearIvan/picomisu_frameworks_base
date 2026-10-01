// Copyright 2026 Picomisu contributors
// SPDX-License-Identifier: Apache-2.0
package com.android.server.pm.permission;

import android.content.pm.PackageParser;

import com.android.server.pm.PackageSetting;
import com.pico.util.IExtBase;

/**
 * PICO PermissionManagerService extension (factory PICO OS 5.13.7
 * com.android.server.pm.permission.IExtPermissionManagerService).
 */
public interface IExtPermissionManagerService extends IExtBase {
    int allowAlertWindowAndRevokeInstallPermission(int oldGrant, PackageParser.Package pkg,
            PackageSetting ps, String perm, BasePermission bp, PermissionsState origPermissions);

    boolean verifyAlertPermission(PackageParser.Package pkg, PackageSetting ps, String permName,
            String packageName);
}
