// Copyright 2026 Picomisu contributors
// SPDX-License-Identifier: Apache-2.0

package com.android.server;

import android.os.DebugSmtEx;

import java.util.List;

/**
 * Smartisan application prefetch manager implemented by the optional sys services JAR.
 * Reconstructed from the PICO OS 5.13.7 factory services; only the methods reached by the
 * ported factory code are present, with their factory default implementations.
 *
 * @hide
 */
public interface IPrefetchManagerService {
    default void updatePrefetchApp(List<String> packageNames, int flag) {
        DebugSmtEx.printDefaultFunInfo(getClass());
    }

    default boolean getPrefetchEnable() {
        DebugSmtEx.printDefaultFunInfo(getClass());
        return false;
    }
}
