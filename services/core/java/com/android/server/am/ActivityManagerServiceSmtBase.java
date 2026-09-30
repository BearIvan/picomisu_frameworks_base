// Copyright 2026 Picomisu contributors
// SPDX-License-Identifier: Apache-2.0

package com.android.server.am;

import android.app.ApplicationErrorReport;
import android.app.IActivityLifeCycleObserver;
import android.app.IActivityManagerSmtEx;
import android.app.IAppStartEventObserver;
import android.app.IMemClient;
import android.app.ISysClient;
import android.content.pm.ApplicationInfo;
import android.os.Binder;
import android.os.Environment;
import android.os.PowerManagerInternal;
import android.os.StatFs;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.util.Slog;

import com.android.server.ITransferController;
import com.android.server.ServiceThread;
import com.android.server.SysOptBridge;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Smartisan extension of the {@link ActivityManagerService} (its {@code mSmtEx}), whose
 * {@link IActivityManagerSmtExBase} binder is returned by {@code IActivityManager.getISmtEx()}.
 * Reconstructed from the PICO OS 5.13.7 factory services; only the members reached by the
 * {@link IActivityManagerSmtEx} methods are present.
 *
 * @hide
 */
public class ActivityManagerServiceSmtBase {
    protected static final String TAG = "ActivityManagerService";
    static final String FROZEN_OBJECT_TAG = "FrozenObject";
    static final String CUSTOM_ERROR_TYPE_FROZEN_OBJECT = "process_frozen";

    protected ActivityManagerService mActivityManagerService;
    public int mStrictModeFlags = 0;
    ITransferController mTransferService;
    private IActivityManagerSmtEx mIActivityManagerSmtEx = this.new IActivityManagerSmtExBase();
    /** Prefetched (pre-started) application processes: pid to package name. */
    public HashMap<Integer, String> mPrefetchApps = new HashMap<>();

    protected ActivityManagerServiceSmtBase(ActivityManagerService ams) {
        mActivityManagerService = ams;
        mTransferService = SysMonitorSvcBridge.getFactory().getTransferController();
        mStrictModeFlags = SystemProperties.getInt("persist.sys.strictmode.flags", 0);
    }

    protected ActivityManagerServiceSmtBase(ActivityManagerService ams,
            ActivityManagerService.Injector injector, ServiceThread handlerThread) {
        mActivityManagerService = ams;
    }

    public void cropHprofDone(String path, boolean delete) {
        SysOptBridge.getFactory().getMemMonitor().cropHprofDone(path, delete);
    }

    public void checkHprof() {
        SysOptBridge.getFactory().getMemMonitor().checkHprof();
    }

    public void createHprof(int pid, String processName, IMemClient client, long dalvikAlloc,
            long dalvikMax) {
        SysOptBridge.getFactory().getMemMonitor().createHprof(pid, processName, client,
                dalvikAlloc, dalvikMax);
    }

    public ApplicationInfo getTopApplication() {
        return mActivityManagerService.mActivityTaskManager.getSmtEx().getTopApplication();
    }

    public void setProcessRunningCpuset(int pid, int cpusetLevel, long timeOut, boolean force) {
        SysOptBridge.getFactory().getSmtResourceControl().setProcessRunningCpuset(pid,
                cpusetLevel, timeOut, force);
    }

    public IActivityManagerSmtEx getISmtEx() {
        return mIActivityManagerSmtEx;
    }

    /** Binder of the Smartisan activity manager extension. */
    public class IActivityManagerSmtExBase extends IActivityManagerSmtEx.Stub {
        protected IActivityManagerSmtExBase() {
        }

        @Override
        public void setAppSlowMainOperations(List<String> slowOperations, int index) {
            ActivityManagerServiceSmtBase.this.setAppSlowMainOperations(slowOperations, index);
        }

        @Override
        public long getRomFreeMemoryKb() {
            return ActivityManagerServiceSmtBase.this.getRomFreeMemoryKb();
        }

        @Override
        public String getSmtExtraInfo(int pid) {
            return ActivityManagerServiceSmtBase.this.getSmtExtraInfo(pid);
        }

        @Override
        public void setSmtExtraInfo(int pid, String info) {
            ActivityManagerServiceSmtBase.this.setSmtExtraInfo(pid, info);
        }

        @Override
        public void forceStopPackageSmart(String packageName, int userId, int taskId,
                int cleanLevel) {
            ActivityManagerServiceSmtBase.this.forceStopPackageSmart(packageName, userId, taskId,
                    cleanLevel);
        }

        @Override
        public String getLastword(int pid) {
            return ActivityManagerServiceSmtBase.this.getLastword(pid);
        }

        @Override
        public List getPrefetchApps() {
            return ActivityManagerServiceSmtBase.this.getPrefetchApps();
        }

        @Override
        public int[] getPrefetchPids() {
            return ActivityManagerServiceSmtBase.this.getPrefetchPids();
        }

        @Override
        public void freezePrefetchApp() {
            ActivityManagerServiceSmtBase.this.freezePrefetchApp();
        }

        @Override
        public void registerSysClient(ISysClient client) {
            ActivityManagerServiceSmtBase.this.registerSysClient(client);
        }

        @Override
        public void setProcessRunningCpuset(int pid, int cpusetLevel, long timeOut,
                boolean force) {
            ActivityManagerServiceSmtBase.this.setProcessRunningCpuset(pid, cpusetLevel, timeOut,
                    force);
        }

        @Override
        public boolean getUidFrozen(int uid) {
            return ActivityManagerServiceSmtBase.this.getUidFrozen(uid);
        }

        @Override
        public ApplicationInfo getTopApplication() {
            return ActivityManagerServiceSmtBase.this.getTopApplication();
        }

        @Override
        public void createHprof(int pid, String processName, IMemClient client,
                long dalvikAlloc, long dalvikMax) {
            ActivityManagerServiceSmtBase.this.createHprof(pid, processName, client, dalvikAlloc,
                    dalvikMax);
        }

        @Override
        public void cropHprofDone(String path, boolean delete) {
            ActivityManagerServiceSmtBase.this.cropHprofDone(path, delete);
        }

        @Override
        public void checkHprof() {
            ActivityManagerServiceSmtBase.this.checkHprof();
        }

        @Override
        public int getStrictModeFlags() {
            return ActivityManagerServiceSmtBase.this.mStrictModeFlags;
        }

        @Override
        public void registerActivityLifeCycleObserver(IActivityLifeCycleObserver observer) {
            mActivityManagerService.enforceCallingPermission(
                    "com.smartisanos.permission.observe.activity.lifecycle",
                    "registerActivityLifeCycleObserver");
            SysOptBridge.getFactory().getActivityManager(mActivityManagerService)
                    .registerActivityLifeCycleObserver(observer);
        }

        @Override
        public void unregisterActivityLifeCycleObserver(IActivityLifeCycleObserver observer) {
            mActivityManagerService.enforceCallingPermission(
                    "com.smartisanos.permission.observe.activity.lifecycle",
                    "unregisterActivityLifeCycleObserver");
            SysOptBridge.getFactory().getActivityManager(mActivityManagerService)
                    .unregisterActivityLifeCycleObserver(observer);
        }

        @Override
        public void registerAppStartEventObserver(IAppStartEventObserver observer) {
            mActivityManagerService.enforceCallingPermission(
                    "com.smartisanos.permission.observe.app.startevent",
                    "registerAppStartEventObserver");
            SysOptBridge.getFactory().getActivityManager(mActivityManagerService)
                    .registerAppStartEventObserver(observer);
        }

        @Override
        public void unregisterAppStartEventObserver(IAppStartEventObserver observer) {
            mActivityManagerService.enforceCallingPermission(
                    "com.smartisanos.permission.observe.app.startevent",
                    "unregisterAppStartEventObserver");
            SysOptBridge.getFactory().getActivityManager(mActivityManagerService)
                    .unregisterAppStartEventObserver(observer);
        }

        @Override
        public void frozenObjectFromNative(
                ApplicationErrorReport.ParcelableCrashInfo crashInfo) {
            final int callingPid = Binder.getCallingPid();
            final int callingUid = Binder.getCallingUid();
            final long origId = Binder.clearCallingIdentity();
            ProcessRecord r = null;
            String processName = null;
            if (callingPid == ActivityManagerService.MY_PID) {
                processName = "system_server";
            } else {
                synchronized (mActivityManagerService.mPidsSelfLocked) {
                    r = mActivityManagerService.mPidsSelfLocked.get(callingPid);
                }
                processName = r == null ? "unknown" : r.processName;
            }
            Slog.i(FROZEN_OBJECT_TAG, "target process is frozen, client name:" + processName
                    + " pid:" + callingPid + " uid:" + callingUid);
            mActivityManagerService.addErrorToDropBox("customerror", r, processName, null, null,
                    null, FROZEN_OBJECT_TAG, null, null, crashInfo,
                    CUSTOM_ERROR_TYPE_FROZEN_OBJECT);
            Binder.restoreCallingIdentity(origId);
        }

        @Override
        public void updatePrefetchApps(List<String> needPrefetchApps, int flag) {
            SysOptBridge.getFactory().getSysPrefetchService().updatePrefetchApps(
                    needPrefetchApps, flag);
            if (SysOptBridge.getFactory().getPrefetchManager().getPrefetchEnable()) {
                SysOptBridge.getFactory().getPrefetchManager().updatePrefetchApp(
                        needPrefetchApps, flag);
            }
            SysOptBridge.getFactory().getMemoryProcessController().updatePrefetchApp(
                    needPrefetchApps);
        }

        @Override
        public void killMemoryLeakProcess(String processName, int pid) {
            SysOptBridge.getFactory().getHandleMemoryLeak().killMemoryLeakProcess(processName,
                    pid);
        }

        @Override
        public void backtraceDoneInform(String processName, int pid) {
            SysMonitorSvcBridge.getFactory().getMemoryStrategy().backtraceDoneInform(
                    processName, pid);
        }

        @Override
        public void clearAllKeepAliveProc() {
            SysOptBridge.getFactory().getMemoryProcessController().clearKeepAliveProcesses();
        }

        @Override
        public boolean isScreenOn() {
            return ActivityManagerServiceSmtBase.this.isScreenOn();
        }
    }

    public boolean isScreenOn() {
        return mActivityManagerService.mWakefulness == PowerManagerInternal.WAKEFULNESS_AWAKE;
    }

    public boolean getUidFrozen(int uid) {
        synchronized (mActivityManagerService) {
            UidRecord uidRecord = mActivityManagerService.mProcessList.getUidRecordLocked(uid);
            if (uidRecord != null) {
                return uidRecord.getSmtEx().curFrozenStat != 0;
            }
            return false;
        }
    }

    public void setAppSlowMainOperations(List<String> slowOperations, int index) {
        mTransferService.setAppSlowMainOperation(slowOperations, index);
    }

    public void setSmtExtraInfo(int pid, String info) {
        synchronized (mActivityManagerService.mPidsSelfLocked) {
            ProcessRecord proc = mActivityManagerService.mPidsSelfLocked.get(pid);
            if (proc != null) {
                proc.getSmtEx().setSmtExtraInfo(info);
            }
        }
    }

    public List getPrefetchApps() {
        synchronized (mPrefetchApps) {
            List<String> prefetchApps = new ArrayList<>();
            prefetchApps.addAll(mPrefetchApps.values());
            return prefetchApps;
        }
    }

    public int[] getPrefetchPids() {
        synchronized (mPrefetchApps) {
            List<Integer> pids = new ArrayList<>();
            pids.addAll(mPrefetchApps.keySet());
            return pids.stream().mapToInt(Integer::intValue).toArray();
        }
    }

    public void freezePrefetchApp() {
        synchronized (mActivityManagerService) {
            int callingPid = Binder.getCallingPid();
            long origId = Binder.clearCallingIdentity();
            ProcessRecord app = null;
            if (callingPid != ActivityManagerService.MY_PID && callingPid >= 0) {
                synchronized (mActivityManagerService.mPidsSelfLocked) {
                    app = mActivityManagerService.mPidsSelfLocked.get(callingPid);
                }
            }
            if (app != null) {
                app.getSmtEx().delayFreezing = true;
                SysOptBridge.getFactory().getSysPrefetchService().sendFreezeCurrentPrefetchMsg(
                        callingPid);
            }
            Binder.restoreCallingIdentity(origId);
        }
    }

    public void forceStopPackageSmart(String packageName, int userId, int taskId,
            int cleanLevel) {
        long callingId = Binder.clearCallingIdentity();
        boolean needForceStop = false;
        try {
            mActivityManagerService.removeTask(taskId);
            if (needForceStop) {
                Slog.d(TAG, "Smart forceStopPackage: packageName=" + packageName + ", userId="
                        + userId + " taskId=" + taskId + " cleanLevel=" + cleanLevel);
                SysOptBridge.getFactory().getTaskDeepClean().addTdcEvent(1, cleanLevel,
                        packageName, 0);
                mActivityManagerService.forceStopPackage(packageName, userId);
            }
        } finally {
            Binder.restoreCallingIdentity(callingId);
        }
    }

    public final void registerSysClient(ISysClient client) {
        int callingPid = Binder.getCallingPid();
        SysMonitorSvcBridge.getFactory().getAnrMonitor().addClient(callingPid, client);
    }

    public String getSmtExtraInfo(int pid) {
        synchronized (mActivityManagerService.mPidsSelfLocked) {
            ProcessRecord proc = mActivityManagerService.mPidsSelfLocked.get(pid);
            if (proc != null) {
                long bootTime = System.currentTimeMillis() - SystemClock.elapsedRealtime();
                return proc.getSmtEx().getSmtExtraInfo() + "\nstart_time : " + proc.startTime
                        + bootTime;
            }
        }
        return null;
    }

    public String getLastword(int pid) {
        return ProcExtraInfoSmtBase.getInstance().getLastWordOfPid(pid);
    }

    public static long getRomFreeMemoryKb() {
        File path = Environment.getDataDirectory();
        StatFs stat = new StatFs(path.getPath());
        long availableBytes = stat.getAvailableBytes();
        return availableBytes / 1024;
    }
}
