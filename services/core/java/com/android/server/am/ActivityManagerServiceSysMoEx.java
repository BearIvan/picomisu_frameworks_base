// Copyright 2026 Picomisu contributors
// SPDX-License-Identifier: Apache-2.0

package com.android.server.am;

import android.app.IActivityManagerSysMoEx;
import android.app.ISysClient;
import android.os.Binder;
import android.os.Environment;
import android.os.StatFs;
import android.os.SystemClock;

import com.android.server.ServiceThread;

import java.io.File;

/**
 * Smartisan system monitor extension of the {@link ActivityManagerService}, returned by
 * {@code IActivityManager.getMonitorEx()}. Reconstructed from the PICO OS 5.13.7 factory
 * services; only the {@link IActivityManagerSysMoEx} methods and the members they reach are
 * present.
 *
 * @hide
 */
public class ActivityManagerServiceSysMoEx extends IActivityManagerSysMoEx.Stub {
    private ActivityManagerService mActivityManagerService;

    protected ActivityManagerServiceSysMoEx(ActivityManagerService activityManagerService) {
        mActivityManagerService = activityManagerService;
    }

    protected ActivityManagerServiceSysMoEx(ActivityManagerService activityManagerService,
            ActivityManagerService.Injector injector, ServiceThread handlerThread) {
        mActivityManagerService = activityManagerService;
    }

    @Override
    public void registerSysClient(ISysClient client) {
        int callingPid = Binder.getCallingPid();
        SysMonitorSvcBridge.getFactory().getAnrMonitor().addClient(callingPid, client);
    }

    @Override
    public long getRomFreeMemoryKb() {
        File path = Environment.getDataDirectory();
        StatFs stat = new StatFs(path.getPath());
        long availableBytes = stat.getAvailableBytes();
        return availableBytes / 1024;
    }

    @Override
    public String getSmtExtraInfo(int pid) {
        synchronized (mActivityManagerService.mPidsSelfLocked) {
            ProcessRecord proc = mActivityManagerService.mPidsSelfLocked.get(pid);
            if (proc != null) {
                long bootTime = System.currentTimeMillis() - SystemClock.elapsedRealtime();
                return "start_time : " + proc.startTime + bootTime;
            }
        }
        return null;
    }

    @Override
    public boolean isForegroundProcess(String processName) {
        for (ProcessRecord r : mActivityManagerService.mProcessList.mLruProcesses) {
            if (r.processName.equals(processName)) {
                return r.isInterestingToUserLocked();
            }
        }
        return false;
    }

    @Override
    public String getPackageName(int pid) {
        synchronized (mActivityManagerService.mPidsSelfLocked) {
            ProcessRecord proc = mActivityManagerService.mPidsSelfLocked.get(pid);
            if (proc != null) {
                return proc.info.packageName;
            }
        }
        return "";
    }

    /** Observer of the system CPU load state (e.g. the idle dex2oat of the package manager). */
    public interface CpuStateObserver {
        enum CPU_USAGE_STATE {
            CPU_NORMAL,
            CPU_BUSY
        }

        enum NOTIFY_FREQUENCY {
            EVERY_TIME,
            ONLY_CHANGE
        }

        void onCpuState(CPU_USAGE_STATE state, long timestamp);

        NOTIFY_FREQUENCY getNotifyRequest();
    }

    /** Source of {@link CpuStateObserver} notifications. */
    public interface CpuStateProvider {
        default void registerCpuStateObserver(CpuStateObserver observer) {}
        default void unregisterCpuStateObserver(CpuStateObserver observer) {}
    }
}
