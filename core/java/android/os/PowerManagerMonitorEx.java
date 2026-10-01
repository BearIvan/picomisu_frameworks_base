// Copyright 2026 Picomisu contributors
// SPDX-License-Identifier: Apache-2.0

package android.os;

/**
 * Smartisan monitor client part of {@link PowerManager}
 * (factory PICO OS 5.13.7 {@code android.os.PowerManagerMonitorEx}; nothing in the factory jars
 * creates it).
 *
 * @hide
 */
public class PowerManagerMonitorEx {
    private PowerManager mPowerManager;
    private IPowerManagerMonitorEx mMonitorEx;

    public PowerManagerMonitorEx(PowerManager powerManager) {
        mPowerManager = powerManager;
    }

    private IPowerManagerMonitorEx getServiceMonitorEx() {
        return mMonitorEx;
    }
}
