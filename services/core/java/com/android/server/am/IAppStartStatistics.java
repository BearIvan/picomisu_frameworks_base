// Copyright 2026 Picomisu contributors
// SPDX-License-Identifier: Apache-2.0

package com.android.server.am;

import android.app.IAppStartEventObserver;
import android.os.DebugSmtEx;

/**
 * Reconstructed from the PICO OS 5.13.7 factory services.
 *
 * @hide
 */
public interface IAppStartStatistics {
    default void init(ActivityManagerService ams) {
        DebugSmtEx.printDefaultFunInfo(getClass());
    }

    default void registerAppStartEventObserver(IAppStartEventObserver observe) {
        DebugSmtEx.printDefaultFunInfo(getClass());
    }

    default void unregisterAppStartEventObserver(IAppStartEventObserver observe) {
        DebugSmtEx.printDefaultFunInfo(getClass());
    }

    default void updateSystemUidProcStateChange(ProcessRecord app, int procState) {
        DebugSmtEx.printDefaultFunInfo(getClass());
    }
}
