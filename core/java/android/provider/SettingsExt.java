// Copyright 2026 Picomisu contributors
// SPDX-License-Identifier: Apache-2.0

package android.provider;

/**
 * Extra settings keys (factory PICO OS 5.13.7 {@code android.provider.SettingsExt}).
 *
 * @hide
 */
public class SettingsExt {
    /** Extra {@link Settings.System} keys for 2D app displays. */
    public static final class System {
        public static final String APP_DISPLAY_ID_LIST = "app_display_id_list";
        public static final String IME_FOR_2D_APP_DISPLAY_ID = "ime_for_2d_app_display_id";
    }
}
