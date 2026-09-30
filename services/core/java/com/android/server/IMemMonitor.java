// Copyright 2026 Picomisu contributors
// SPDX-License-Identifier: Apache-2.0

package com.android.server;

import android.app.IMemClient;
import android.os.DebugSmtEx;

/**
 * Smartisan memory monitor (hprof collection) implemented by the optional sys services JAR.
 * Reconstructed from the PICO OS 5.13.7 factory services; only the methods reached by the
 * ported factory code are present, with their factory default implementations.
 *
 * @hide
 */
public interface IMemMonitor {
    default void cropHprofDone(String path, boolean delete) {
        DebugSmtEx.printDefaultFunInfo(getClass());
    }

    default void createHprof(int pid, String processName, IMemClient client, long dalvikAlloc,
            long dalvikMax) {
        DebugSmtEx.printDefaultFunInfo(getClass());
    }

    default void checkHprof() {
        DebugSmtEx.printDefaultFunInfo(getClass());
    }
}
