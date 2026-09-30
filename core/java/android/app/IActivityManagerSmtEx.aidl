// Copyright 2026 Picomisu contributors
// SPDX-License-Identifier: Apache-2.0
// Reconstructed from the factory PICO OS 5.13.7 DEX by tools/reconstruct-pico-aidl.py.
package android.app;

import android.app.ApplicationErrorReport;
import android.app.IActivityLifeCycleObserver;
import android.app.IAppStartEventObserver;
import android.app.IMemClient;
import android.app.ISysClient;
import android.content.pm.ApplicationInfo;

/** @hide */
interface IActivityManagerSmtEx {
    oneway void setAppSlowMainOperations(in List<String> slowOperations, int index);
    long getRomFreeMemoryKb();
    String getSmtExtraInfo(int pid);
    String getLastword(int pid);
    oneway void setSmtExtraInfo(int pid, String info);
    List<String> getPrefetchApps();
    int[] getPrefetchPids();
    void freezePrefetchApp();
    void registerSysClient(ISysClient client);
    oneway void setProcessRunningCpuset(int pid, int cpusetLevel, long timeOut, boolean force);
    boolean getUidFrozen(int uid);
    ApplicationInfo getTopApplication();
    oneway void createHprof(int pid, String processName, IMemClient client, long dalvikAlloc, long dalvikMax);
    oneway void cropHprofDone(String path, boolean delete);
    oneway void checkHprof();
    int getStrictModeFlags();
    void registerActivityLifeCycleObserver(IActivityLifeCycleObserver observer);
    void unregisterActivityLifeCycleObserver(IActivityLifeCycleObserver observer);
    void registerAppStartEventObserver(IAppStartEventObserver observer);
    void unregisterAppStartEventObserver(IAppStartEventObserver observer);
    void updatePrefetchApps(in List<String> prefetchApps, int flag);
    void killMemoryLeakProcess(String processName, int pid);
    void backtraceDoneInform(String processName, int pid);
    void clearAllKeepAliveProc();
    boolean isScreenOn();
    void forceStopPackageSmart(String packageName, int userId, int taskId, int cleanLevel);
    void frozenObjectFromNative(in ApplicationErrorReport.ParcelableCrashInfo crashInfo);
}
