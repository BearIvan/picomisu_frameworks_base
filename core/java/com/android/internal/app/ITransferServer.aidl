// Copyright 2026 Picomisu contributors
// SPDX-License-Identifier: Apache-2.0
// Reconstructed from the factory PICO OS 5.13.7 DEX by tools/reconstruct-pico-aidl.py.
package com.android.internal.app;

import android.app.AppMainMsgInfo;

/** @hide */
interface ITransferServer {
    oneway void reportAlarm(int pid, int type, int appReasonIndex, int switchWindow, int jankThreshold, long maxDuration, long totalDuration, long endWallTime, String name, int displayMode);
    oneway void reportGameBalanceConfig(String layerName, String config1, String config2, String config3, String config4, double litCpuLoad, double bigCpuLoad);
    oneway void reportGameBalanceFps(String layerName, String avgFps);
    oneway void reportJunk(int pid, int fps, int switchWindow, long jankDuration, long totalDuration, long endWallTime, String name);
    oneway void reportFps(int pid, double fps, String name, int displayMode, long duration, int type);
    oneway void reportInputHang(in List<String> touchWindows, in float[] touchPositions, long responseTime);
    oneway void requestGameAtrace(String traceName);
    oneway void reportAppMainTerribleMsg(in List<AppMainMsgInfo> msgInfos, int uid, long endWallTime);
    oneway void reportUnityFpsList(int pid, in int[] fpsList, int displayMode);
    oneway void reportFirstFrameCompleted(int pid, long completedTime);
    oneway void reportAlgCostBad(int imu_cost, int image_cost);
    oneway void startPerfetto(int autoDump);
    oneway void dumpPerfetto(String fileName);
    oneway void startPerfettoForce(int autoDump);
    int startPerfettoFromMTP(String packageName, int currentFpsMode, int type, int value, int count, boolean forceDumpFlag);
    int dumpPerfettoFromMTP(String packageName, int currentFpsMode, int type, int value, int count, boolean isUserReport, boolean forceDumpFlag, long frameNumber);
    int dumpSystemInfo(String packageName, int currentFpsMode, int type, int value, int count);
    oneway void startPerfettoFromUserReport(String packageName);
    oneway void reportFocusAppChanged(String packageName);
    oneway void reportEvent(int eventCode, int reportCode, float ratio, int reasonCode, String reason, int reportCount, in long[] returnTimeArray);
    float[] getMuduleEventInfo(int moduleCode);
    oneway void chmodSyslogDir();
    oneway void reportThreadTID(int pid, int tid, String threadName);
    float getGPUUtilization(int targetFPS, int currentFPS);
    float getGPUPeak();
    int setPerfettoStatusForLab(String packageName, int currentFpsMode, int type, int value, int count);
    int[] getGPUInfo();
    int dumpSchedInfoFromMTP(int type, int currentFpsMode, int count, int cost);
    int getGPULoad();
    oneway void updateAppDisplayRefresh(String packageName, int displayRefresh, float avgFps, float jankScale, boolean requestDowngrade);
    oneway void reportVSTStatus(int windowCount, int vstStatus);
}
