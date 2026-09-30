// Copyright 2026 Picomisu contributors
// SPDX-License-Identifier: Apache-2.0

package com.android.server.am;

import android.content.ComponentName;
import android.os.DebugSmtEx;

import java.util.List;

/**
 * Smartisan memory process controller (keep-alive and prefetch processes) implemented by the
 * optional sys services JAR. Reconstructed from the PICO OS 5.13.7 factory services; only the
 * methods reached by the ported factory code are present, with their factory default
 * implementations.
 *
 * @hide
 */
public interface IMemoryProcessController {
    default void keepAliveBackground(ComponentName className, int pid, int flags, int level) {
        DebugSmtEx.printDefaultFunInfo(getClass());
    }

    default void clearKeepAliveProcesses() {
        DebugSmtEx.printDefaultFunInfo(getClass());
    }

    default void updatePrefetchApp(List<String> packageNames) {
        DebugSmtEx.printDefaultFunInfo(getClass());
    }
}
