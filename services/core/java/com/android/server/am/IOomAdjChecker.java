// Copyright 2026 Picomisu contributors
// SPDX-License-Identifier: Apache-2.0

package com.android.server.am;

import android.os.DebugSmtEx;

/**
 * Reconstructed from the PICO OS 5.13.7 factory services.
 *
 * @hide
 */
public interface IOomAdjChecker {
    default void scheduleOomAdjCheck(int delay) {
        DebugSmtEx.printDefaultFunInfo(getClass());
    }

    default void setDebugLowMem(boolean debug) {
        DebugSmtEx.printDefaultFunInfo(getClass());
    }
}
