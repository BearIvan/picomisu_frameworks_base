// Copyright 2026 Picomisu contributors
// SPDX-License-Identifier: Apache-2.0
package com.android.server.devicepolicy;

import android.content.ComponentName;
import android.os.SystemProperties;

import com.pico.util.IExtBase;

/**
 * PICO device policy extension (factory PICO OS 5.13.7
 * com.android.server.devicepolicy.IExtDevicePolicyManagerService): ToB device-owner provisioning,
 * the persist.sys.tob.dpm.enabled admin switch and admin change tracking.
 */
public interface IExtDevicePolicyManagerService extends IExtBase {
    boolean IS_TOB_DEVICE = SystemProperties.getInt("ro.pxr.externalfunc", 0) != 0;

    void deleteActiveAdmin(ComponentName admin);

    void sendBootEventTrack(DevicePolicyManagerService.DevicePolicyData data);

    default boolean tobForceEnableDeviceOwnerProvisioning() {
        return false;
    }

    default boolean isPackageAvailable(String pkgName, int userId) {
        return true;
    }

    default boolean canSetActiveAdmin(String adminPackageName) {
        return true;
    }
}
