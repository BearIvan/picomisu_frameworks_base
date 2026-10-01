// Copyright 2026 Picomisu contributors
// SPDX-License-Identifier: Apache-2.0

package smartisanos.os;

import smartisanos.util.FeatLog;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.List;

/**
 * Reads the Smartisan kernel CPU idle statistics. Reconstructed from the PICO OS 5.13.7 factory
 * framework.
 *
 * @hide
 */
public class CpuFileReader {
    private static final String TAG = "CpuFileReader";
    private static final String PATH_CPU_IDLE_STATE = "/proc/cpu_idle_stat";
    private static boolean noNeedReadIdleFile = false;

    public static List<Long> getCpuIdleSmt() {
        BufferedReader in = null;
        List<Long> idleList = new ArrayList<>();
        try {
            in = new BufferedReader(new FileReader(PATH_CPU_IDLE_STATE));
            String cmdline;
            while ((cmdline = in.readLine()) != null) {
                idleList.add(Long.valueOf(cmdline));
            }
        } catch (Exception e) {
            FeatLog.e(TAG, "FEAT_CPU_IDLE_STATISTICS", 0,
                    "getCpuIdleSmt Exception=" + e.getMessage());
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (Exception e) {
                }
            }
        }
        return idleList;
    }

    public static long getCpuTotalIdleSmt() {
        BufferedReader in = null;
        long totalIdle = -1;
        try {
            in = new BufferedReader(new FileReader(PATH_CPU_IDLE_STATE));
            String cmdline = in.readLine();
            if (cmdline != null) {
                totalIdle = Long.valueOf(cmdline);
            }
        } catch (Exception e) {
            FeatLog.e(TAG, "FEAT_CPU_IDLE_STATISTICS", 0,
                    "getCpuTotalIdleSmt Exception=" + e.getMessage());
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (Exception e) {
                }
            }
        }
        return totalIdle;
    }

    public static long replaceOriginalIdleTime(long srcIdleTime) {
        if (noNeedReadIdleFile) {
            return srcIdleTime;
        }
        long totalIdle = getCpuTotalIdleSmt();
        if (totalIdle == -1) {
            noNeedReadIdleFile = true;
            FeatLog.w(TAG, "FEAT_CPU_IDLE_STATISTICS", 1,
                    "No need check cpu_idle_stat, maybe no implement here");
            return srcIdleTime;
        }
        if (totalIdle <= 0) {
            FeatLog.w(TAG, "FEAT_CPU_IDLE_STATISTICS", 2,
                    "get original idle time, because total idle=" + totalIdle);
            return srcIdleTime;
        }
        return totalIdle;
    }
}
