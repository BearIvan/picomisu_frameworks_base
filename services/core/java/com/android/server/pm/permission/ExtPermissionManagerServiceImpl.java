// Copyright 2026 Picomisu contributors
// SPDX-License-Identifier: Apache-2.0
package com.android.server.pm.permission;

import android.content.pm.PackageParser;
import android.pico.utils.PicoSystemConfig;
import android.util.ArraySet;
import android.util.Log;

import com.android.server.pm.PackageSetting;

import java.util.ArrayList;

/**
 * PICO PermissionManagerService extension (factory PICO OS 5.13.7
 * com.android.server.pm.permission.ExtPermissionManagerServiceImpl).
 *
 * <p>SYSTEM_ALERT_WINDOW is only granted to non-system packages that are VR apps (PICO VR
 * whitelist, com.picovr.type / pvr.app.type meta-data "vr", or a VR launcher category) or that
 * are on the PICO 2D floating-window whitelist; for those an install-time denial becomes a
 * runtime grant.
 */
public class ExtPermissionManagerServiceImpl implements IExtPermissionManagerService {
    private static final String TAG = "PackageManager";
    private static final String[] VR_CATEGORY_ARRAY = {"com.google.intent.category.DAYDREAM",
            "com.google.intent.category.CARDBOARD", "com.qti.intent.category.SNAPDRAGON_VR",
            "PVR"};
    private static final String[] VR_TAG_ARRAY = {"com.picovr.type", "pvr.app.type"};
    private PermissionManagerService mBase;

    public ExtPermissionManagerServiceImpl(PermissionManagerService base) {
        mBase = base;
    }

    private static boolean isPackageVRType(PackageParser.Package pkg) {
        try {
            ArraySet<String> whitelistVrApps =
                    PicoSystemConfig.getInstance().getPicoWhitelistVrPackages();
            if (whitelistVrApps.contains(pkg.packageName)) {
                return true;
            }
            if (pkg.mAppMetaData != null) {
                for (int i = 0; i < VR_TAG_ARRAY.length; i++) {
                    String value = pkg.mAppMetaData.getString(VR_TAG_ARRAY[i]);
                    if (value != null) {
                        if (value.equalsIgnoreCase("vr")) {
                            return true;
                        }
                        if (value.equalsIgnoreCase("2d")) {
                            return false;
                        }
                    }
                }
            }
            for (PackageParser.Activity activity : pkg.activities) {
                if (activity != null && activity.metaData != null) {
                    for (int i = 0; i < VR_TAG_ARRAY.length; i++) {
                        String activityMetadataValue = activity.metaData.getString(VR_TAG_ARRAY[i]);
                        if (activityMetadataValue != null
                                && activityMetadataValue.equalsIgnoreCase("vr")) {
                            return true;
                        }
                    }
                    ArrayList<PackageParser.ActivityIntentInfo> activityIntents = activity.intents;
                    if (activityIntents != null && activityIntents.size() > 0) {
                        for (int i = 0; i < activityIntents.size(); i++) {
                            PackageParser.ActivityIntentInfo aii = activityIntents.get(i);
                            for (int j = 0; j < VR_CATEGORY_ARRAY.length; j++) {
                                if (aii != null && aii.hasCategory(VR_CATEGORY_ARRAY[j])) {
                                    return true;
                                }
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            Log.w(TAG, "isPackageVRType exception:", e);
        }
        return false;
    }

    private static boolean isPackageInWhitelistToGetSystemAlertWindowPermission(
            PackageParser.Package pkg) {
        ArraySet<String> whitelist2dFloatApps =
                PicoSystemConfig.getInstance().getPicoWhitelist2dFloatPackages();
        return whitelist2dFloatApps.contains(pkg.packageName);
    }

    @Override
    public int allowAlertWindowAndRevokeInstallPermission(int oldGrant, PackageParser.Package pkg,
            PackageSetting ps, String perm, BasePermission bp, PermissionsState origPermissions) {
        int grant = oldGrant;
        if (!ps.isSystem() && "android.permission.SYSTEM_ALERT_WINDOW".equals(perm)
                && !isPackageVRType(pkg) && !isPackageInWhitelistToGetSystemAlertWindowPermission(pkg)) {
            Log.v(TAG, "package:" + pkg.packageName
                    + " requires SYSTEM_ALERT_WINDOW permission, just deny!");
            grant = PermissionManagerService.GRANT_DENIED;
        }
        if (!ps.isSystem() && "android.permission.SYSTEM_ALERT_WINDOW".equals(perm)
                && (isPackageVRType(pkg) || isPackageInWhitelistToGetSystemAlertWindowPermission(pkg))
                && grant == PermissionManagerService.GRANT_DENIED) {
            grant = PermissionManagerService.GRANT_INSTALL;
        }
        return revokeInstallPermissionInner(grant, pkg, ps, bp, origPermissions);
    }

    @Override
    public boolean verifyAlertPermission(PackageParser.Package pkg, PackageSetting ps,
            String permName, String packageName) {
        if ("android.permission.SYSTEM_ALERT_WINDOW".equals(permName) && !ps.isSystem()
                && !isPackageVRType(pkg) && !isPackageInWhitelistToGetSystemAlertWindowPermission(pkg)) {
            Log.w(TAG, "package:" + packageName + " is granting development permission:"
                    + permName + ", deny it!");
            return true;
        }
        return false;
    }

    private int revokeInstallPermissionInner(int oldGrant, PackageParser.Package pkg,
            PackageSetting ps, BasePermission bp, PermissionsState origPermissions) {
        return oldGrant;
    }
}
