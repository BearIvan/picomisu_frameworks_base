// Copyright 2026 Picomisu contributors
// SPDX-License-Identifier: Apache-2.0

package com.android.server;

import android.os.DebugSmtEx;

/**
 * Reconstructed from the PICO OS 5.13.7 factory services.
 *
 * @hide
 */
public interface ISmartMonitorController {
    default void updateSwitchStatus(long switchType, boolean open) {
        DebugSmtEx.printDefaultFunInfo(getClass());
    }

    default void updateDailyCpuUsage() {
        DebugSmtEx.printDefaultFunInfo(getClass());
    }
}
