// Copyright 2026 Picomisu contributors
// SPDX-License-Identifier: Apache-2.0

package com.android.server;

import android.os.DebugSmtEx;

/**
 * Smartisan process cpuset resource control implemented by the optional sys services JAR.
 * Reconstructed from the PICO OS 5.13.7 factory services; only the methods reached by the
 * ported factory code are present, with their factory default implementations.
 *
 * @hide
 */
public interface ISmtResourceControl {
    default void setProcessRunningCpuset(int pid, int cpusetLevel, long timeOut, boolean force) {
        DebugSmtEx.printDefaultFunInfo(getClass());
    }
}
