// Copyright 2026 Picomisu contributors
// SPDX-License-Identifier: Apache-2.0

package com.android.server.am;

import android.os.DebugSmtEx;

/**
 * Smartisan task deep clean implemented by the optional sys services JAR. Reconstructed from
 * the PICO OS 5.13.7 factory services; only the methods reached by the ported factory code are
 * present, with their factory default implementations.
 *
 * @hide
 */
public interface ITaskDeepClean {
    default void addTdcEvent(int type, int cleanLevel, String pkg, int reason) {
        DebugSmtEx.printDefaultFunInfo(getClass());
    }
}
