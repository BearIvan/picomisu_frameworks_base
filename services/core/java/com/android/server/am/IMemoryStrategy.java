// Copyright 2026 Picomisu contributors
// SPDX-License-Identifier: Apache-2.0

package com.android.server.am;

import android.os.DebugSmtEx;
import com.android.internal.util.MemInfoReader;
import java.util.HashMap;

/**
 * Reconstructed from the PICO OS 5.13.7 factory services.
 *
 * @hide
 */
public interface IMemoryStrategy {
    default void backtraceDoneInform(String ProcessName, int pid) {
        DebugSmtEx.printDefaultFunInfo(getClass());
    }

    default void executeMemoryStrategy(String name, int pid, long pss, int oomAdj) {
        DebugSmtEx.printDefaultFunInfo(getClass());
    }

    default void executeMeminfoMemoryStrategy(MemInfoReader memInfo, HashMap<String, long[]> processMems, long usedPss, long cachedPss, long ionHeapOther) {
        DebugSmtEx.printDefaultFunInfo(getClass());
    }

    default void setSoundProcessMemoryStrategy(boolean flag) {
        DebugSmtEx.printDefaultFunInfo(getClass());
    }

    default void enterIdleStateInform(boolean flag) {
        DebugSmtEx.printDefaultFunInfo(getClass());
    }
}
