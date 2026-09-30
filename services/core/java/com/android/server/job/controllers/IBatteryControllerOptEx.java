// Copyright 2026 Picomisu contributors
// SPDX-License-Identifier: Apache-2.0

package com.android.server.job.controllers;

import android.os.DebugSmtEx;

/**
 * Smartisan battery job controller optimization implemented by the optional sys services JAR
 * ({@code ISysJobFactory.getBatteryControllerOptEx()}). Reconstructed from the PICO OS 5.13.7
 * factory services, with its factory default implementations.
 *
 * @hide
 */
public interface IBatteryControllerOptEx {
    default void reportNewChargingState(boolean charging, boolean batteryNotLow,
            boolean powerConnected) {
        DebugSmtEx.printDefaultFunInfo(getClass());
    }
}
