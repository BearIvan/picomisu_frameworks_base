// Copyright 2026 Picomisu contributors
// SPDX-License-Identifier: Apache-2.0

package com.android.server.am;

import android.util.Log;

/**
 * Reconstructed from the PICO OS 5.13.7 factory services.
 *
 * @hide
 */
public class ActivityManagerDebugConfigSmtEx {
    static final String POSTFIX_FREEZE = "";
    public static boolean DEBUG_FREEZE = true;
    public static boolean DEBUG_POWER_KILL = true;
    static boolean DEBUG_3RD_BG_APP = Log.isLoggable("Bg3rdApp", 3);
    static boolean DEBUG_CPU_TRACK = Log.isLoggable("CpuTracker", 3);
    static boolean DEBUG_LOCK_PACKAGE = false;
    static boolean DEBUG_CHAINBOOT_BLACKLIST = false;
    static boolean DEBUG_WIFI_UPLOAD = false;
    public static String DEBUG_INTELLIGENTENGINE = "DEBUG_INTELLIGENTENGINE";
}
