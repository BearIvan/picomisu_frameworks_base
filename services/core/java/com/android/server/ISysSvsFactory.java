// Copyright 2026 Picomisu contributors
// SPDX-License-Identifier: Apache-2.0

package com.android.server;

import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.os.DebugSmtEx;
import android.os.PowerAdvisorInternal;
import android.os.WorkSource;

import com.android.server.am.ActivityManagerService;
import com.android.server.am.IBatteryStatsServiceOptEx;
import com.android.server.am.IHandleMemoryLeak;
import com.android.server.am.IMemoryProcessController;
import com.android.server.am.ISingle3DApp;
import com.android.server.am.ITaskDeepClean;
import com.android.server.power.ISmartisanPowerAdvisor;
import com.android.server.wm.IActivityTaskManagerOptEx;

/**
 * Factory of the Smartisan system service optimizations implemented by the optional sys
 * services JAR ({@link SysOptBridge}). Reconstructed from the PICO OS 5.13.7 factory services;
 * only the getters reached by the ported factory code are present, with their factory default
 * implementations.
 *
 * @hide
 */
public interface ISysSvsFactory extends ISysMonitorSvcFactory {
    default ITaskDeepClean getTaskDeepClean() {
        return new ITaskDeepClean() {};
    }

    default ISingle3DApp getSingle3DApp() {
        return new ISingle3DApp() {};
    }

    default IActivityTaskManagerOptEx getAtmOptEx() {
        return new IActivityTaskManagerOptEx() {};
    }

    default IActivityManagerOptEx getActivityManager(ActivityManagerService service) {
        return new IActivityManagerOptEx() {};
    }

    default ISmartService getSmartService() {
        return new ISmartService() {};
    }

    default ISmartScenes getSmartScenes() {
        return new ISmartScenes() {};
    }

    default IMemoryProcessController getMemoryProcessController() {
        return new IMemoryProcessController() {};
    }

    default IMemMonitor getMemMonitor() {
        return new IMemMonitor() {};
    }

    default ISmtResourceControl getSmtResourceControl() {
        return new ISmtResourceControl() {};
    }

    default IBatteryStatsServiceOptEx getBatteryStatsServiceOptEx() {
        return new IBatteryStatsServiceOptEx() {};
    }

    default ISysPrefetchService getSysPrefetchService() {
        return new ISysPrefetchService() {};
    }

    default IHandleMemoryLeak getHandleMemoryLeak() {
        return new IHandleMemoryLeak() {};
    }

    default IPrefetchManagerService getPrefetchManager() {
        return new IPrefetchManagerService() {};
    }

    default ISmartisanPowerAdvisor getSmartisanPowerAdvisorInstance() {
        return new ISmartisanPowerAdvisor() {};
    }

    /**
     * Publishes the {@link PowerAdvisorInternal} local service; the default one never limits
     * anything (the sys services JAR starts {@code SmartisanPowerAdvisor} instead).
     */
    default void startSmartisanPowerAdvisor(SystemServiceManager ssm) {
        try {
            LocalServices.addService(PowerAdvisorInternal.class, new PowerAdvisorInternal() {
                public boolean wakelockCouldDisabled(String packageName, int uid, String tag,
                        boolean deviceIdle) {
                    return false;
                }

                public boolean wakelockCouldDisabled(int uid, String tag, WorkSource workSource,
                        boolean deviceIdle) {
                    return false;
                }

                @Override
                public int shouldAppKillSkipInDeviceIdle(ApplicationInfo applicationInfo) {
                    return 0;
                }

                @Override
                public boolean shouldAppKillSkipInDeviceIdleDrainFast(
                        ApplicationInfo applicationInfo) {
                    return false;
                }

                public void uploadDisabledPackages() {
                }

                @Override
                public void switchPerfileWhenScreenUpdate(boolean screenOn) {
                }

                @Override
                public void entryPCMode(boolean PCMode) {
                }

                @Override
                public void reportEvent(int eventType, String packageName, int userId) {
                }

                @Override
                public long getAdjustUsedElapsedTime(int userId, String packageName) {
                    return 0;
                }

                @Override
                public boolean inPowerCheckBlacklist(String packageName, int uid) {
                    return false;
                }

                @Override
                public void notifyRecentPSPShow(boolean show, boolean switchPerf,
                        boolean fromRecent) {
                }

                @Override
                public void notifyPowerAppSwitch(ActivityInfo activityInfo) {
                }

                @Override
                public void notifyLimitedChanged() {
                }

                @Override
                public void updatePowerAdvisorFeatureEnable(String tag, int state) {
                }

                @Override
                public boolean inSleepMode() {
                    return false;
                }

                @Override
                public boolean inDozeMode() {
                    return false;
                }
            });
        } catch (Exception e) {
        } finally {
            DebugSmtEx.printDefaultFunInfo(getClass());
        }
    }
}
