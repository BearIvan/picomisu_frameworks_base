// Copyright 2026 Picomisu contributors
// SPDX-License-Identifier: Apache-2.0

package com.android.server.power;

import android.content.Context;
import android.content.IntentFilter;
import android.os.DebugSmtEx;

/**
 * Reconstructed from the PICO OS 5.13.7 factory services.
 *
 * @hide
 */
public interface IPowerManagerOptEx {
    public static final int MSG_DISPLAY_CHANGE_UPDATE_WAKELOCKS = 200;

    default void init(Context context, PowerManagerService service) {
        DebugSmtEx.printDefaultFunInfo(getClass());
    }

    default void systemReady() {
        DebugSmtEx.printDefaultFunInfo(getClass());
    }

    default void registerSleepMode(IntentFilter filter) {
        DebugSmtEx.printDefaultFunInfo(getClass());
    }

    default void updateWakeLockAcquireStateLocked(PowerManagerService.WakeLock wakeLock) {
        DebugSmtEx.printDefaultFunInfo(getClass());
    }

    default void updateWakelockDisabledStateDelay() {
        DebugSmtEx.printDefaultFunInfo(getClass());
    }

    default boolean controlPartialWakeLock(PowerManagerService.WakeLock wakeLock) {
        DebugSmtEx.printDefaultFunInfo(getClass());
        return false;
    }

    default boolean controlScreenWakeLock(PowerManagerService.WakeLock wakeLock) {
        DebugSmtEx.printDefaultFunInfo(getClass());
        return false;
    }

    default boolean getScreenLocked() {
        DebugSmtEx.printDefaultFunInfo(getClass());
        return false;
    }

    default boolean uidTop(int procState, boolean visible, boolean screenLocked) {
        DebugSmtEx.printDefaultFunInfo(getClass());
        return false;
    }

    default void uidFrozen(int uid, boolean frozen) {
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

    default void updateReleaseWakeLockTimeLocked(String wakeName) {
        DebugSmtEx.printDefaultFunInfo(getClass());
    }
}
