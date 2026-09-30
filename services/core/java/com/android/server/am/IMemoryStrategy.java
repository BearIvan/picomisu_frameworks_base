// Copyright 2026 Picomisu contributors
// SPDX-License-Identifier: Apache-2.0

package com.android.server.am;

import android.os.DebugSmtEx;

/**
 * Smartisan memory strategy implemented by the optional sysmonitor services JAR.
 * Reconstructed from the PICO OS 5.13.7 factory services; only the methods reached by the
 * ported factory code are present, with their factory default implementations.
 *
 * @hide
 */
public interface IMemoryStrategy {
    default void backtraceDoneInform(String ProcessName, int pid) {
        DebugSmtEx.printDefaultFunInfo(getClass());
    }
}
