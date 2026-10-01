// Copyright 2026 Picomisu contributors
// SPDX-License-Identifier: Apache-2.0

package com.android.server;

import android.app.IApplicationThread;
import android.content.Context;
import android.os.DebugSmtEx;
import android.os.Parcel;
import android.os.SystemProperties;
import com.android.server.am.ActivityManagerService;
import java.util.ArrayList;

/**
 * Reconstructed from the PICO OS 5.13.7 factory services.
 *
 * @hide
 */
public interface ISmartService {
    public static final int BACKUP_FILES = 40;
    public static final int CANCEL_ANTUTU_SPECIAL_CHEAT = 53;
    public static final int CANCEL_THERMAL_CHEAT = 51;
    public static final int CHANGE_DEX2OAT_CPUS = 700;
    public static final int CHANGE_GPU_PRIO = 1000;
    public static final int CMD_FREEZED_START = 300;
    public static final int CMD_GET_KGSL_GLOBALS = 150;
    public static final int COMPACT_MEMORY = 200;
    public static final int DISPLAY_FLAG_AUTO_60 = 4;
    public static final int DISPLAY_FLAG_AUTO_90 = 8;
    public static final int DISPLAY_FLAG_FORCE_60 = 1;
    public static final int DISPLAY_FLAG_FORCE_90 = 2;
    public static final int DO_ANTUTU_SPECIAL_CHEAT = 52;
    public static final int DO_THERMAL_CHEAT = 50;
    public static final int DUMP_PERF_INFO = 409;
    public static final int EXECUTE_IORAP_TASK = 411;
    public static final int FPS_LEVEL_1 = 0;
    public static final int FPS_LEVEL_2 = 1;
    public static final int GET_INSTRUCTION = 111;
    public static final int GET_SUB_SYSTEM_SLEEP = 114;
    public static final int HIGH_PERFORANCE_MODE = 2;
    public static final int IORAP_SEARCH_ROOTFS = 413;
    public static final int IORAP_TRACE_TASK = 412;
    public static final int LAUNCH_TYPE_COLD = 0;
    public static final int LAUNCH_TYPE_HOT = 1;
    public static final int LOW_PERFORMANCE_MODE = 0;
    public static final int NORMAL_PERFORANCE_MODE = 1;
    public static final int NOTIFY_COREDUMP_GEN = 501;
    public static final int PERF_CHMOD_SYSLOG_DIR = 208;
    public static final int PXRPS_RESET_CPU_GPU_LEVEL = 525;
    public static final int PXRPS_SET_CPU_LEVEL = 524;
    public static final int REMOVE_FILES = 20;
    public static final int RESET_COREDUMP_PROP = 500;
    public static final int SET_VRSHELL_AGAINST_BROWSER_CPUS = 701;
    public static final int SET_VRSHELL_CPUS = 702;
    public static final int START_POWERDETECT = 10;
    public static final int START_STATIS_INSTRUCTION = 110;
    public static final int STOP_POWERDETECT = 15;
    public static final int STOP_STATIS_INSTRUCTION = 112;
    public static final long SWITCH_TYPE_FREEZE = 1;
    public static final int THERMAL_CONTROL = 205;
    public static final int UPDATE_DEVICE_IDLE = 113;
    public static final int UPDATE_PID_RECLAIM = 116;
    public static final boolean mIsUserDebug = "userdebug".equals(SystemProperties.get("ro.build.type"));

    default ActivityManagerService getActivityManagerService() {
        DebugSmtEx.printDefaultFunInfo(getClass());
        return null;
    }

    default String addBinderPeer(int tid, ArrayList<Integer> firstPids, ArrayList<Integer> nativePids) {
        DebugSmtEx.printDefaultFunInfo(getClass());
        return "";
    }

    default void schedulePerformanceJobService(Context context) {
        DebugSmtEx.printDefaultFunInfo(getClass());
    }

    default void systemReady(Context context) {
        DebugSmtEx.printDefaultFunInfo(getClass());
    }

    default void preDex2oat() {
        DebugSmtEx.printDefaultFunInfo(getClass());
    }

    default void shutdown() {
        DebugSmtEx.printDefaultFunInfo(getClass());
    }

    default void saveLastPssData(IApplicationThread applicationThread, String processName, long pss, int curAdj, int curPid, String packageName) {
        DebugSmtEx.printDefaultFunInfo(getClass());
    }

    default void updateSwitchStatus(long switchType, boolean open) {
        DebugSmtEx.printDefaultFunInfo(getClass());
    }

    default boolean isFromSystemUi(int callingUid) {
        DebugSmtEx.printDefaultFunInfo(getClass());
        return false;
    }

    default boolean transact(int mode, int... args) {
        DebugSmtEx.printDefaultFunInfo(getClass());
        return false;
    }

    default boolean transact(int transCode, Parcel data, Parcel reply, int flags) {
        DebugSmtEx.printDefaultFunInfo(getClass());
        return false;
    }

    default void dumpPerfInfo() {
        DebugSmtEx.printDefaultFunInfo(getClass());
    }

    default void screenTurnOff() {
        DebugSmtEx.printDefaultFunInfo(getClass());
    }

    default void screenTurnOn() {
        DebugSmtEx.printDefaultFunInfo(getClass());
    }

    default void clearUidCpuUsage() {
        DebugSmtEx.printDefaultFunInfo(getClass());
    }

    default void monitorUidCpuUsage() {
        DebugSmtEx.printDefaultFunInfo(getClass());
    }

    default void printUidCpuUsages() {
        DebugSmtEx.printDefaultFunInfo(getClass());
    }

    default void deleteMonitorDailyFiles(String subDir, boolean uploaded) {
        DebugSmtEx.printDefaultFunInfo(getClass());
    }

    default void controlThermal(int type) {
        DebugSmtEx.printDefaultFunInfo(getClass());
    }

    default void enterPCMode() {
        DebugSmtEx.printDefaultFunInfo(getClass());
    }

    default void exitPCMode() {
        DebugSmtEx.printDefaultFunInfo(getClass());
    }

    default void updateWindowVisibleTime(int uid, String windowName, long visibleTime, int displayFpsMode) {
        DebugSmtEx.printDefaultFunInfo(getClass());
    }

    default void updateFocusWindow(int currentFocusUid, String packageName, String activityName, int width, int height) {
        DebugSmtEx.printDefaultFunInfo(getClass());
    }

    default void updateFocusSurfaceViewArea(int pid, int width, int height, boolean currentVisible) {
        DebugSmtEx.printDefaultFunInfo(getClass());
    }

    default void onSurfaceViewVisibilityChanged(int pid, int visibility) {
        DebugSmtEx.printDefaultFunInfo(getClass());
    }

    default int getFocusAppRequestDisplayMode() {
        DebugSmtEx.printDefaultFunInfo(getClass());
        return 0;
    }

    default void parseAppRefreshRate(String jsonStr) {
        DebugSmtEx.printDefaultFunInfo(getClass());
    }

    default boolean closeAllSmartRefresh() {
        DebugSmtEx.printDefaultFunInfo(getClass());
        return false;
    }

    default boolean closeDetectSmartRefresh() {
        DebugSmtEx.printDefaultFunInfo(getClass());
        return false;
    }

    default boolean isInputEffectiveDuration() {
        DebugSmtEx.printDefaultFunInfo(getClass());
        return false;
    }

    default void setLightsState(int id, int color) {
        DebugSmtEx.printDefaultFunInfo(getClass());
    }

    default void setFlashlightState(boolean isOn) {
        DebugSmtEx.printDefaultFunInfo(getClass());
    }

    default void changeSurfaceFlingerCpuset(boolean forceBig) {
        DebugSmtEx.printDefaultFunInfo(getClass());
    }

    default long getKgslGlobals() {
        DebugSmtEx.printDefaultFunInfo(getClass());
        return 0L;
    }

    default void addTrimMemForStart(int uid, int minADJ, int killCount, int freedMem, int needMem) {
        DebugSmtEx.printDefaultFunInfo(getClass());
    }

    default void finishBooting() {
        DebugSmtEx.printDefaultFunInfo(getClass());
    }

    default int getCurrentDisplayMode() {
        DebugSmtEx.printDefaultFunInfo(getClass());
        return 0;
    }

    default void monitorJniInfoSwitch(int type, int jniThreshold) {
        DebugSmtEx.printDefaultFunInfo(getClass());
    }

    default void unFreezeAppsByStartProcess() {
        DebugSmtEx.printDefaultFunInfo(getClass());
    }

    default void freezeAppsByStartProcess(int uid) {
        DebugSmtEx.printDefaultFunInfo(getClass());
    }

    default void chmodSyslogDir() {
        DebugSmtEx.printDefaultFunInfo(getClass());
    }

    default String createNewMonitorFile(String subDir, String filePrefix) {
        DebugSmtEx.printDefaultFunInfo(getClass());
        return "";
    }

    default void updateScreenOnCpuUsageByUid() {
        DebugSmtEx.printDefaultFunInfo(getClass());
    }

    default void updateScreenOffCpuUsageByUid() {
        DebugSmtEx.printDefaultFunInfo(getClass());
    }

    default void killBackgroundProcessesOvertime(long lastTime) {
        DebugSmtEx.printDefaultFunInfo(getClass());
    }

    default void freezeAppsByStartProcess() {
        DebugSmtEx.printDefaultFunInfo(getClass());
    }
}
