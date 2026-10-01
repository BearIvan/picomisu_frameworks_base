// Copyright 2026 Picomisu contributors
// SPDX-License-Identifier: Apache-2.0

package smartisanos.os;

import static android.os.Process.PROC_COMBINE;
import static android.os.Process.PROC_OUT_FLOAT;
import static android.os.Process.PROC_OUT_LONG;
import static android.os.Process.PROC_SPACE_TERM;

import android.os.Process;
import android.util.Log;

/**
 * Light-weight system CPU usage tracker. Reconstructed from the PICO OS 5.13.7 factory
 * framework.
 *
 * @hide
 */
public class SimpleCpuTracker {
    private static final String TAG = "SimpleCpuTracker";
    private static final boolean DEBUG = false;

    protected static final int[] SYSTEM_CPU_FORMAT = new int[] {
        PROC_SPACE_TERM | PROC_COMBINE,
        PROC_SPACE_TERM | PROC_OUT_LONG,                // 1: user time
        PROC_SPACE_TERM | PROC_OUT_LONG,                // 2: nice time
        PROC_SPACE_TERM | PROC_OUT_LONG,                // 3: sys time
        PROC_SPACE_TERM | PROC_OUT_LONG,                // 4: idle time
        PROC_SPACE_TERM | PROC_OUT_LONG,                // 5: iowait time
        PROC_SPACE_TERM | PROC_OUT_LONG,                // 6: irq time
        PROC_SPACE_TERM | PROC_OUT_LONG                 // 7: softirq time
    };

    private final long[] mSystemCpuData = new long[7];

    protected static final int[] LOAD_AVERAGE_FORMAT = new int[] {
        PROC_SPACE_TERM | PROC_OUT_FLOAT,               // 0: 1 min
        PROC_SPACE_TERM | PROC_OUT_FLOAT,               // 1: 5 mins
        PROC_SPACE_TERM | PROC_OUT_FLOAT                // 2: 15 mins
    };

    private final float[] mLoadAverageData = new float[3];

    private float mLoad1;
    private long mBaseUserTime;
    private long mBaseSystemTime;
    private long mBaseIoWaitTime;
    private long mBaseIrqTime;
    private long mBaseSoftIrqTime;
    private long mBaseIdleTime;
    private int mCpuUsageRatio;
    protected int mIoWaitRatio;

    CpuStatsInfo mInfo = new CpuStatsInfo();

    /** @hide */
    public static class CpuStatsInfo {
        public long time;
        public int usage;
        public int iowait;
        public float avgload;
        public int[] meminfo = new int[5];

        public void set(CpuStatsInfo other) {
            usage = other.usage;
            iowait = other.iowait;
            avgload = other.avgload;
            for (int i = 0; i < 5; i++) {
                meminfo[i] = other.meminfo[i];
            }
        }
    }

    public CpuStatsInfo update() {
        mInfo.usage = updateCpuUsage();
        mInfo.iowait = mIoWaitRatio;
        mInfo.avgload = updateCpuLoadAvg();
        mInfo.meminfo[0] = updateMemStats();
        return mInfo;
    }

    public float updateCpuLoadAvg() {
        final float[] loadAverages = mLoadAverageData;
        if (Process.readProcFile("/proc/loadavg", LOAD_AVERAGE_FORMAT, null, null,
                loadAverages)) {
            mLoad1 = loadAverages[0];
        }
        return mLoad1;
    }

    public int updateMemStats() {
        return 0;
    }

    public int updateCpuUsage() {
        final long[] sysCpu = mSystemCpuData;
        if (Process.readProcFile("/proc/stat", SYSTEM_CPU_FORMAT, null, sysCpu, null)) {
            final long usertime = sysCpu[0] + sysCpu[1];
            final long systemtime = sysCpu[2];
            final long idletime = CpuFileReader.replaceOriginalIdleTime(sysCpu[3]);
            final long iowaittime = sysCpu[4];
            final long irqtime = sysCpu[5];
            final long softirqtime = sysCpu[6];

            int relUserTime = (int) (usertime - mBaseUserTime);
            int relSystemTime = (int) (systemtime - mBaseSystemTime);
            int relIoWaitTime = (int) (iowaittime - mBaseIoWaitTime);
            int relIrqTime = (int) (irqtime - mBaseIrqTime);
            int relSoftIrqTime = (int) (softirqtime - mBaseSoftIrqTime);
            int relIdleTime = (int) (idletime - mBaseIdleTime);

            mBaseUserTime = usertime;
            mBaseSystemTime = systemtime;
            mBaseIoWaitTime = iowaittime;
            mBaseIrqTime = irqtime;
            mBaseSoftIrqTime = softirqtime;
            mBaseIdleTime = idletime;

            final int totalTime = relUserTime + relSystemTime + relIoWaitTime + relIrqTime
                    + relSoftIrqTime + relIdleTime;
            if (totalTime > 0) {
                mCpuUsageRatio = ((totalTime - relIdleTime) * 100) / totalTime;
                mIoWaitRatio = (relIoWaitTime * 100) / totalTime;
            } else {
                mCpuUsageRatio = -1;
                mIoWaitRatio = -1;
                Log.w(TAG, "updateCpuUsage via /proc/stat, totalTime < 0.");
            }
        } else {
            mCpuUsageRatio = -1;
            mIoWaitRatio = -1;
        }
        return mCpuUsageRatio;
    }

    @Override
    public String toString() {
        return "SimpleCpuTracker{ }";
    }
}
