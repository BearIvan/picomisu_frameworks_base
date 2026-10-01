// Copyright 2026 Picomisu contributors
// SPDX-License-Identifier: Apache-2.0
package com.android.server.display;

import com.pico.util.IExtBase;

/**
 * PICO display device info flags (factory PICO OS 5.13.7
 * com.android.server.display.IExtDisplayDeviceInfo). The virtual display creation flag, the
 * DisplayDeviceInfo flag and the Display flag use the same bits.
 * @hide
 */
public interface IExtDisplayDeviceInfo extends IExtBase {
    int FLAG_2D_APP_VIRTUAL_DISPLAY = 32768;
    int FLAG_2D_APP_VIRTUAL_DISPLAY_UNFOCUSABLE = 65536;
    int FLAG_VR_LOADING_VIRTUAL_DISPLAY = 16384;
}
