// Copyright 2026 Picomisu contributors
// SPDX-License-Identifier: Apache-2.0
package com.android.server.pm.permission;

import android.content.pm.PackageInfo;

import com.pico.util.IExtBase;

/**
 * PICO DefaultPermissionGrantPolicy extension (factory PICO OS 5.13.7
 * com.android.server.pm.permission.IExtDefaultPermissionGrantPolicy).
 */
public interface IExtDefaultPermissionGrantPolicy extends IExtBase {
    void onGrantPermissionsToSysComponentsAndPrivApps(int userId, PackageInfo pkg);

    String updateBrowserPkgName(String browserPackage);
}
