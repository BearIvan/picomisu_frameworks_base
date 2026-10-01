// Copyright 2026 Picomisu contributors
// SPDX-License-Identifier: Apache-2.0

package com.android.server.power;

/**
 * Reconstructed from the PICO OS 5.13.7 factory services.
 *
 * @hide
 */
public class PowerManagerServiceBridgeEx extends android.os.IPowerManagerMonitorEx.Stub {
    private PowerManagerService mPowerManagerService;

    public PowerManagerServiceBridgeEx(PowerManagerService powerManagerService) {
        this.mPowerManagerService = powerManagerService;
    }
}
