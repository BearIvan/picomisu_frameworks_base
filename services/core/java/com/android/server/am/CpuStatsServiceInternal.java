// Copyright 2026 Picomisu contributors
// SPDX-License-Identifier: Apache-2.0

package com.android.server.am;

import android.os.DebugSmtEx;
import smartisanos.os.SimpleCpuTracker;

/**
 * Reconstructed from the PICO OS 5.13.7 factory services.
 *
 * @hide
 */
public interface CpuStatsServiceInternal {

    public interface CpuStatsHistoryObserver {
        default void onCpuStatsHistory(CpuStatsServiceInternal stats) {
            DebugSmtEx.printDefaultFunInfo(getClass());
        }
    }

    default void addCpuStatsHistoryObserver(CpuStatsHistoryObserver b) {
        DebugSmtEx.printDefaultFunInfo(getClass());
    }

    default long getCurrentIndex() {
        DebugSmtEx.printDefaultFunInfo(getClass());
        return 0L;
    }

    default SimpleCpuTracker.CpuStatsInfo getIndex(long index) {
        DebugSmtEx.printDefaultFunInfo(getClass());
        return null;
    }
}
