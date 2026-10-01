// Copyright 2026 Picomisu contributors
// SPDX-License-Identifier: Apache-2.0

package com.android.internal.os;

import android.os.Process;
import android.os.StrictMode;
import android.os.SystemClock;

import smartisanos.os.CpuFileReader;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Smartisan extension of {@link ProcessCpuTracker}. Reconstructed from the PICO OS 5.13.7
 * factory framework.
 *
 * @hide
 */
public class ProcessCpuTrackerSmtBase {
    private ProcessCpuTracker mProcessCpuTracker;
    public UidTracker mUidTracker = new UidTracker();

    public ProcessCpuTrackerSmtBase(ProcessCpuTracker processCpuTracker) {
        mProcessCpuTracker = processCpuTracker;
    }

    public int collectProcStats() {
        final StrictMode.ThreadPolicy savedPolicy = StrictMode.allowThreadDiskReads();
        try {
            mProcessCpuTracker.mCurPids = mProcessCpuTracker.collectStats("/proc", -1,
                    mProcessCpuTracker.mFirst, mProcessCpuTracker.mCurPids,
                    mProcessCpuTracker.mProcStats);
            return mProcessCpuTracker.countStats();
        } finally {
            StrictMode.setThreadPolicy(savedPolicy);
        }
    }

    public void updateReportTaskState(ProcessCpuTracker.Stats st, char state) {
        synchronized (st.taskStateLock) {
            st.taskState = state;
            if (state == 'Z') {
                if (st.zombie_start == 0) {
                    st.zombie_start = SystemClock.uptimeMillis();
                }
            } else {
                st.zombie_start = 0;
            }
        }
    }

    /** @hide */
    public class UidTracker {
        private static final boolean DEBUG = false;
        private static final String TAG = "UidCpuTracker";

        private int mLastUid;
        private long mLastUidUsage;
        private long mLastTotalUsage;
        private long mLastSystemUsage;
        private int mCpuUsageRatio;
        private int mSystemRatio;
        private int mUidRatio;
        private int mUidRatioNoneSystem;

        public int getTotalCpuPercentExt() {
            if (SystemClock.uptimeMillis() - mProcessCpuTracker.mLastSampleTime < 10000) {
                return mCpuUsageRatio;
            }
            final long[] sysCpu = mProcessCpuTracker.mSystemCpuData;
            int relUserTime = 0;
            int relSystemTime = 0;
            int relIoWaitTime = 0;
            int relIrqTime = 0;
            int relSoftIrqTime = 0;
            int relIdleTime = 0;
            if (Process.readProcFile("/proc/stat", ProcessCpuTracker.SYSTEM_CPU_FORMAT,
                    null, sysCpu, null)) {
                final long usertime = sysCpu[0] + sysCpu[1];
                final long systemtime = sysCpu[2];
                final long idletime = CpuFileReader.replaceOriginalIdleTime(sysCpu[3]);
                final long iowaittime = sysCpu[4];
                final long irqtime = sysCpu[5];
                final long softirqtime = sysCpu[6];
                relUserTime = (int) (usertime - mProcessCpuTracker.mBaseUserTime);
                relSystemTime = (int) (systemtime - mProcessCpuTracker.mBaseSystemTime);
                relIoWaitTime = (int) (iowaittime - mProcessCpuTracker.mBaseIoWaitTime);
                relIrqTime = (int) (irqtime - mProcessCpuTracker.mBaseIrqTime);
                relSoftIrqTime = (int) (softirqtime - mProcessCpuTracker.mBaseSoftIrqTime);
                relIdleTime = (int) (idletime - mProcessCpuTracker.mBaseIdleTime);
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

        public long getCpuForUid(int uid) {
            final String cpuUsageFile = "/acct" + (uid > 0 ? "/uid_" + uid : "")
                    + "/cpuacct.usage";
            final long[] data = new long[1];
            final int[] format = new int[] { Process.PROC_SPACE_TERM | Process.PROC_OUT_LONG };
            if (Process.readProcFile(cpuUsageFile, format, null, data, null)) {
                return data[0];
            }
            return 0;
        }

        public long getCpuTimeForSystem() {
            long systemUsage = getCpuForUid(Process.SYSTEM_UID);
            // The factory sums the system uid, uid 943, the radio uid and the media uid.
            return systemUsage + getCpuForUid(943) + getCpuForUid(Process.PHONE_UID)
                    + getCpuForUid(Process.MEDIA_UID);
        }

        public void updatePidsCpuUsage() {
            updateCpuUsage();
            mProcessCpuTracker.mCurPids = mProcessCpuTracker.collectStats("/proc", -1,
                    mProcessCpuTracker.mFirst, mProcessCpuTracker.mCurPids,
                    mProcessCpuTracker.mProcStats);
            mProcessCpuTracker.mWorkingProcsSorted = false;
        }

        public int getWorkingStatsCount() {
            sortPidsCpuUsage();
            return mProcessCpuTracker.mWorkingProcs.size();
        }

        public void sortPidsCpuUsage() {
            if (!mProcessCpuTracker.mWorkingProcsSorted) {
                mProcessCpuTracker.mWorkingProcs.clear();
                final int N = mProcessCpuTracker.mProcStats.size();
                for (int i = 0; i < N; i++) {
                    ProcessCpuTracker.Stats stats = mProcessCpuTracker.mProcStats.get(i);
                    if (stats.working) {
                        mProcessCpuTracker.mWorkingProcs.add(stats);
                    }
                }
                Collections.sort(mProcessCpuTracker.mWorkingProcs,
                        ProcessCpuTracker.sLoadComparator);
                mProcessCpuTracker.mWorkingProcsSorted = true;
            }
        }

        public void sortStatsWorkingThreads(ProcessCpuTracker.Stats stats) {
            if (stats.threadStats != null && stats.threadStats.size() > 1) {
                stats.workingThreads.clear();
                final int M = stats.threadStats.size();
                for (int j = 0; j < M; j++) {
                    ProcessCpuTracker.Stats tstats = stats.threadStats.get(j);
                    if (tstats.working) {
                        stats.workingThreads.add(tstats);
                    }
                }
                Collections.sort(stats.workingThreads, ProcessCpuTracker.sLoadComparator);
            }
        }

        public int updateCpuUsage() {
            mProcessCpuTracker.mLastSampleTime = mProcessCpuTracker.mCurrentSampleTime;
            mProcessCpuTracker.mCurrentSampleTime = SystemClock.uptimeMillis();
            mProcessCpuTracker.mLastSampleRealTime = mProcessCpuTracker.mCurrentSampleRealTime;
            mProcessCpuTracker.mCurrentSampleRealTime = SystemClock.elapsedRealtime();
            final long[] sysCpu = mProcessCpuTracker.mSystemCpuData;
            if (Process.readProcFile("/proc/stat", ProcessCpuTracker.SYSTEM_CPU_FORMAT,
                    null, sysCpu, null)) {
                final long usertime = sysCpu[0] + sysCpu[1];
                final long systemtime = sysCpu[2];
                final long idletime = CpuFileReader.replaceOriginalIdleTime(sysCpu[3]);
                final long iowaittime = sysCpu[4];
                final long irqtime = sysCpu[5];
                final long softirqtime = sysCpu[6];
                mProcessCpuTracker.mRelUserTime =
                        (int) (usertime - mProcessCpuTracker.mBaseUserTime);
                mProcessCpuTracker.mRelSystemTime =
                        (int) (systemtime - mProcessCpuTracker.mBaseSystemTime);
                mProcessCpuTracker.mRelIoWaitTime =
                        (int) (iowaittime - mProcessCpuTracker.mBaseIoWaitTime);
                mProcessCpuTracker.mRelIrqTime =
                        (int) (irqtime - mProcessCpuTracker.mBaseIrqTime);
                mProcessCpuTracker.mRelSoftIrqTime =
                        (int) (softirqtime - mProcessCpuTracker.mBaseSoftIrqTime);
                mProcessCpuTracker.mRelIdleTime =
                        (int) (idletime - mProcessCpuTracker.mBaseIdleTime);
                mProcessCpuTracker.mBaseUserTime = usertime;
                mProcessCpuTracker.mBaseSystemTime = systemtime;
                mProcessCpuTracker.mBaseIoWaitTime = iowaittime;
                mProcessCpuTracker.mBaseIrqTime = irqtime;
                mProcessCpuTracker.mBaseSoftIrqTime = softirqtime;
                mProcessCpuTracker.mBaseIdleTime = idletime;
            }
            final int totalTime = mProcessCpuTracker.mRelUserTime
                    + mProcessCpuTracker.mRelSystemTime + mProcessCpuTracker.mRelIoWaitTime
                    + mProcessCpuTracker.mRelIrqTime + mProcessCpuTracker.mRelSoftIrqTime
                    + mProcessCpuTracker.mRelIdleTime;
            if (totalTime > 0) {
                mCpuUsageRatio = ((totalTime - mProcessCpuTracker.mRelIdleTime) * 100)
                        / totalTime;
            } else {
                mCpuUsageRatio = -1;
            }
            return mCpuUsageRatio;
        }

        public void updateForegroundUsage(int lastUid, int nextUid, boolean computeSystem) {
            final long systemUsage = computeSystem ? getCpuTimeForSystem() : -1;
            final long totalCpu = getCpuForUid(-1);
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

        public void getUidUsage(HashMap<Integer, Long> uids, boolean includeSystem) {
            if (includeSystem) {
                uids.put(Process.SYSTEM_UID, new Long(0));
                uids.put(943, new Long(0));
                uids.put(Process.PHONE_UID, new Long(0));
                uids.put(Process.MEDIA_UID, new Long(0));
            } else {
                uids.remove(Process.SYSTEM_UID);
                uids.remove(943);
                uids.remove(Process.PHONE_UID);
                uids.remove(Process.MEDIA_UID);
            }
            for (Map.Entry<Integer, Long> entry : uids.entrySet()) {
                int uid = entry.getKey();
                uids.put(uid, new Long(getCpuForUid(uid)));
            }
        }

        public void updateUidUsage(HashMap<Integer, Long> cpus, HashMap<Integer, Long> usages) {
            usages.clear();
            for (Map.Entry<Integer, Long> entry : cpus.entrySet()) {
                int uid = entry.getKey();
                long lastCpu = entry.getValue();
                long curCpu = getCpuForUid(uid);
                usages.put(uid, new Long(curCpu - lastCpu));
                cpus.put(uid, new Long(curCpu));
            }
        }

        @Override
        public String toString() {
            return "ProcessCpuTracker.UidTracker{, mLastUid=" + mLastUid
                    + ", mLastUidUsage+" + mLastUidUsage
                    + ", mLastTotalUsage=" + mLastTotalUsage
                    + ", mLastSystemUsage=" + mLastSystemUsage
                    + ", mCpuUsageRatio=" + mCpuUsageRatio
                    + ", mSystemRatio=" + mSystemRatio
                    + ", mUidRatio=" + mUidRatio
                    + ", mUidRatioNoneSystem=" + mUidRatioNoneSystem + " }";
        }
    }
}
