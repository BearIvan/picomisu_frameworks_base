// Copyright 2026 Picomisu contributors
// SPDX-License-Identifier: Apache-2.0
package com.android.server.wm;

import android.os.SystemProperties;

/**
 * PICO display rotation extension (factory PICO OS 5.13.7
 * com.android.server.wm.ExtDisplayRotationImpl).
 */
public class ExtDisplayRotationImpl implements IExtDisplayRotation {
    private DisplayRotation mBase;

    public ExtDisplayRotationImpl(DisplayRotation base) {
        mBase = base;
    }

    /** DisplayRotation.configure: portrait maps to landscape on the default display. */
    @Override
    public boolean forceLandscape() {
        return SystemProperties.getBoolean("persist.pvr.force_landscape", true)
                && mBase.isDefaultDisplay;
    }
}
