// Copyright 2026 Picomisu contributors
// SPDX-License-Identifier: Apache-2.0
package com.android.server.pm.permission;

import android.content.pm.PackageInfo;
import android.os.UserHandle;
import android.text.TextUtils;
import android.util.ArraySet;
import android.util.Log;

import com.android.internal.util.ArrayUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * PICO DefaultPermissionGrantPolicy extension (factory PICO OS 5.13.7
 * com.android.server.pm.permission.ExtDefaultPermissionGrantPolicyImpl).
 *
 * <p>System packages that are not system components / persistent platform-signed privileged
 * apps get every requested runtime permission outside MANAGED_PERMISSIONS (com.picovr.preview
 * also gets the storage pair); for WHITE_SYSTEM_PKG_LIST the user-set / user-fixed flags of the
 * managed permissions are cleared. The default browser gets no default location grant
 * (updateBrowserPkgName returns null).
 */
public class ExtDefaultPermissionGrantPolicyImpl implements IExtDefaultPermissionGrantPolicy {
    private static final String TAG = "DefaultPermGrantPolicy";
    private DefaultPermissionGrantPolicy mBase;
    private static final Set<String> MANAGED_PERMISSIONS = new ArraySet<>();
    public static final List<String> WHITE_SYSTEM_PKG_LIST = new ArrayList<>();

    public ExtDefaultPermissionGrantPolicyImpl(DefaultPermissionGrantPolicy policy) {
        mBase = policy;
    }

    @Override
    public String updateBrowserPkgName(String browsePackage) {
        return null;
    }

    static {
        MANAGED_PERMISSIONS.add("android.permission.READ_EXTERNAL_STORAGE");
        MANAGED_PERMISSIONS.add("android.permission.WRITE_EXTERNAL_STORAGE");
        MANAGED_PERMISSIONS.add("android.permission.ACCESS_MEDIA_LOCATION");
        MANAGED_PERMISSIONS.add("android.permission.CAMERA");
        MANAGED_PERMISSIONS.add("android.permission.READ_PHONE_STATE");
        MANAGED_PERMISSIONS.add("android.permission.READ_PHONE_NUMBERS");
        MANAGED_PERMISSIONS.add("android.permission.CALL_PHONE");
        MANAGED_PERMISSIONS.add("com.android.voicemail.permission.ADD_VOICEMAIL");
        MANAGED_PERMISSIONS.add("android.permission.USE_SIP");
        MANAGED_PERMISSIONS.add("android.permission.ANSWER_PHONE_CALLS");
        MANAGED_PERMISSIONS.add("android.permission.ACCEPT_HANDOVER");
        MANAGED_PERMISSIONS.add("android.permission.ACCESS_FINE_LOCATION");
        MANAGED_PERMISSIONS.add("android.permission.ACCESS_COARSE_LOCATION");
        MANAGED_PERMISSIONS.add("android.permission.ACCESS_BACKGROUND_LOCATION");
        MANAGED_PERMISSIONS.add("android.permission.RECORD_AUDIO");
        MANAGED_PERMISSIONS.add("com.picovr.permission.EYE_TRACKING");
        MANAGED_PERMISSIONS.add("com.picovr.permission.FACE_TRACKING");
        WHITE_SYSTEM_PKG_LIST.add("com.pico.browser.overseas");
        WHITE_SYSTEM_PKG_LIST.add("com.pico.browser");
        WHITE_SYSTEM_PKG_LIST.add("com.picopui.im");
        WHITE_SYSTEM_PKG_LIST.add("com.pvr.socialhome");
        WHITE_SYSTEM_PKG_LIST.add("com.ss.android.ttvr.global");
        WHITE_SYSTEM_PKG_LIST.add("com.pvr.picocast");
    }

    @Override
    public void onGrantPermissionsToSysComponentsAndPrivApps(int userId, PackageInfo pkg) {
        if (ArrayUtils.isEmpty(pkg.requestedPermissions)) {
            return;
        }
        if (DefaultPermissionGrantPolicy.doesPackageSupportRuntimePermissions(pkg)
                && mBase.isSystemPackage(pkg)) {
            grantNotManagerRuntimePermissionsForSystemPackage(userId, pkg);
            if (WHITE_SYSTEM_PKG_LIST.contains(pkg.packageName)) {
                updateWhitlePkgManagerRuntimePermissionFlag(userId, pkg);
            }
        }
    }

    private void grantNotManagerRuntimePermissionsForSystemPackage(int userId, PackageInfo pkg) {
        Set<String> permissions = new ArraySet<>();
        for (String permission : pkg.requestedPermissions) {
            BasePermission bp = mBase.mPermissionManager.getPermission(permission);
            if (bp != null && bp.isRuntime()) {
                if (!isManagedPermission(permission)) {
                    Log.d(TAG, "grant permission:" + permission + " for " + pkg.packageName);
                    permissions.add(permission);
                } else if (TextUtils.equals("com.picovr.preview", pkg.packageName)
                        && (TextUtils.equals("android.permission.WRITE_EXTERNAL_STORAGE",
                                permission)
                        || TextUtils.equals("android.permission.READ_EXTERNAL_STORAGE",
                                permission))) {
                    permissions.add(permission);
                }
            }
        }
        if (!permissions.isEmpty()) {
            mBase.grantRuntimePermissions(pkg, permissions, true, userId);
        }
    }

    private void updateWhitlePkgManagerRuntimePermissionFlag(int userId, PackageInfo pkg) {
        for (String permission : pkg.requestedPermissions) {
            if (isManagedPermission(permission)) {
                UserHandle user = UserHandle.of(userId);
                int oldFlags = mBase.mContext.getPackageManager().getPermissionFlags(permission,
                        pkg.packageName, user);
                int flagMask = 0;
                if ((oldFlags & 32) != 0) {
                    flagMask |= 32;
                }
                if ((oldFlags & 16) != 0) {
                    flagMask |= 16;
                }
                if (flagMask != 0) {
                    mBase.mContext.getPackageManager().updatePermissionFlags(permission,
                            pkg.packageName, flagMask, 0, user);
                }
            }
        }
    }

    private boolean isManagedPermission(String permission) {
        return MANAGED_PERMISSIONS.contains(permission);
    }
}
