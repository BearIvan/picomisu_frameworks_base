// Copyright 2026 Picomisu contributors
// SPDX-License-Identifier: Apache-2.0

package com.android.server;

import android.os.DebugSmtEx;

/**
 * Smartisan smart service (display refresh rate and surface view tracking) implemented by the
 * optional sys services JAR. Reconstructed from the PICO OS 5.13.7 factory services; only the
 * methods reached by the ported factory code are present, with their factory default
 * implementations.
 *
 * @hide
 */
public interface ISmartService {
    default void updateFocusSurfaceViewArea(int pid, int width, int height,
            boolean currentVisible) {
        DebugSmtEx.printDefaultFunInfo(getClass());
    }

    default void onSurfaceViewVisibilityChanged(int pid, int visibility) {
        DebugSmtEx.printDefaultFunInfo(getClass());
    }

    default void parseAppRefreshRate(String jsonStr) {
        DebugSmtEx.printDefaultFunInfo(getClass());
    }
}
