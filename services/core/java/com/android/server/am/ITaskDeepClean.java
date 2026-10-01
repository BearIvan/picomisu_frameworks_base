// Copyright 2026 Picomisu contributors
// SPDX-License-Identifier: Apache-2.0

package com.android.server.am;

import android.os.DebugSmtEx;

/**
 * Reconstructed from the PICO OS 5.13.7 factory services.
 *
 * @hide
 */
public interface ITaskDeepClean {
    default void init(ActivityManagerService ams) {
        DebugSmtEx.printDefaultFunInfo(getClass());
    }

    default boolean isDeepClean(String packageName, int cleanLevel) {
        DebugSmtEx.printDefaultFunInfo(getClass());
        return false;
    }

    default void updateVpnPackage(String vpnPackage) {
        DebugSmtEx.printDefaultFunInfo(getClass());
    }

    default void tryDoForceStop(int callerPid, String pkgName, int userId) {
        DebugSmtEx.printDefaultFunInfo(getClass());
    }

    default void addTdcEvent(int type, int cleanLevel, String pkg, int reason) {
        DebugSmtEx.printDefaultFunInfo(getClass());
    }
}
