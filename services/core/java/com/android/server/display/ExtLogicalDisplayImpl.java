// Copyright 2026 Picomisu contributors
// SPDX-License-Identifier: Apache-2.0
package com.android.server.display;

import android.view.DisplayInfo;
import android.view.SurfaceControl;

/**
 * PICO logical display extension (factory PICO OS 5.13.7
 * com.android.server.display.ExtLogicalDisplayImpl): the PICO display flags (1 << 14 VR
 * loading, 1 << 15 2D app, 1 << 16 unfocusable 2D app, 1 << 20) go from the device to the
 * DisplayInfo, and a 2D app display is flagged to SurfaceFlinger (flag 1, 1 << 20 passed on).
 */
public class ExtLogicalDisplayImpl implements IExtLogicalDisplay {
    private static final int DISPLAY_2D_APP_VIRTUAL = 1;
    private LogicalDisplay mBase;

    public ExtLogicalDisplayImpl(LogicalDisplay base) {
        mBase = base;
    }

    @Override
    public void adjustDisplayInfoFlags(DisplayInfo displayInfo, int displayDeviceFlags) {
        if ((displayDeviceFlags & 32768) != 0) {
            displayInfo.flags |= 32768;
        }
        if ((displayDeviceFlags & 65536) != 0) {
            displayInfo.flags |= 65536;
        }
        if ((displayDeviceFlags & 16384) != 0) {
            displayInfo.flags |= 16384;
        }
        if ((displayDeviceFlags & 1048576) != 0) {
            displayInfo.flags |= 1048576;
        }
    }

    @Override
    public void setDisplayFlags(SurfaceControl.Transaction t, DisplayDevice device,
            DisplayInfo displayInfo) {
        int flags = 0;
        if ((displayInfo.flags & 32768) != 0) {
            flags = 0 | DISPLAY_2D_APP_VIRTUAL;
        }
        if ((displayInfo.flags & 1048576) != 0) {
            flags |= 1048576;
        }
        device.getExt().setDisplayFlagsLocked(t, flags);
    }
}
