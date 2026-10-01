// Copyright 2026 Picomisu contributors
// SPDX-License-Identifier: Apache-2.0

package com.android.server;

import android.app.IActivityLifeCycleObserver;
import android.app.IAppStartEventObserver;
import android.app.IApplicationThread;
import android.os.Debug;
import android.os.DebugSmtEx;
import android.os.Message;
import android.os.Parcel;
import com.android.server.am.IUidCpuRunner;
import com.android.server.am.IUidMonitorSmt;
import com.android.server.am.PackageUsageStatsBase;
import com.android.server.am.ProcessRecord;
import com.android.server.wm.ActivityRecord;
import com.android.server.wm.WindowProcessController;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashSet;
import smartisanos.os.RemoteCallback;

/**
 * Reconstructed from the PICO OS 5.13.7 factory services.
 *
 * @hide
 */
public interface IActivityManagerOptEx {
    public static final String ACTIVITY_LIFECYCLE_CALLBACK = "sys.activity.lifecycle.callback";
    public static final int LIFECYCLE_DESTROY = 5;
    public static final int LIFECYCLE_PAUSE = 3;
    public static final int LIFECYCLE_RESUME = 2;
    public static final int LIFECYCLE_START = 1;
    public static final int LIFECYCLE_STOP = 4;

    default void systemReady() {
        DebugSmtEx.printDefaultFunInfo(getClass());
    }

    default void notifyLimitedChanged() {
        DebugSmtEx.printDefaultFunInfo(getClass());
    }

    default RemoteCallback getUpdateApptypeCallback() {
        DebugSmtEx.printDefaultFunInfo(getClass());
        return null;
    }

    default void onActivityManagerReady() {
        DebugSmtEx.printDefaultFunInfo(getClass());
    }

    default IUidCpuRunner getmUidCpuRunner() {
        DebugSmtEx.printDefaultFunInfo(getClass());
        return null;
    }

    default long getmLastKillAppSetTime() {
        DebugSmtEx.printDefaultFunInfo(getClass());
        return 0L;
    }

    default void setmLastKillAppSetTime(long time) {
        DebugSmtEx.printDefaultFunInfo(getClass());
    }

    default boolean getmEnableMemAppSetKill() {
        DebugSmtEx.printDefaultFunInfo(getClass());
        return false;
    }

    default HashSet<Integer> getmVisebleAppsSet() {
        DebugSmtEx.printDefaultFunInfo(getClass());
        return null;
    }

    default HashSet<Integer> getmBServiceAppsSet() {
        DebugSmtEx.printDefaultFunInfo(getClass());
        return null;
    }

    default long getmMaxSizeVisibleMemory() {
        DebugSmtEx.printDefaultFunInfo(getClass());
        return 0L;
    }

    default long getmMinSizeCachedMemory() {
        DebugSmtEx.printDefaultFunInfo(getClass());
        return 0L;
    }

    default int getKILL_B_SERVICE_APP_SET() {
        DebugSmtEx.printDefaultFunInfo(getClass());
        return 0;
    }

    default int getKILL_VISIBLE_APP_SET() {
        DebugSmtEx.printDefaultFunInfo(getClass());
        return 0;
    }

    default int getmMaxBServiceCount() {
        DebugSmtEx.printDefaultFunInfo(getClass());
        return 0;
    }

    default void forceStopAppSet(int killNum, int type) {
        DebugSmtEx.printDefaultFunInfo(getClass());
    }

    default void initSmartisanOS() {
        DebugSmtEx.printDefaultFunInfo(getClass());
    }

    default int getmKillAppSetMinTime() {
        DebugSmtEx.printDefaultFunInfo(getClass());
        return 0;
    }

    default int getmMaxCachedProcesses() {
        DebugSmtEx.printDefaultFunInfo(getClass());
        return 0;
    }

    default void setmMaxCachedProcesses(int num) {
        DebugSmtEx.printDefaultFunInfo(getClass());
    }

    default void setAppDebugFlag(ProcessRecord app) {
        DebugSmtEx.printDefaultFunInfo(getClass());
    }

    default void initSettingObservers() {
        DebugSmtEx.printDefaultFunInfo(getClass());
    }

    default ArrayList<ProcessRecord> getmServiceBList() {
        DebugSmtEx.printDefaultFunInfo(getClass());
        return null;
    }

    default void setmServiceBList(ArrayList<ProcessRecord> list) {
        DebugSmtEx.printDefaultFunInfo(getClass());
    }

    default PackageUsageStatsBase getmPackStats() {
        DebugSmtEx.printDefaultFunInfo(getClass());
        return null;
    }

    default boolean getmEnablePeropt() {
        DebugSmtEx.printDefaultFunInfo(getClass());
        return false;
    }

    default void setmEnablePeropt(boolean enable) {
        DebugSmtEx.printDefaultFunInfo(getClass());
    }

    default boolean getmBlindModeEnabled() {
        DebugSmtEx.printDefaultFunInfo(getClass());
        return false;
    }

    default void setmBlindModeEnabled(boolean enable) {
        DebugSmtEx.printDefaultFunInfo(getClass());
    }

    default boolean getmIsLowMemState() {
        DebugSmtEx.printDefaultFunInfo(getClass());
        return false;
    }

    default void setmIsLowMemState(boolean state) {
        DebugSmtEx.printDefaultFunInfo(getClass());
    }

    default int getLOW_MEMORY_TIME() {
        DebugSmtEx.printDefaultFunInfo(getClass());
        return 0;
    }

    default boolean isBlindModeApp(ProcessRecord app) {
        DebugSmtEx.printDefaultFunInfo(getClass());
        return false;
    }

    default boolean isScreenOn() {
        DebugSmtEx.printDefaultFunInfo(getClass());
        return false;
    }

    default ArrayList<Integer> getfocusedLinkedUIDList() {
        DebugSmtEx.printDefaultFunInfo(getClass());
        return null;
    }

    default IUidMonitorSmt getmUidMonitorSmt() {
        DebugSmtEx.printDefaultFunInfo(getClass());
        return null;
    }

    default void handleMessageSmt(Message msg) {
        DebugSmtEx.printDefaultFunInfo(getClass());
    }

    default void handleMessageOpt(Message msg) {
        DebugSmtEx.printDefaultFunInfo(getClass());
    }

    default boolean isHighPriorityApp(ProcessRecord app) {
        DebugSmtEx.printDefaultFunInfo(getClass());
        return false;
    }

    default void setTrackStateCallback() {
        DebugSmtEx.printDefaultFunInfo(getClass());
    }

    default void checkForceCleanupUid(ProcessRecord app) {
        DebugSmtEx.printDefaultFunInfo(getClass());
    }

    default void killBackgroundProcessesOvertime(long lastTime) {
        DebugSmtEx.printDefaultFunInfo(getClass());
    }

    default void asyncSetProcessGroupAll(ProcessRecord pr, int pid, int processGroup) {
        DebugSmtEx.printDefaultFunInfo(getClass());
    }

    default void bringProcessToDefaultLocked(WindowProcessController windowProcessController) {
        DebugSmtEx.printDefaultFunInfo(getClass());
    }

    default void registerPeroptWhiteListReceiver() {
        DebugSmtEx.printDefaultFunInfo(getClass());
    }

    default long getMemInfoCache(ProcessRecord processRecord, long[] tmp, long[] egl) {
        DebugSmtEx.printDefaultFunInfo(getClass());
        return 0L;
    }

    default Debug.MemoryInfo obtainMemInfo(ProcessRecord processRecord, int pid, int callingUid, long now, long lastPssTime) {
        DebugSmtEx.printDefaultFunInfo(getClass());
        return null;
    }

    default char getProcessStateLocked(ProcessRecord app) {
        DebugSmtEx.printDefaultFunInfo(getClass());
        return (char) 0;
    }

    default long[] getSwapsUsed() {
        DebugSmtEx.printDefaultFunInfo(getClass());
        return null;
    }

    default ProcessRecord getFocusedAppOnExtDisplay() {
        DebugSmtEx.printDefaultFunInfo(getClass());
        return null;
    }

    default void onAttachApplicationLocked(ProcessRecord app) {
        DebugSmtEx.printDefaultFunInfo(getClass());
    }

    default boolean onTransact(int code, Parcel data, Parcel reply, int flags) {
        DebugSmtEx.printDefaultFunInfo(getClass());
        return false;
    }

    default boolean isFreezeEnable() {
        DebugSmtEx.printDefaultFunInfo(getClass());
        return false;
    }

    default void dumpProcessesLocked(PrintWriter pw, String dumpPackage) {
        DebugSmtEx.printDefaultFunInfo(getClass());
    }

    default void registerActivityLifeCycleObserver(IActivityLifeCycleObserver observer) {
        DebugSmtEx.printDefaultFunInfo(getClass());
    }

    default void unregisterActivityLifeCycleObserver(IActivityLifeCycleObserver observer) {
        DebugSmtEx.printDefaultFunInfo(getClass());
    }

    default void notifyActivityLifeCycleStateChanged(ActivityRecord activityRecord, int lifecycleType) {
        DebugSmtEx.printDefaultFunInfo(getClass());
    }

    default void handleMessageUiHandlerOpt(Message msg) {
        DebugSmtEx.printDefaultFunInfo(getClass());
    }

    default void registerAppStartEventObserver(IAppStartEventObserver observer) {
        DebugSmtEx.printDefaultFunInfo(getClass());
    }

    default void unregisterAppStartEventObserver(IAppStartEventObserver observer) {
        DebugSmtEx.printDefaultFunInfo(getClass());
    }

    default void appDiedLocked(ProcessRecord app, int pid, IApplicationThread thread, boolean fromBinderDied) {
        DebugSmtEx.printDefaultFunInfo(getClass());
    }
}
