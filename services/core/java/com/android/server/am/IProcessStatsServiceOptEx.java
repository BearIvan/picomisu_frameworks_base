// Copyright 2026 Picomisu contributors
// SPDX-License-Identifier: Apache-2.0

package com.android.server.am;

import android.os.Parcel;

/**
 * Reconstructed from the PICO OS 5.13.7 factory services.
 *
 * @hide
 */
public interface IProcessStatsServiceOptEx {
    default void init(ProcessStatsService service) {
    }

    default void pendingCommit() {
    }

    default void saveData(Parcel data) {
    }

    default void addNativeMemUsage(String name, int uid, int pid, long pss, long uss, long rss) {
    }

    default void saveDataDaily() {
    }
}
