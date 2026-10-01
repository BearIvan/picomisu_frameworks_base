// Copyright 2026 Picomisu contributors
// SPDX-License-Identifier: Apache-2.0

package com.android.server;

import android.os.DebugSmtEx;

/**
 * Reconstructed from the PICO OS 5.13.7 factory services.
 *
 * @hide
 */
public interface IAtraceStatusMonitor {
    default void addAppCrash(String packageName, int uid) {
        DebugSmtEx.printDefaultFunInfo(getClass());
    }
}
