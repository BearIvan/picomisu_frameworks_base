// Copyright 2026 Picomisu contributors
// SPDX-License-Identifier: Apache-2.0

package com.android.server;

import android.content.Context;
import android.os.DebugSmtEx;
import java.util.List;

/**
 * Reconstructed from the PICO OS 5.13.7 factory services.
 *
 * @hide
 */
public interface ITransferController {
    public static final String SCREEN_TYPE_PHONE = "smart_phone_screen";
    public static final String SCREEN_TYPE_TNT = "smart_tnt_screen";

    default void setAppSlowMainOperation(List<String> operations, int index) {
        DebugSmtEx.printDefaultFunInfo(getClass());
    }

    default void publish(Context context, boolean always) {
        DebugSmtEx.printDefaultFunInfo(getClass());
    }

    default void shutdown() {
        DebugSmtEx.printDefaultFunInfo(getClass());
    }

    default void systemReady() {
        DebugSmtEx.printDefaultFunInfo(getClass());
    }

    default void finishBooting() {
        DebugSmtEx.printDefaultFunInfo(getClass());
    }

    default void setDoJankLog(boolean doJankLog) {
        DebugSmtEx.printDefaultFunInfo(getClass());
    }

    default void startPerfettoForce(int auto) {
        DebugSmtEx.printDefaultFunInfo(getClass());
    }
}
