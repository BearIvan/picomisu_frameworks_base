// Copyright 2026 Picomisu contributors
// SPDX-License-Identifier: Apache-2.0
package com.android.server;

import com.android.server.lights.Light;
import com.pico.util.IExtBase;

/**
 * PICO battery service extension (factory PICO OS 5.13.7 com.android.server.IExtBatteryService),
 * implemented by {@link ExtBatteryServiceImpl}.
 * @hide
 */
public interface IExtBatteryService extends IExtBase {
    /** End of BatteryService.processValuesLocked when a value changed. */
    void onBatteryChanged();

    /** BatteryService.processValuesLocked, after the shutdown checks. */
    void onProcessValuesLocked();

    /** End of BatteryService.onStart. */
    void onStart();

    /** BatteryService.shutdownIfNoPowerLocked: true = shut down now. */
    boolean shouldShutdownLocked();

    /** BatteryService.Led.updateLightsLocked: true = the AOSP LED logic is skipped. */
    boolean updateLightsLocked(Light batteryLight);
}
