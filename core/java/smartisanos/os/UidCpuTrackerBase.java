// Copyright 2026 Picomisu contributors
// SPDX-License-Identifier: Apache-2.0

package smartisanos.os;

import static android.os.Process.PROC_COMBINE;
import static android.os.Process.PROC_NEWLINE_TERM;
import static android.os.Process.PROC_OUT_LONG;
import static android.os.Process.PROC_SPACE_TERM;

import android.os.Process;
import android.os.SystemClock;
import android.util.Log;
import android.util.SparseArray;

import com.android.internal.os.ProcessCpuTracker;

/**
 * Per-uid (cpuacct) and per-cluster CPU usage tracker. Reconstructed from the PICO OS 5.13.7
 * factory framework.
 *
 * @hide
 */
public class UidCpuTrackerBase extends ProcessCpuTracker {
    protected static final String TAG = "UidCpuTracker";
    protected static final boolean DEBUG = false;
    protected static final boolean DEBUG_V1 = false;

    public static final int TOTAL_CPU_UID = -1;

    /** The "cpu" line and the eight "cpuN" lines of /proc/stat. */
    protected static final int[] SYSTEM_CPU_FORMAT_LINE = new int[] {
        PROC_SPACE_TERM | PROC_COMBINE,
        PROC_SPACE_TERM | PROC_OUT_LONG,
        PROC_SPACE_TERM | PROC_OUT_LONG,
        PROC_SPACE_TERM | PROC_OUT_LONG,
        PROC_SPACE_TERM | PROC_OUT_LONG,
        PROC_SPACE_TERM | PROC_OUT_LONG,
        PROC_SPACE_TERM | PROC_OUT_LONG,
        PROC_SPACE_TERM | PROC_OUT_LONG,
        PROC_NEWLINE_TERM,
        PROC_SPACE_TERM | PROC_COMBINE,
        PROC_SPACE_TERM | PROC_OUT_LONG,
        PROC_SPACE_TERM | PROC_OUT_LONG,
        PROC_SPACE_TERM | PROC_OUT_LONG,
        PROC_SPACE_TERM | PROC_OUT_LONG,
        PROC_SPACE_TERM | PROC_OUT_LONG,
        PROC_SPACE_TERM | PROC_OUT_LONG,
        PROC_SPACE_TERM | PROC_OUT_LONG,
        PROC_NEWLINE_TERM,
        PROC_SPACE_TERM | PROC_COMBINE,
        PROC_SPACE_TERM | PROC_OUT_LONG,
        PROC_SPACE_TERM | PROC_OUT_LONG,
        PROC_SPACE_TERM | PROC_OUT_LONG,
        PROC_SPACE_TERM | PROC_OUT_LONG,
        PROC_SPACE_TERM | PROC_OUT_LONG,
        PROC_SPACE_TERM | PROC_OUT_LONG,
        PROC_SPACE_TERM | PROC_OUT_LONG,
        PROC_NEWLINE_TERM,
        PROC_SPACE_TERM | PROC_COMBINE,
        PROC_SPACE_TERM | PROC_OUT_LONG,
        PROC_SPACE_TERM | PROC_OUT_LONG,
        PROC_SPACE_TERM | PROC_OUT_LONG,
        PROC_SPACE_TERM | PROC_OUT_LONG,
        PROC_SPACE_TERM | PROC_OUT_LONG,
        PROC_SPACE_TERM | PROC_OUT_LONG,
        PROC_SPACE_TERM | PROC_OUT_LONG,
        PROC_NEWLINE_TERM,
        PROC_SPACE_TERM | PROC_COMBINE,
        PROC_SPACE_TERM | PROC_OUT_LONG,
        PROC_SPACE_TERM | PROC_OUT_LONG,
        PROC_SPACE_TERM | PROC_OUT_LONG,
        PROC_SPACE_TERM | PROC_OUT_LONG,
        PROC_SPACE_TERM | PROC_OUT_LONG,
        PROC_SPACE_TERM | PROC_OUT_LONG,
        PROC_SPACE_TERM | PROC_OUT_LONG,
        PROC_NEWLINE_TERM,
        PROC_SPACE_TERM | PROC_COMBINE,
        PROC_SPACE_TERM | PROC_OUT_LONG,
        PROC_SPACE_TERM | PROC_OUT_LONG,
        PROC_SPACE_TERM | PROC_OUT_LONG,
        PROC_SPACE_TERM | PROC_OUT_LONG,
        PROC_SPACE_TERM | PROC_OUT_LONG,
        PROC_SPACE_TERM | PROC_OUT_LONG,
        PROC_SPACE_TERM | PROC_OUT_LONG,
        PROC_NEWLINE_TERM,
        PROC_SPACE_TERM | PROC_COMBINE,
        PROC_SPACE_TERM | PROC_OUT_LONG,
        PROC_SPACE_TERM | PROC_OUT_LONG,
        PROC_SPACE_TERM | PROC_OUT_LONG,
        PROC_SPACE_TERM | PROC_OUT_LONG,
        PROC_SPACE_TERM | PROC_OUT_LONG,
        PROC_SPACE_TERM | PROC_OUT_LONG,
        PROC_SPACE_TERM | PROC_OUT_LONG,
        PROC_NEWLINE_TERM,
        PROC_SPACE_TERM | PROC_COMBINE,
        PROC_SPACE_TERM | PROC_OUT_LONG,
        PROC_SPACE_TERM | PROC_OUT_LONG,
        PROC_SPACE_TERM | PROC_OUT_LONG,
        PROC_SPACE_TERM | PROC_OUT_LONG,
        PROC_SPACE_TERM | PROC_OUT_LONG,
        PROC_SPACE_TERM | PROC_OUT_LONG,
        PROC_SPACE_TERM | PROC_OUT_LONG,
        PROC_NEWLINE_TERM,
        PROC_SPACE_TERM | PROC_COMBINE,
        PROC_SPACE_TERM | PROC_OUT_LONG,
        PROC_SPACE_TERM | PROC_OUT_LONG,
        PROC_SPACE_TERM | PROC_OUT_LONG,
        PROC_SPACE_TERM | PROC_OUT_LONG,
        PROC_SPACE_TERM | PROC_OUT_LONG,
        PROC_SPACE_TERM | PROC_OUT_LONG,
        PROC_SPACE_TERM | PROC_OUT_LONG,
    };

    private final long[] mCpuData = new long[63];
    private final int CPU_STATE_COUNT = 7;
    private long[][] mBasePerCpusUsage = new long[8][6];
    private long[][] mRelPerCpusUsage = new long[8][8];

    long mCurrentSampleTime;
    long mLastSampleTime;
    long mCurrentSampleRealTime;
    long mLastSampleRealTime;

    protected int mLastUid;
    protected long mLastUidUsage;
    protected long mLastTotalUsage;
    protected long mLastSystemUsage;
    protected int mCpuUsageRatio;
    protected int mIoWaitRatio;
    protected int mSystemRatio;
    protected int mUidRatio;
    protected int mUidRatioNoneSystem;

    /** @hide */
    public static class CpuUsageInfo {
        public int uid;
        public long beginCpu;
        public long usage;

        public CpuUsageInfo(int u, long b, long us) {
            uid = u;
            beginCpu = b;
            usage = us;
        }

        public void reset() {
            beginCpu = 0;
            usage = 0;
        }

        @Override
        public String toString() {
            return "CpuUsageInfo{ uid=" + uid + ", beginCpu=" + beginCpu + ", usage=" + usage
                    + " }";
        }
    }

    public UidCpuTrackerBase() {
        super(false);
    }

    public int getIoWaitRatio() {
        return mIoWaitRatio;
    }

    public long getCpuForUid(int uid) {
        final String cpuUsageFile = "/acct" + (uid != TOTAL_CPU_UID ? "/uid_" + uid : "")
                + "/cpuacct.usage";
        final long[] data = new long[1];
        final int[] format = new int[] { PROC_SPACE_TERM | PROC_OUT_LONG };
        if (Process.readProcFile(cpuUsageFile, format, null, data, null)) {
            return data[0];
        }
        return 0;
    }

    public int updateCpuUsage() {
        mLastSampleTime = mCurrentSampleTime;
        mCurrentSampleTime = SystemClock.uptimeMillis();
        mLastSampleRealTime = mCurrentSampleRealTime;
        mCurrentSampleRealTime = SystemClock.elapsedRealtime();

        final long[] sysCpu = mCpuData;
        if (Process.readProcFile("/proc/stat", SYSTEM_CPU_FORMAT_LINE, null, sysCpu, null)) {
            for (int i = 0; i < sysCpu.length / CPU_STATE_COUNT; i++) {
                final long usertime = sysCpu[i * CPU_STATE_COUNT + 0]
                        + sysCpu[i * CPU_STATE_COUNT + 1];
                final long systemtime = sysCpu[i * CPU_STATE_COUNT + 2];
                final long idletime =
                        CpuFileReader.replaceOriginalIdleTime(sysCpu[i * CPU_STATE_COUNT + 3]);
                final long iowaittime = sysCpu[i * CPU_STATE_COUNT + 4];
                final long irqtime = sysCpu[i * CPU_STATE_COUNT + 5];
                final long softirqtime = sysCpu[i * CPU_STATE_COUNT + 6];
                final int index = i - 1;
                if (index < 0) {
                    mRelUserTime = (int) (usertime - mBaseUserTime);
                    mRelSystemTime = (int) (systemtime - mBaseSystemTime);
                    mRelIdleTime = (int) (idletime - mBaseIdleTime);
                    mRelIoWaitTime = (int) (iowaittime - mBaseIoWaitTime);
                    mRelIrqTime = (int) (irqtime - mBaseIrqTime);
                    mRelSoftIrqTime = (int) (softirqtime - mBaseSoftIrqTime);
                    mBaseUserTime = usertime;
                    mBaseSystemTime = systemtime;
                    mBaseIdleTime = idletime;
                    mBaseIoWaitTime = iowaittime;
                    mBaseIrqTime = irqtime;
                    mBaseSoftIrqTime = softirqtime;
                } else {
                    mRelPerCpusUsage[index][0] = usertime - mBasePerCpusUsage[index][0];
                    mRelPerCpusUsage[index][1] = systemtime - mBasePerCpusUsage[index][1];
                    mRelPerCpusUsage[index][2] = idletime - mBasePerCpusUsage[index][2];
                    mRelPerCpusUsage[index][3] = iowaittime - mBasePerCpusUsage[index][3];
                    mRelPerCpusUsage[index][4] = irqtime - mBasePerCpusUsage[index][4];
                    mRelPerCpusUsage[index][5] = softirqtime - mBasePerCpusUsage[index][5];
                    mBasePerCpusUsage[index][0] = usertime;
                    mBasePerCpusUsage[index][1] = systemtime;
                    mBasePerCpusUsage[index][2] = idletime;
                    mBasePerCpusUsage[index][3] = iowaittime;
                    mBasePerCpusUsage[index][4] = irqtime;
                    mBasePerCpusUsage[index][5] = softirqtime;
                    mRelPerCpusUsage[index][6] = mRelPerCpusUsage[index][0]
                            + mRelPerCpusUsage[index][1] + mRelPerCpusUsage[index][2]
                            + mRelPerCpusUsage[index][3] + mRelPerCpusUsage[index][4]
                            + mRelPerCpusUsage[index][5];
                    if (mRelPerCpusUsage[index][6] > 0) {
                        mRelPerCpusUsage[index][7] = ((mRelPerCpusUsage[index][6]
                                - mRelPerCpusUsage[index][2]) * 100)
                                / mRelPerCpusUsage[index][6];
                    } else {
                        mRelPerCpusUsage[index][7] = -1;
                    }
                }
            }
        }

        final int totalTime = mRelUserTime + mRelSystemTime + mRelIoWaitTime + mRelIrqTime
                + mRelSoftIrqTime + mRelIdleTime;
        if (totalTime > 0) {
            mCpuUsageRatio = ((totalTime - mRelIdleTime) * 100) / totalTime;
            mIoWaitRatio = (mRelIoWaitTime * 100) / totalTime;
        } else {
            mCpuUsageRatio = -1;
        }
        return mCpuUsageRatio;
    }

    public static void addSystemUids(SparseArray<CpuUsageInfo> uids) {
        // The factory adds the system uid, uid 943, the radio uid and the media uid.
        uids.put(Process.SYSTEM_UID, new CpuUsageInfo(Process.SYSTEM_UID, 0, 0));
        uids.put(943, new CpuUsageInfo(943, 0, 0));
        uids.put(Process.PHONE_UID, new CpuUsageInfo(Process.PHONE_UID, 0, 0));
        uids.put(Process.MEDIA_UID, new CpuUsageInfo(Process.MEDIA_UID, 0, 0));
    }

    public void getUidCpu(SparseArray<CpuUsageInfo> uids) {
        final int N = uids.size();
        for (int i = 0; i < N; i++) {
            int uid = uids.keyAt(i);
            CpuUsageInfo info = uids.valueAt(i);
            info.beginCpu = getCpuForUid(uid);
        }
    }

    public void updateUidUsage(SparseArray<CpuUsageInfo> uidCpus, boolean updateBeginCpu) {
        final int N = uidCpus.size();
        for (int i = 0; i < N; i++) {
            int uid = uidCpus.keyAt(i);
            CpuUsageInfo info = uidCpus.valueAt(i);
            long curCpu = getCpuForUid(uid);
            info.usage = curCpu > 0 ? curCpu - info.beginCpu : 0;
            if (updateBeginCpu) {
                info.beginCpu = curCpu;
            }
        }
    }

    @Override
    public String toString() {
        return "UidTracker{, mLastUid=" + mLastUid
                + ", mLastUidUsage=" + mLastUidUsage
                + ", mLastTotalUsage=" + mLastTotalUsage
                + ", mLastSystemUsage=" + mLastSystemUsage
                + ", mCpuUsageRatio=" + mCpuUsageRatio
                + ", mSystemRatio=" + mSystemRatio
                + ", mUidRatio=" + mUidRatio
                + ", mUidRatioNoneSystem=" + mUidRatioNoneSystem + " }";
    }

    public int getSilverCPUUsageRatio() {
        int ratio = -1;
        long totalTime = 0;
        long totalIdle = 0;
        for (int i = 0; i < 4; i++) {
            totalIdle += mRelPerCpusUsage[i][2];
            totalTime += mRelPerCpusUsage[i][6];
        }
        if (totalTime != 0) {
            ratio = (int) (((totalTime - totalIdle) * 100) / totalTime);
        }
        Log.i(TAG, "small cpu total = " + totalTime + " ; idle = " + totalIdle + " ; ratio = "
                + ratio + " ; mCpuUsageRatio = " + mCpuUsageRatio);
        return ratio;
    }

    public int getGoldCPUUsageRatio() {
        int ratio = -1;
        long totalTime = 0;
        long totalIdle = 0;
        for (int i = 4; i < 7; i++) {
            totalIdle += mRelPerCpusUsage[i][2];
            totalTime += mRelPerCpusUsage[i][6];
        }
        if (totalTime != 0) {
            ratio = (int) (((totalTime - totalIdle) * 100) / totalTime);
        }
        Log.i(TAG, "larger cpu total = " + totalTime + " ; idle = " + totalIdle + " ; ratio = "
                + ratio + " ; mCpuUsageRatio = " + mCpuUsageRatio);
        return ratio;
    }

    public int getPrimeCPUUsageRatio() {
        int ratio = -1;
        long totalTime = 0;
        long totalIdle = 0;
        totalIdle += mRelPerCpusUsage[7][2];
        totalTime += mRelPerCpusUsage[7][6];
        if (totalTime != 0) {
            ratio = (int) (((totalTime - totalIdle) * 100) / totalTime);
        }
        Log.i(TAG, "super cpu total = " + totalTime + " ; idle = " + totalIdle + " ; ratio = "
                + ratio + " ; mCpuUsageRatio = " + mCpuUsageRatio);
        return ratio;
    }
}
