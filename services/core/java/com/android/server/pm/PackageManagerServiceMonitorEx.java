// Copyright 2026 Picomisu contributors
// SPDX-License-Identifier: Apache-2.0

package com.android.server.pm;

import android.app.AppGlobals;
import android.content.pm.IPackageManagerMonitorEx;
import android.os.Binder;

import com.android.server.am.ActivityManagerServiceSysMoEx;
import com.android.server.pm.dex.DexoptOptions;
import com.android.server.pm.permission.PermissionManagerServiceInternal;

import smartisanos.util.FeatLog;

import java.util.HashMap;
import java.util.Map;

/**
 * Smartisan system monitor extension of the {@link PackageManagerService}. Reconstructed from
 * the PICO OS 5.13.7 factory services; the task-persist queries and the idle dex2oat CPU
 * observer that the preserved system server JARs use are present.
 *
 * @hide
 */
public class PackageManagerServiceMonitorEx extends IPackageManagerMonitorEx.Stub {
    public static final String TAG = "PackageManager";
    static final int DEX2OPT_IN_SCREENOFF_IDLE_MSG = 200;

    static final Object sIdleDex2oatLock = new Object();
    static boolean sObserverRegistered = false;
    static boolean sDexoptResumed = false;

    static final Map<String, DexoptOptions> pendingDexoptMap = new HashMap<>();

    private static ActivityManagerServiceSysMoEx.CpuStateProvider sCpuStateProvider;

    private PackageManagerService mPackageManagerService;
    /** Never assigned in the factory services either. */
    protected PermissionManagerServiceInternal mPermissionManager;

    public PackageManagerServiceMonitorEx(PackageManagerService packageManagerService) {
        mPackageManagerService = packageManagerService;
    }

    private static ActivityManagerServiceSysMoEx.CpuStateObserver mIdleDex2oatObServer =
            new ActivityManagerServiceSysMoEx.CpuStateObserver() {
        @Override
        public void onCpuState(ActivityManagerServiceSysMoEx.CpuStateObserver.CPU_USAGE_STATE state,
                long timestamp) {
            if (state == ActivityManagerServiceSysMoEx.CpuStateObserver.CPU_USAGE_STATE
                    .CPU_NORMAL) {
                synchronized (sIdleDex2oatLock) {
                    if (sDexoptResumed) {
                        return;
                    }
                    sDexoptResumed = true;
                }
                PackageManagerService pms =
                        (PackageManagerService) AppGlobals.getPackageManager();
                pms.mHandler.removeMessages(DEX2OPT_IN_SCREENOFF_IDLE_MSG);
                pms.mHandler.sendEmptyMessage(DEX2OPT_IN_SCREENOFF_IDLE_MSG);
            } else if (state == ActivityManagerServiceSysMoEx.CpuStateObserver.CPU_USAGE_STATE
                    .CPU_BUSY) {
                synchronized (sIdleDex2oatLock) {
                    sDexoptResumed = false;
                }
            }
        }

        @Override
        public ActivityManagerServiceSysMoEx.CpuStateObserver.NOTIFY_FREQUENCY getNotifyRequest() {
            return ActivityManagerServiceSysMoEx.CpuStateObserver.NOTIFY_FREQUENCY.EVERY_TIME;
        }
    };

    public static void stopIdleDex2oat() {
        synchronized (sIdleDex2oatLock) {
            if (!sObserverRegistered) {
                return;
            }
            unregisterCpuStateObserver(mIdleDex2oatObServer);
            sObserverRegistered = false;
            sDexoptResumed = false;
        }
    }

    private static void unregisterCpuStateObserver(
            ActivityManagerServiceSysMoEx.CpuStateObserver observer) {
        if (sCpuStateProvider != null) {
            sCpuStateProvider.unregisterCpuStateObserver(observer);
            FeatLog.d(TAG, "FEAT_DELAY_DEX2OAT", 20, "unregisterCpuStateObserver");
        }
    }

    public boolean isTaskPersist(String packageName, int userId) {
        if (packageName == null) {
            return false;
        }
        int callingUid = Binder.getCallingUid();
        mPermissionManager.enforceCrossUserPermission(callingUid, userId, true, false,
                "persist task");
        return mPackageManagerService.mSettings.getSmtEx().isTaskPersist(packageName, userId);
    }

    public static boolean isTaskPersist(int uid) {
        return SettingsSmtEx.mAllTaskPersistUids.get(uid) > 0;
    }
}
