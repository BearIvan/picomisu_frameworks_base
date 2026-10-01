// Copyright 2026 Picomisu contributors
// SPDX-License-Identifier: Apache-2.0

package com.android.server;

import android.app.IMemClient;
import android.os.DebugSmtEx;

/**
 * Reconstructed from the PICO OS 5.13.7 factory services.
 *
 * @hide
 */
public interface IMemMonitor {
    default void cropHprofDone(String path, boolean delete) {
        DebugSmtEx.printDefaultFunInfo(getClass());
    }

    default void createHprof(int pid, String processName, IMemClient client, long dalvikAlloc, long dalvikMax) {
        DebugSmtEx.printDefaultFunInfo(getClass());
    }

    default void checkHprof() {
        DebugSmtEx.printDefaultFunInfo(getClass());
    }

    default boolean isReportMemUsage() {
        DebugSmtEx.printDefaultFunInfo(getClass());
        return false;
    }

    default void saveLastWorst(int pid, String name, long pss, long now) {
        DebugSmtEx.printDefaultFunInfo(getClass());
    }
}
