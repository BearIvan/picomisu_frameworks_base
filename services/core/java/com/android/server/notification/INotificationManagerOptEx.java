// Copyright 2026 Picomisu contributors
// SPDX-License-Identifier: Apache-2.0

package com.android.server.notification;

import android.content.IntentFilter;
import android.os.DebugSmtEx;

/**
 * Reconstructed from the PICO OS 5.13.7 factory services.
 *
 * @hide
 */
public interface INotificationManagerOptEx {
    default void addAction(IntentFilter filter) {
        DebugSmtEx.printDefaultFunInfo(getClass());
    }

    default void receiveAction(String action) {
        DebugSmtEx.printDefaultFunInfo(getClass());
    }

    default boolean inSleepMode() {
        DebugSmtEx.printDefaultFunInfo(getClass());
        return false;
    }
}
