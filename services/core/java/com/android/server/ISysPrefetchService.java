// Copyright 2026 Picomisu contributors
// SPDX-License-Identifier: Apache-2.0

package com.android.server;

import java.util.List;

/**
 * Smartisan system prefetch service implemented by the optional sys services JAR.
 * Reconstructed from the PICO OS 5.13.7 factory services; only the methods reached by the
 * ported factory code are present, with their factory default (empty) implementations.
 *
 * @hide
 */
public interface ISysPrefetchService {
    default void sendFreezeCurrentPrefetchMsg(int pid) {
    }

    default void updatePrefetchApps(List<String> needPrefetchApps, int flag) {
    }
}
