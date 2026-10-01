// Copyright 2026 Picomisu contributors
// SPDX-License-Identifier: Apache-2.0
package com.android.server.display;

import android.view.SurfaceControl;

/**
 * PICO display device extension (factory PICO OS 5.13.7
 * com.android.server.display.ExtDisplayDeviceImpl).
 */
public class ExtDisplayDeviceImpl implements IExtDisplayDevice {
    private final DisplayDevice mBase;

    public ExtDisplayDeviceImpl(DisplayDevice base) {
        mBase = base;
    }

    /** SurfaceFlinger display flags of the device (1: 2D app virtual display). */
    @Override
    public void setDisplayFlagsLocked(SurfaceControl.Transaction t, int flags) {
        t.setDisplayFlags(mBase.getDisplayTokenLocked(), flags);
    }
}
