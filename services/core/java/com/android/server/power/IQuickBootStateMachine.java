// Copyright 2026 Picomisu contributors
// SPDX-License-Identifier: Apache-2.0

package com.android.server.power;

import android.os.DebugSmtEx;
import android.os.Handler;
import com.android.server.policy.WindowManagerPolicy;

/**
 * Reconstructed from the PICO OS 5.13.7 factory services.
 *
 * @hide
 */
public interface IQuickBootStateMachine {
    public static final String WAKE_DETAIL_ANIM = "quickBoot";
    public static final int WAKE_REASON_QB_SHUTDOWN = 101;

    default boolean goToQuickBootShutdown(String shutdownReason) {
        DebugSmtEx.printDefaultFunInfo(getClass());
        return false;
    }

    default boolean goToQuickBoot(WindowManagerPolicy.WindowManagerFuncs windowManagerFuncs) {
        DebugSmtEx.printDefaultFunInfo(getClass());
        return false;
    }

    default boolean handleQBPowerKeyUp() {
        DebugSmtEx.printDefaultFunInfo(getClass());
        return false;
    }

    default boolean sendPowerLongPressMsg(int msgPowerLongPress, Handler handle, long defined_time) {
        DebugSmtEx.printDefaultFunInfo(getClass());
        return false;
    }

    default boolean isInQBShutdownState() {
        DebugSmtEx.printDefaultFunInfo(getClass());
        return false;
    }

    default boolean isInQBChargingAnim() {
        DebugSmtEx.printDefaultFunInfo(getClass());
        return false;
    }

    default boolean goToSleepByQB() {
        DebugSmtEx.printDefaultFunInfo(getClass());
        return false;
    }

    default boolean isInQBLightOn() {
        DebugSmtEx.printDefaultFunInfo(getClass());
        return false;
    }
}
