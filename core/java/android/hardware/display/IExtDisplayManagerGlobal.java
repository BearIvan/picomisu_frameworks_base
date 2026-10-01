// Copyright 2026 Picomisu contributors
// SPDX-License-Identifier: Apache-2.0
package android.hardware.display;

import android.view.DisplayInfo;

import com.pico.util.IExtBase;

/**
 * PICO display manager global extension (factory PICO OS 5.13.7
 * android.hardware.display.IExtDisplayManagerGlobal).
 * @hide
 */
public interface IExtDisplayManagerGlobal extends IExtBase {
    void adjustDisplayInfo(int displayId, DisplayInfo displayInfo);
}
