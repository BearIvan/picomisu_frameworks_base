// Copyright 2026 Picomisu contributors
// SPDX-License-Identifier: Apache-2.0

package com.android.server;

import android.os.DebugSmtEx;

import java.util.List;

/**
 * Smartisan main-thread slow operation transfer controller implemented by the optional
 * sysmonitor services JAR. Reconstructed from the PICO OS 5.13.7 factory services; only the
 * methods reached by the ported factory code are present, with their factory default
 * implementations.
 *
 * @hide
 */
public interface ITransferController {
    default void setAppSlowMainOperation(List<String> operations, int index) {
        DebugSmtEx.printDefaultFunInfo(getClass());
    }
}
