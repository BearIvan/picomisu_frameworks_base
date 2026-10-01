// Copyright 2026 Picomisu contributors
// SPDX-License-Identifier: Apache-2.0

package smartisanos.tnt;

import android.os.SystemProperties;

/**
 * TNT (Smartisan desktop mode) feature switches
 * (factory PICO OS 5.13.7 {@code smartisanos.tnt.TntFeature}).
 *
 * @hide
 */
public class TntFeature {
    private static final String TAG = "TntFeature";

    public static final boolean TNT_ON_AOSP = false;
    public static final boolean IME = true;
    public static final boolean POWER = true;
    public static final boolean PARA_POSE = true;

    public static boolean TNT_PAD = SystemProperties.getInt("persist.debug.pad.tnt30", 0) == 1;
    public static boolean TASK_DIVIDER_ENABLED;

    public static void updatePadMode(boolean enablePad) {
        TNT_PAD = enablePad;
        TASK_DIVIDER_ENABLED = TNT_PAD;
    }
}
