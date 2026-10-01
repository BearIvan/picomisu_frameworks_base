// Copyright 2026 Picomisu contributors
// SPDX-License-Identifier: Apache-2.0

package com.android.server.am;

import android.app.IActivityManagerSysMoEx;
import android.app.ISysClient;
import android.os.Binder;
import android.os.Environment;
import android.os.Parcel;
import android.os.StatFs;
import android.os.SystemClock;
import android.util.Slog;
import com.android.server.IActivityManagerOptEx;
import com.android.server.ServiceThread;
import com.android.server.wm.ActivityRecord;
import java.io.File;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 * Reconstructed from the PICO OS 5.13.7 factory services.
 *
 * @hide
 */
public class ActivityManagerServiceSysMoEx extends IActivityManagerSysMoEx.Stub {
    private static final String TAG = "ActivityManagerService";
    public static volatile boolean mCleanupCancelled = false;
    protected static IActivityManagerOptEx mSmtOptEx = null;
    static final int sSystemMask = 129;
    private ActivityManagerService mActivityManagerService;
    final List<ActivityRecord> mPendingLaunchRecords = new LinkedList();

    public interface CpuStateObserver {

        public enum CPU_USAGE_STATE {
            CPU_NORMAL,
            CPU_BUSY
        }

        public enum NOTIFY_FREQUENCY {
            EVERY_TIME,
            ONLY_CHANGE
        }

        NOTIFY_FREQUENCY getNotifyRequest();

        void onCpuState(CPU_USAGE_STATE cpu_usage_state, long j);
    }

    protected ActivityManagerServiceSysMoEx(ActivityManagerService activityManagerService) {
        this.mActivityManagerService = activityManagerService;
        mSmtOptEx = SysMonitorSvcBridge.getFactory().getActivityManager(activityManagerService);
    }

    protected ActivityManagerServiceSysMoEx(ActivityManagerService activityManagerService, ActivityManagerService.Injector injector, ServiceThread handlerThread) {
        this.mActivityManagerService = activityManagerService;
        mSmtOptEx = SysMonitorSvcBridge.getFactory().getActivityManager(activityManagerService);
    }

    public void registerSysClient(ISysClient client) {
        int callingPid = Binder.getCallingPid();
        SysMonitorSvcBridge.getFactory().getAnrMonitor().addClient(callingPid, client);
    }

    public boolean onTransactMonitorEx(int code, Parcel data, Parcel reply, int flags) {
        switch (code) {
            case 7709:
                data.enforceInterface("android.app.IActivityManager");
                int loopDump = data.readInt();
                SysMonitorSvcBridge.getFactory().getSysPerfMonitorService().transact(105, new int[0]);
                SysMonitorSvcBridge.getFactory().getSysPerfMonitorService().transact(108, loopDump);
                return true;
            case 7710:
                data.enforceInterface("android.app.IActivityManager");
                SysMonitorSvcBridge.getFactory().getSysPerfMonitorService().transact(106, new int[0]);
                return true;
            case 7711:
                data.enforceInterface("android.app.IActivityManager");
                SysMonitorSvcBridge.getFactory().getSysPerfMonitorService().transact(107, new int[0]);
                return true;
            default:
                return false;
        }
    }

    public long getRomFreeMemoryKb() {
        File path = Environment.getDataDirectory();
        StatFs stat = new StatFs(path.getPath());
        long availableBytes = stat.getAvailableBytes();
        return availableBytes / 1024;
    }

    public String getSmtExtraInfo(int pid) {
        synchronized (this.mActivityManagerService.mPidsSelfLocked) {
            ProcessRecord proc = this.mActivityManagerService.mPidsSelfLocked.get(pid);
            if (proc != null) {
                long bootTime = System.currentTimeMillis() - SystemClock.elapsedRealtime();
                return "start_time : " + proc.startTime + bootTime;
            }
            return null;
        }
    }

    public boolean isForegroundProcess(String processName) {
        for (ProcessRecord r : this.mActivityManagerService.mProcessList.mLruProcesses) {
            if (r.processName.equals(processName)) {
                return r.isInterestingToUserLocked();
            }
        }
        return false;
    }

    public String getPackageName(int pid) {
        synchronized (this.mActivityManagerService.mPidsSelfLocked) {
            ProcessRecord proc = this.mActivityManagerService.mPidsSelfLocked.get(pid);
            if (proc != null) {
                return proc.info.packageName;
            }
            return "";
        }
    }

    public interface CpuStateProvider {
        default void registerCpuStateObserver(CpuStateObserver observer) {
        }

        default void unregisterCpuStateObserver(CpuStateObserver observer) {
        }
    }

    public static void cleanupProcessesScreenOffForCpu(ActivityManagerService ams) {
        int killUidSize;
        int killedUidSize;
        try {
            if (mCleanupCancelled) {
                return;
            }
            Slog.d(TAG, "check processes stage 1");
            ArrayList<Integer> killUidlist = new ArrayList<>();
            synchronized (ams) {
                try {
                    ActivityManagerService.boostPriorityForLockedSection();
                    for (ProcessRecord r : ams.mProcessList.mLruProcesses) {
                        if ((r.info.getSmtEx().peroptFlag & 256) != 0 && !killUidlist.contains(Integer.valueOf(r.uid))) {
                            killUidlist.add(Integer.valueOf(r.uid));
                        }
                    }
                    if (mCleanupCancelled) {
                        ActivityManagerService.resetPriorityAfterLockedSection();
                        return;
                    }
                    if (mSmtOptEx.getmUidCpuRunner() != null && (killUidSize = killUidlist.size()) > 0 && (killedUidSize = mSmtOptEx.getmUidCpuRunner().removeUids(killUidlist, false, false, killUidSize)) != killUidSize) {
                        Slog.w(TAG, "cleanup UidSize= " + killUidSize + " killedUidSize= " + killedUidSize);
                    }
                    ActivityManagerService.resetPriorityAfterLockedSection();
                } catch (Throwable th) {
                    ActivityManagerService.resetPriorityAfterLockedSection();
                    throw th;
                }
            }
        } catch (Exception e) {
            Slog.e(TAG, e.getMessage());
        }
    }

    public void clearPendingLaunchRecords() {
        synchronized (this.mPendingLaunchRecords) {
            Slog.i(TAG, "clear all has not complete start activity when new Activity start complete");
            for (ActivityRecord r : this.mPendingLaunchRecords) {
                r.getActivityRecordMonitorEx().launchTimeStatistics.clearLaunchStepIfPausing("clearPendingLaunchRecords");
            }
            this.mPendingLaunchRecords.clear();
        }
    }
}
