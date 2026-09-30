// Copyright 2026 Picomisu contributors
// SPDX-License-Identifier: Apache-2.0

package com.android.server;

import android.app.IActivityLifeCycleObserver;
import android.app.IAppStartEventObserver;
import android.os.DebugSmtEx;

/**
 * Smartisan activity manager optimizations implemented by the optional sys services JAR.
 * Reconstructed from the PICO OS 5.13.7 factory services; only the methods reached by the
 * ported factory code are present, with their factory default implementations.
 *
 * @hide
 */
public interface IActivityManagerOptEx {
    default void registerActivityLifeCycleObserver(IActivityLifeCycleObserver observer) {
        DebugSmtEx.printDefaultFunInfo(getClass());
    }

    default void unregisterActivityLifeCycleObserver(IActivityLifeCycleObserver observer) {
        DebugSmtEx.printDefaultFunInfo(getClass());
    }

    default void registerAppStartEventObserver(IAppStartEventObserver observer) {
        DebugSmtEx.printDefaultFunInfo(getClass());
    }

    default void unregisterAppStartEventObserver(IAppStartEventObserver observer) {
        DebugSmtEx.printDefaultFunInfo(getClass());
    }
}
