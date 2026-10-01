// Copyright 2026 Picomisu contributors
// SPDX-License-Identifier: Apache-2.0

package android.provider;

/**
 * Smartisan settings keys and values
 * (factory PICO OS 5.13.7 {@code android.provider.SettingsSmtBase}).
 *
 * @hide
 */
public class SettingsSmtBase {
    static final String TAG = "SettingsSmtEx";

    /** Smartisan {@link Settings.System} keys. */
    public abstract static class SystemBase {
        public static final String HERO_FORBID_APPSTART_ENABLED = "forbidapp.start";
        public static final String HERO_FORBID_APPSTART_AUTHORITY = "forbidapp.start.authority";
        public static final String SCREEN_BRIGHTNESS_TEMP = "screen_brightness_temp";
        public static final String REALTIME_SCREEN_BRIGHTNESS = "realtime_screen_brightness";
        public static final String SUNSHINE_SCREEN = "sunshine_screen";
    }

    /** Smartisan {@link Settings.Secure} keys. */
    public abstract static class SecureBase {
    }

    /** Smartisan {@link Settings.Global} keys. */
    public abstract static class GlobalBase {
        public static final String POWER_SLEEP_MODE_ENABLE = "sleep_mode_enable";
        public static final String SLEEP_MODE_LAST_WIFI_STATE = "sleep_mode_last_wifi_state";
        public static final String SLEEP_MODE_LAST_MOBILE_STATE = "sleep_mode_last_mobile_state";
        public static final String POWER_ACL_MODE_ENABLE = "acl_mode_enable";
        public static final String GLOBAL_PC_MODE_SETTINGS = "global_pc_mode_settings";
    }

    /** Values of {@link GlobalBase#POWER_SLEEP_MODE_ENABLE}. */
    public abstract static class SLEEP_MODE_BASE {
        public static final int SLEEP_MODE_OFF = 0;
        public static final int SLEEP_MODE_ON = 1;
        public static final int SLEEP_MODE_LAST_OFF_STATE = 0;
        public static final int SLEEP_MODE_LAST_ON_STATE = 1;
    }

    /** Values of {@link GlobalBase#POWER_ACL_MODE_ENABLE}. */
    public abstract static class ACL_MODE_BASE {
        public static final int ACL_MODE_OFF = 0;
        public static final int ACL_MODE_ON = 1;
        public static final int ACL_MODE_DEFAULT = ACL_MODE_ON;
    }
}
