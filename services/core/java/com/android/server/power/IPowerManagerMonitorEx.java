// Copyright 2026 Picomisu contributors
// SPDX-License-Identifier: Apache-2.0

package com.android.server.power;

import android.os.DebugSmtEx;

/**
 * Reconstructed from the PICO OS 5.13.7 factory services.
 *
 * @hide
 */
public interface IPowerManagerMonitorEx {
    default void init(PowerManagerService service) {
        DebugSmtEx.printDefaultFunInfo(getClass());
    }

    default void updateDeviceIdleModeTimeLocked(boolean enabled) {
        DebugSmtEx.printDefaultFunInfo(getClass());
    }

    default void updateLightDeviceIdleModeTimeLocked(boolean enabled) {
        DebugSmtEx.printDefaultFunInfo(getClass());
    }

    default void updateAcquireWakeLockTimeLocked(String wakeName) {
        DebugSmtEx.printDefaultFunInfo(getClass());
    }

    default void updateReleaseWakeLockTimeLocked(String wakeame) {
        DebugSmtEx.printDefaultFunInfo(getClass());
    }

    default long getLastLightDozeTime() {
        DebugSmtEx.printDefaultFunInfo(getClass());
        return 0L;
    }

    default long getLastDeepDozeTime() {
        DebugSmtEx.printDefaultFunInfo(getClass());
        return 0L;
    }

    default long getLastWakelockBlameTime() {
        DebugSmtEx.printDefaultFunInfo(getClass());
        return 0L;
    }

    default long getLastDisplayOnTime() {
        DebugSmtEx.printDefaultFunInfo(getClass());
        return 0L;
    }
}
