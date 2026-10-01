// Copyright 2026 Picomisu contributors
// SPDX-License-Identifier: Apache-2.0

package smartisanos.os;

import android.os.Process;
import android.os.SystemClock;

import com.android.internal.os.ProcessCpuTracker;

import java.util.Collections;
import java.util.HashMap;

/**
 * Foreground uid CPU usage tracker. Reconstructed from the PICO OS 5.13.7 factory framework.
 *
 * @hide
 */
public class UidCpuTracker extends UidCpuTrackerBase {

    public int getTotalCpuPercentExt() {
        if (SystemClock.uptimeMillis() - mLastSampleTime < 10000) {
            return mCpuUsageRatio;
        }
        final long[] sysCpu = mSystemCpuData;
        int relUserTime = 0;
        int relSystemTime = 0;
        int relIoWaitTime = 0;
        int relIrqTime = 0;
        int relSoftIrqTime = 0;
        int relIdleTime = 0;
        if (Process.readProcFile("/proc/stat", SYSTEM_CPU_FORMAT, null, sysCpu, null)) {
            final long usertime = sysCpu[0] + sysCpu[1];
            final long systemtime = sysCpu[2];
            final long idletime = CpuFileReader.replaceOriginalIdleTime(sysCpu[3]);
            final long iowaittime = sysCpu[4];
            final long irqtime = sysCpu[5];
            final long softirqtime = sysCpu[6];
            relUserTime = (int) (usertime - mBaseUserTime);
            relSystemTime = (int) (systemtime - mBaseSystemTime);
            relIoWaitTime = (int) (iowaittime - mBaseIoWaitTime);
            relIrqTime = (int) (irqtime - mBaseIrqTime);
            relSoftIrqTime = (int) (softirqtime - mBaseSoftIrqTime);
            relIdleTime = (int) (idletime - mBaseIdleTime);
        }
        final int totalTime = relUserTime + relSystemTime + relIoWaitTime + relIrqTime
                + relSoftIrqTime + relIdleTime;
        return totalTime > 0 ? ((totalTime - relIdleTime) * 100) / totalTime : -1;
    }

    public int getUidRatio() {
        return mUidRatio;
    }

    public int getUidRatioNoneSystem() {
        return mUidRatioNoneSystem;
    }

    public int getSystemRatio() {
        return mSystemRatio;
    }

    public long getCpuTimeForSystem() {
        long systemUsage = getCpuForUid(Process.SYSTEM_UID);
        // The factory sums the system uid, uid 943, the radio uid and the media uid.
        return systemUsage + getCpuForUid(943) + getCpuForUid(Process.PHONE_UID)
                + getCpuForUid(Process.MEDIA_UID);
    }

    public void updatePidsCpuUsage() {
        updateCpuUsage();
        mCurPids = collectStats("/proc", -1, mFirst, mCurPids, mProcStats);
        mWorkingProcsSorted = false;
    }

    public int getWorkingStatsCount() {
        sortPidsCpuUsage();
        return mWorkingProcs.size();
    }

    public void sortPidsCpuUsage() {
        if (!mWorkingProcsSorted) {
            mWorkingProcs.clear();
            final int N = mProcStats.size();
            for (int i = 0; i < N; i++) {
                ProcessCpuTracker.Stats stats = mProcStats.get(i);
                if (stats.working) {
                    mWorkingProcs.add(stats);
                }
            }
            Collections.sort(mWorkingProcs, sLoadComparator);
            mWorkingProcsSorted = true;
        }
    }

    public void updateForegroundUsage(int lastUid, int nextUid, boolean computeSystem) {
        final long systemUsage = computeSystem ? getCpuTimeForSystem() : -1;
        final long totalCpu = getCpuForUid(TOTAL_CPU_UID);
        final boolean computeUid = mLastUid == lastUid && totalCpu > 0;
        final long uidCpu = computeUid ? getCpuForUid(lastUid) : -1;
        final long delaTotal = totalCpu - mLastTotalUsage;
        final long delaUid = uidCpu - mLastUidUsage;
        if (uidCpu > 0) {
            mUidRatio = (int) ((delaUid * 100) / delaTotal);
        } else {
            mUidRatio = -1;
        }
        if (systemUsage > 0 && mLastSystemUsage > 0) {
            final long noneSystemUsage = totalCpu - systemUsage;
            final long lastNSUsage = mLastTotalUsage - mLastSystemUsage;
            final long deltaSystem = systemUsage - mLastSystemUsage;
            final long deltaNS = noneSystemUsage - lastNSUsage;
            mSystemRatio = (int) ((deltaSystem * 100) / delaTotal);
            if (uidCpu > 0) {
                mUidRatioNoneSystem = (int) ((delaUid * 100) / deltaNS);
            } else {
                mUidRatioNoneSystem = -1;
            }
        } else {
            mSystemRatio = mUidRatioNoneSystem = -1;
        }
        mLastSystemUsage = systemUsage;
        mLastTotalUsage = totalCpu;
        mLastUidUsage = uidCpu;
        if (nextUid > 0 && mLastUid != nextUid) {
            mLastUid = nextUid;
            mLastUidUsage = getCpuForUid(nextUid);
        }
    }

    public static void removeSystemUids(HashMap<Integer, CpuUsageInfo> uids) {
        uids.remove(Process.SYSTEM_UID);
        uids.remove(943);
        uids.remove(Process.PHONE_UID);
        uids.remove(Process.MEDIA_UID);
    }
}
