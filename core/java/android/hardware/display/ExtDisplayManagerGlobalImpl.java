// Copyright 2026 Picomisu contributors
// SPDX-License-Identifier: Apache-2.0
package android.hardware.display;

import android.pico.utils.PicoUtils;
import android.view.DisplayInfo;

/**
 * PICO display manager global extension (factory PICO OS 5.13.7
 * android.hardware.display.ExtDisplayManagerGlobalImpl): a 2D app that uses the new
 * configuration solution sees display 0 with its 2D app size.
 * @hide
 */
public class ExtDisplayManagerGlobalImpl implements IExtDisplayManagerGlobal {
    private DisplayManagerGlobal mBase;

    public ExtDisplayManagerGlobalImpl(DisplayManagerGlobal base) {
        mBase = base;
    }

    @Override
    public void adjustDisplayInfo(int displayId, DisplayInfo displayInfo) {
        PicoUtils.updateDisplayInfo(displayId, displayInfo);
    }
}
