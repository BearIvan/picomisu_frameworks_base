// Copyright 2026 Picomisu contributors
// SPDX-License-Identifier: Apache-2.0

package com.android.server.am;

import android.os.DebugSmtEx;
import java.util.HashMap;
import java.util.List;

/**
 * Reconstructed from the PICO OS 5.13.7 factory services.
 *
 * @hide
 */
public interface IUidMonitorSmt {
    public static final boolean UID_MONITOR_CONTROL = true;

    default void onActiveUidAdded(int uid) {
        DebugSmtEx.printDefaultFunInfo(getClass());
    }

    default void onActiveUidRemoved(int uid) {
        DebugSmtEx.printDefaultFunInfo(getClass());
    }

    default void updateForeground(int prevUid, int nextUid) {
        DebugSmtEx.printDefaultFunInfo(getClass());
    }

    default void reportBackgroundUidUsage() {
        DebugSmtEx.printDefaultFunInfo(getClass());
    }

    default long getUidFgUsageTime(int uid) {
        DebugSmtEx.printDefaultFunInfo(getClass());
        return 0L;
    }

    default long getUidTotalUsageTime(int uid) {
        DebugSmtEx.printDefaultFunInfo(getClass());
        return 0L;
    }

    default void updateCpuBusyData(boolean cpuLastBusy) {
        DebugSmtEx.printDefaultFunInfo(getClass());
    }

    default HashMap getCurrentUidCpuUsage() {
        DebugSmtEx.printDefaultFunInfo(getClass());
        return new HashMap();
    }

    default long getIdleCpuUsageUpNow() {
        DebugSmtEx.printDefaultFunInfo(getClass());
        return 0L;
    }

    default long getTotalCpuUsageUpNow() {
        DebugSmtEx.printDefaultFunInfo(getClass());
        return 0L;
    }

    default long getTotalUidCpuUsageUpNow() {
        DebugSmtEx.printDefaultFunInfo(getClass());
        return 0L;
    }

    default void updateScreenOnCpuUsageByUid() {
        DebugSmtEx.printDefaultFunInfo(getClass());
    }

    default void updateScreenOffCpuUsageByUid() {
        DebugSmtEx.printDefaultFunInfo(getClass());
    }

    default void computeUidIOForSysevent() {
        DebugSmtEx.printDefaultFunInfo(getClass());
    }

    default List getCurrentUidCpuUsageInOrder(int count) {
        DebugSmtEx.printDefaultFunInfo(getClass());
        return null;
    }

    public static class UidCpuUsage {
        public int uid;
        public long fgCpuUsage = 0;
        public long cpuUsageByNow = 0;
        public long acctUsageBg = 0;
        public long acctUsageBgLastTime = 0;
        public long acctUsageBgScreenOn = 0;
        public long acctUsageBgScreenOnLastTime = 0;
        public long acctUsageScreenOn = 0;
        public long acctUsageScreenOnLastTime = 0;
        public HashMap<Integer, Long> freqPoints = new HashMap<>();
        public HashMap<Integer, Long> acctFreqTableBg = new HashMap<>();
        public HashMap<Integer, Long> acctFreqTableBgLastTime = new HashMap<>();
        public HashMap<Integer, Long> acctFreqTableBgScreenOn = new HashMap<>();
        public HashMap<Integer, Long> acctFreqTableBgScreenOnLastTime = new HashMap<>();

        public String toString() {
            return this.uid + "|" + this.cpuUsageByNow + "|" + this.fgCpuUsage + "|" + this.acctUsageBg + "|" + this.acctUsageBgLastTime + "|" + this.acctUsageBgScreenOn + "|" + this.acctUsageBgScreenOnLastTime + "|" + this.acctUsageScreenOn + "|" + this.acctUsageScreenOnLastTime;
        }
    }
}
