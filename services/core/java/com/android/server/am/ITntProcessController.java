// Copyright 2026 Picomisu contributors
// SPDX-License-Identifier: Apache-2.0

package com.android.server.am;

import android.os.DebugSmtEx;
import java.io.PrintWriter;

/**
 * Reconstructed from the PICO OS 5.13.7 factory services.
 *
 * @hide
 */
public interface ITntProcessController {
    default void shutDownSaveTntProcessMessage() {
        DebugSmtEx.printDefaultFunInfo(getClass());
    }

    default void updateTntProcessMessage() {
        DebugSmtEx.printDefaultFunInfo(getClass());
    }

    default void removeTntProcess(String processName) {
        DebugSmtEx.printDefaultFunInfo(getClass());
    }

    default void recordTntProcessVisible(ProcessRecord app) {
        DebugSmtEx.printDefaultFunInfo(getClass());
    }

    default void recordTntProcessInvisible(ProcessRecord app) {
        DebugSmtEx.printDefaultFunInfo(getClass());
    }

    default void recordKillTntProcess(ProcessRecord app) {
        DebugSmtEx.printDefaultFunInfo(getClass());
    }

    default void dump(PrintWriter pw) {
        DebugSmtEx.printDefaultFunInfo(getClass());
    }

    default int getTntProcessFraction(String processName) {
        DebugSmtEx.printDefaultFunInfo(getClass());
        return 0;
    }
}
