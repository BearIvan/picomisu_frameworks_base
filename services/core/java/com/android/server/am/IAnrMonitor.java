// Copyright 2026 Picomisu contributors
// SPDX-License-Identifier: Apache-2.0

package com.android.server.am;

import android.app.ISysClient;
import android.os.DebugSmtEx;

/**
 * Smartisan ANR monitor implemented by the optional sysmonitor services JAR. Reconstructed
 * from the PICO OS 5.13.7 factory services; only the methods reached by the ported factory
 * code are present, with their factory default implementations.
 *
 * @hide
 */
public interface IAnrMonitor {
    default void addClient(int pid, ISysClient client) {
        DebugSmtEx.printDefaultFunInfo(getClass());
    }
}
