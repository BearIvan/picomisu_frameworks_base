// Copyright 2026 Picomisu contributors
// SPDX-License-Identifier: Apache-2.0
package com.android.server.display;

import android.view.DisplayInfo;
import android.view.SurfaceControl;

import com.pico.util.IExtBase;

/**
 * PICO logical display extension (factory PICO OS 5.13.7
 * com.android.server.display.IExtLogicalDisplay).
 * @hide
 */
public interface IExtLogicalDisplay extends IExtBase {
    void adjustDisplayInfoFlags(DisplayInfo displayInfo, int displayDeviceFlags);

    void setDisplayFlags(SurfaceControl.Transaction t, DisplayDevice device,
            DisplayInfo displayInfo);
}
