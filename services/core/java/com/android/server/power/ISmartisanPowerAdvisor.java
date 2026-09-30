// Copyright 2026 Picomisu contributors
// SPDX-License-Identifier: Apache-2.0

package com.android.server.power;

import android.os.DebugSmtEx;

/**
 * Smartisan power advisor implemented by the optional sys services JAR
 * ({@code ISysSvsFactory.getSmartisanPowerAdvisorInstance()}). Reconstructed from the
 * PICO OS 5.13.7 factory services, with its factory default implementations.
 *
 * @hide
 */
public interface ISmartisanPowerAdvisor {
    default boolean powerFreezeUseScenes() {
        DebugSmtEx.printDefaultFunInfo(getClass());
        return false;
    }

    default void systemReady() {
        DebugSmtEx.printDefaultFunInfo(getClass());
    }
}
