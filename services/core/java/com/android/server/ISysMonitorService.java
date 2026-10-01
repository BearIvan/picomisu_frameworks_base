// Copyright 2026 Picomisu contributors
// SPDX-License-Identifier: Apache-2.0

package com.android.server;

import android.content.Context;
import android.os.DebugSmtEx;
import com.android.server.am.ActivityManagerService;
import com.android.server.pm.PackageManagerService;
import com.android.server.power.PowerManagerService;

/**
 * Reconstructed from the PICO OS 5.13.7 factory services.
 *
 * @hide
 */
public interface ISysMonitorService {
    default void systemReady() {
        DebugSmtEx.printDefaultFunInfo(getClass());
    }

    default void initContext(Context context) {
        DebugSmtEx.printDefaultFunInfo(getClass());
    }

    default void initActivityManagerService(ActivityManagerService activityManagerService) {
        DebugSmtEx.printDefaultFunInfo(getClass());
    }

    default void initPackageManagerService(PackageManagerService packageManagerService) {
        DebugSmtEx.printDefaultFunInfo(getClass());
    }

    default void initPowerManagerService(PowerManagerService powerManagerService) {
        DebugSmtEx.printDefaultFunInfo(getClass());
    }
}
