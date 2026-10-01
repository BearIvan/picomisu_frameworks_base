// Copyright 2026 Picomisu contributors
// SPDX-License-Identifier: Apache-2.0

package com.android.server;

import android.content.Context;
import android.os.Parcel;

/**
 * Reconstructed from the PICO OS 5.13.7 factory services.
 *
 * @hide
 */
public interface ISysPerfMonitorService {
    public static final int DEBUG_OPT_ALL = 29;
    public static final int DEBUG_OPT_DEFAULT = 17;
    public static final int DEBUG_OPT_LAUNCH_STACK = 4;
    public static final int DEBUG_OPT_LOG_FPS = 8;
    public static final int DEBUG_OPT_PHONE_SLOW_ATRACE = 16;
    public static final int DEBUG_OPT_SCREENSHOT = 2;
    public static final int DEBUG_OPT_TRACE = 1;
    public static final String DEBUG_PROPERTY_NAME = "persist.debug.sysmonitor.opt";
    public static final int DUMP_ATRACE_ALL = 4;
    public static final int DUMP_ATRACE_ANIMATION = 1;
    public static final int DUMP_ATRACE_GAME = 8;
    public static final int DUMP_ATRACE_JANK = 2;
    public static final int DUMP_ATRACE_LAUNCH = 16;
    public static final int DUMP_ATRACE_NONE = 0;
    public static final int DUMP_ATRACE_NORESPONSE = 32;
    public static final int DUMP_PERFETTO = 107;
    public static final int DUMP_SCHEDINFO = 130;
    public static final int DUMP_SCHED_PERFETTO = 134;
    public static final int DUMP_SYSTRACE = 103;
    public static final int GET_KTOP = 410;
    public static final int GET_MEMFRAG_SYSEVENT = 209;
    public static final int GET_TRACING_ON = 104;
    public static final int LAUNCH_TYPE_COLD_ACTIVITY = 20;
    public static final int LAUNCH_TYPE_COLD_ACTIVITY_FREEZE = 30;
    public static final int LAUNCH_TYPE_COLD_ACTIVITY_PREFETCH_FREEZE = 31;
    public static final int LAUNCH_TYPE_COLD_PROCESS = 10;
    public static final int LAUNCH_TYPE_HOT = 40;
    public static final int LAUNCH_TYPE_HOT_FREEZE = 50;
    public static final int LAUNCH_TYPE_NONE = 0;
    public static final int LAUNCH_TYPE_VR_PREFETCH_HOT = 41;
    public static final int MONITOR_DEFAULT = 93;
    public static final int MONITOR_FLAG_ALL = 95;
    public static final int MONITOR_FLAG_CRASH_STATS = 2;
    public static final int MONITOR_FLAG_FLUENCY = 1;
    public static final int MONITOR_FLAG_FLUENCY_ANALYSIS = 64;
    public static final int MONITOR_FLAG_FUNC_TRACKER = 128;
    public static final int MONITOR_FLAG_GAME_BALANCE = 16;
    public static final int MONITOR_FLAG_INPUT = 4;
    public static final int MONITOR_FLAG_LAUNCH = 8;
    public static final int MONITOR_OLD_DEFAULT = 89;
    public static final String PHONE_OTA_TIME = "phone_ota_time";
    public static final String PROPERTY_NAME = "persist.sys.monitor";
    public static final int SET_LOOP_DUMP_PERFETTO = 108;
    public static final int SET_TID_IO_THRESHOLD = 109;
    public static final int SET_TID_IO_TID_DATA = 110;
    public static final int SET_TIME_INFO_TO_KERNEL = 136;
    public static final int START_PERFETTO = 105;
    public static final int START_SCHED_PERFETTO = 132;
    public static final int START_SYSTRACE = 101;
    public static final int STOP_PERFETTO = 106;
    public static final int STOP_SCHED_PERFETTO = 133;
    public static final int STOP_SYSTRACE = 102;
    public static final int WRITE_SHEDINFO = 131;

    default int getMonitorControlOpt() {
        return 0;
    }

    default void setMonitorControlOpt(int flag) {
    }

    default int getMonitorDebugOpt() {
        return 0;
    }

    default void systemReady(Context context) {
    }

    default void updatePidUidInfo(int pid, int uid) {
    }

    default void updateUidVersion(int uid, long versionCode) {
    }

    default void removePid(int pid) {
    }

    default void shutdown() {
    }

    default boolean transact(int mode, int... args) {
        return false;
    }

    default boolean transact(int transCode, Parcel data, Parcel reply, int flags) {
        return false;
    }

    default void updateAppFirstLaunchTime(String packageName) {
    }

    default void resetAppFirstLaunchTime(String packageName) {
    }

    default void updateActivityLaunchTime(int uid, String packageName, String activityName, long launchTime, int launchType, long launchStartTime, long launchDisplayedTime) {
    }

    default void perfettoDumpJudgement(String packageName, int uid) {
    }

    default void screenTurnOff() {
    }

    default void screenTurnOn() {
    }

    default void writeAdjCountToSysEvent(int countForeground, int countVisible, int countPerceptible, int countService, int countBService, int countCached) {
    }

    default void updatePhoneSignalStrength(int subId, int level) {
    }

    default void notifyActivityStart() {
    }

    default void notifyWindowDisplayed() {
    }

    default void clearPendingLaunchRecords() {
    }

    default boolean isScreenOn() {
        return false;
    }

    default boolean allowUserAtrace() {
        return false;
    }

    default void setUserDumpAtraceType(int dumpAtraceType) {
    }

    default void setUserDumpAtraceWindow(String packOrWindow) {
    }

    default void updateAdjProcessCount(int countForeground, int countVisible, int countPerceptible, int countService, int countBService, int countCached) {
    }

    default void killBackgroundAppsOvertime(long lasttime) {
    }

    default void setSysEventScenesStatus(int scenesType, int scenesStatus) {
    }

    default void updateFocusWindow(int uid) {
    }

    default void setWriteMonitorFileInterval(long intervalTime) {
    }

    default void uploadSystemMonitorData() {
    }

    default void updateTopApp(int pid) {
    }

    default void updateKTopInfo() {
    }

    default void updateKTopInfo(boolean isJank) {
    }

    default void onActiveUidAdded(int uid) {
    }

    default void onActiveUidRemoved(int uid) {
    }

    default void notifyForeground(int prevUid, int nextUid) {
    }

    default void updateAppInfo(String packageName) {
    }

    default void enableAtrace() {
    }
}
