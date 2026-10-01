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
public interface IFreezeStats {
    default void startRecording(IApplicationFreezer.FreezeReason reason, String processName) {
        DebugSmtEx.printDefaultFunInfo(getClass());
    }

    default void endRecording(IApplicationFreezer.UnfreezeReason reason) {
        DebugSmtEx.printDefaultFunInfo(getClass());
    }

    default int getWeightedAvgScore(String procName) {
        DebugSmtEx.printDefaultFunInfo(getClass());
        return 0;
    }

    default String getProcessName() {
        DebugSmtEx.printDefaultFunInfo(getClass());
        return "";
    }

    default void dumpFreezeStats(PrintWriter pw) {
        DebugSmtEx.printDefaultFunInfo(getClass());
    }
}
