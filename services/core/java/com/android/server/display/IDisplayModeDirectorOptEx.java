// Copyright 2026 Picomisu contributors
// SPDX-License-Identifier: Apache-2.0

package com.android.server.display;

import android.content.Context;
import android.os.DebugSmtEx;

/**
 * Reconstructed from the PICO OS 5.13.7 factory services.
 *
 * @hide
 */
public interface IDisplayModeDirectorOptEx {

    public static final class Vote {
        public static final int PRIORITY_THERMAL_MODE = 19;
    }

    default void init(DisplayModeDirector displayModeDirector, Context context) {
        DebugSmtEx.printDefaultFunInfo(getClass());
    }

    default void setDefaultPeakRefreshRate(float defaultPeakRefreshRate) {
        DebugSmtEx.printDefaultFunInfo(getClass());
    }

    default void updateThermalModeLocked(float peakRefreshRate) {
        DebugSmtEx.printDefaultFunInfo(getClass());
    }
}
