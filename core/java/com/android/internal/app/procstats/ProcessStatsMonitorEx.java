// Copyright 2026 Picomisu contributors
// SPDX-License-Identifier: Apache-2.0

package com.android.internal.app.procstats;

import java.util.ArrayList;

/**
 * Smartisan system monitor extension of {@link ProcessStats}. Reconstructed from the PICO OS
 * 5.13.7 factory framework.
 *
 * @hide
 */
public class ProcessStatsMonitorEx {
    private ProcessStats mProcessStats;

    public ProcessStatsMonitorEx(ProcessStats processStats) {
        mProcessStats = processStats;
    }

    public void copyPageTypeTo(ArrayList<Integer> nodes, ArrayList<String> zones,
            ArrayList<String> labels, ArrayList<int[]> sizes) {
        nodes.addAll(mProcessStats.mPageTypeNodes);
        zones.addAll(mProcessStats.mPageTypeZones);
        labels.addAll(mProcessStats.mPageTypeLabels);
        sizes.addAll(mProcessStats.mPageTypeSizes);
    }
}
