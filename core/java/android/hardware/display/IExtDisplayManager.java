// Copyright 2026 Picomisu contributors
// SPDX-License-Identifier: Apache-2.0
package android.hardware.display;

import android.content.Context;

import com.pico.util.IExtBase;

/**
 * PICO display manager extension (factory PICO OS 5.13.7
 * android.hardware.display.IExtDisplayManager).
 * @hide
 */
public interface IExtDisplayManager extends IExtBase {
    int VIRTUAL_DISPLAY_FLAG_2D_APP = 32768;
    int VIRTUAL_DISPLAY_FLAG_UNFOCUSABLE = 65536;
    int VIRTUAL_DISPLAY_FLAG_VR_LOADING = 16384;

    boolean registerDisplayListener(Context context);
}
