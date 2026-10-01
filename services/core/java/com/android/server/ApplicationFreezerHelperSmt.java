// Copyright 2026 Picomisu contributors
// SPDX-License-Identifier: Apache-2.0

package com.android.server;

/**
 * Reconstructed from the PICO OS 5.13.7 factory services.
 *
 * @hide
 */
public class ApplicationFreezerHelperSmt {
    private static ApplicationFreezerInternalSmt sInstance;

    public static boolean registerFrozenCallbackByPidOnce(int pid, int uid, ApplicationFreezerInternalSmt.IFrozenCallback b) {
        if (sInstance == null) {
            sInstance = (ApplicationFreezerInternalSmt) LocalServices.getService(ApplicationFreezerInternalSmt.class);
        }
        ApplicationFreezerInternalSmt applicationFreezerInternalSmt = sInstance;
        if (applicationFreezerInternalSmt != null) {
            return applicationFreezerInternalSmt.registerFrozenCallbackByPidOnce(pid, uid, b);
        }
        return false;
    }

    public static boolean registerFrozenCallback(ApplicationFreezerInternalSmt.IFrozenCallback b, boolean register) {
        if (sInstance == null) {
            sInstance = (ApplicationFreezerInternalSmt) LocalServices.getService(ApplicationFreezerInternalSmt.class);
        }
        ApplicationFreezerInternalSmt applicationFreezerInternalSmt = sInstance;
        if (applicationFreezerInternalSmt != null) {
            return applicationFreezerInternalSmt.registerFrozenCallback(b, register);
        }
        return false;
    }

    public static boolean unregisterFrozenCallbackByPidOnce(int pid, int uid, ApplicationFreezerInternalSmt.IFrozenCallback b) {
        ApplicationFreezerInternalSmt applicationFreezerInternalSmt = sInstance;
        if (applicationFreezerInternalSmt == null) {
            return false;
        }
        return applicationFreezerInternalSmt.unregisterFrozenCallbackByPidOnce(pid, uid, b);
    }
}
