// Copyright 2026 Picomisu contributors
// SPDX-License-Identifier: Apache-2.0

package android.os;

import android.util.Slog;

/**
 * Smartisan debug helpers. Reconstructed from the PICO OS 5.13.7 factory framework.
 *
 * @hide
 */
public class DebugSmtEx {
    /** Smartisan extra /proc/meminfo indices (after the AOSP Debug.MEMINFO_* entries). */
    public static final int MEMINFO_ION_SYSTEM = 15;
    public static final int MEMINFO_ION_CACHED = 16;
    public static final int MEMINFO_ZRAM_PHY_USED = 17;
    public static final int MEMINFO_GFX_CACHED = 18;
    public static final int MEMINFO_COUNT = 19;

    public static final String SYS_DEFAULT_LOG = "SYS_DEFAULT_LOG";

    /**
     * Logs that the default (no-op) implementation of a Smartisan bridge interface method ran,
     * i.e. that the optional sys-/sysmonitor- factory implementation is not installed.
     */
    public static void printDefaultFunInfo(Class<?> callingClass) {
        Slog.e(SYS_DEFAULT_LOG, callingClass + "|"
                + callingClass.getInterfaces()[0].getSimpleName() + "|"
                + Thread.currentThread().getStackTrace()[3].getMethodName()
                + "|SYS_DEFAULT_FUN_CONTENT");
    }
}
