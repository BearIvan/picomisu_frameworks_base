// Copyright 2026 Picomisu contributors
// SPDX-License-Identifier: Apache-2.0

package com.android.server;

import android.content.Context;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.os.DebugSmtEx;
import android.os.PowerAdvisorInternal;
import android.os.WorkSource;
import android.view.MotionEvent;
import android.view.WindowManagerPolicyConstants;
import com.android.server.am.ActivityManagerService;
import com.android.server.am.IActiveUidsOptEx;
import com.android.server.am.IAddVrPrevious;
import com.android.server.am.IAnrMonitor;
import com.android.server.am.IAppStartStatistics;
import com.android.server.am.IApplicationFreezer;
import com.android.server.am.IBatteryStatsServiceOptEx;
import com.android.server.am.IBinderStat;
import com.android.server.am.IBootEventStat;
import com.android.server.am.ICollect3rdInfo;
import com.android.server.am.IFreezeController;
import com.android.server.am.IFreezeStats;
import com.android.server.am.IHandleMemoryLeak;
import com.android.server.am.IKillingStats;
import com.android.server.am.ILowMemDetectorOptEx;
import com.android.server.am.IMemoryProcessController;
import com.android.server.am.IMemoryReclaimer;
import com.android.server.am.IOomAdjChecker;
import com.android.server.am.IOomAdjusterOptEx;
import com.android.server.am.IPauseTimeoutDataUpload;
import com.android.server.am.IPrefetchStats;
import com.android.server.am.IPrimaryProfCollecter;
import com.android.server.am.IProcStatsSmt;
import com.android.server.am.IProcessIntercept;
import com.android.server.am.IProcessListOptEx;
import com.android.server.am.ISingle3DApp;
import com.android.server.am.ISmartisanBrainBridge;
import com.android.server.am.ITaskDeepClean;
import com.android.server.am.ITntProcessController;
import com.android.server.am.IUidCpuRunner;
import com.android.server.am.IUidMonitorSmt;
import com.android.server.am.OomAdjuster;
import com.android.server.audio.ISmtMediaMonitorService;
import com.android.server.display.IDisplayModeDirectorOptEx;
import com.android.server.location.IAppForegroundHelperOptEx;
import com.android.server.net.INetworkPolicyManagerServiceOptEx;
import com.android.server.notification.INotificationManagerOptEx;
import com.android.server.notification.NotificationManagerService;
import com.android.server.pm.PackageManagerService;
import com.android.server.power.IPowerManagerOptEx;
import com.android.server.power.IQuickBootStateMachine;
import com.android.server.power.ISmartPowerData;
import com.android.server.power.ISmartisanPowerAdvisor;
import com.android.server.wm.ActivityRecord;
import com.android.server.wm.IActivityRecordOptEx;
import com.android.server.wm.IActivityTaskManagerOptEx;

/**
 * Reconstructed from the PICO OS 5.13.7 factory services.
 *
 * @hide
 */
public interface ISysSvsFactory extends ISysMonitorSvcFactory {
    default IUidCpuRunner getUidCpuRunnerInstance() {
        return new IUidCpuRunner() {
        };
    }

    default IUidMonitorSmt getUidMonitorSmt() {
        return new IUidMonitorSmt() {
        };
    }

    default void startSmartisanPowerAdvisor(SystemServiceManager systemServiceManager) {
        try {
            LocalServices.addService(PowerAdvisorInternal.class, new PowerAdvisorInternal() {
                public boolean wakelockCouldDisabled(String packageName, int uid, String tag, boolean deviceIdle) {
                    return false;
                }

                public boolean wakelockCouldDisabled(int uid, String tag, WorkSource workSource, boolean deviceIdle) {
                    return false;
                }

                public int shouldAppKillSkipInDeviceIdle(ApplicationInfo applicationInfo) {
                    return 0;
                }

                public boolean shouldAppKillSkipInDeviceIdleDrainFast(ApplicationInfo applicationInfo) {
                    return false;
                }

                public void uploadDisabledPackages() {
                }

                public void switchPerfileWhenScreenUpdate(boolean screenOn) {
                }

                public void entryPCMode(boolean PCMode) {
                }

                public void reportEvent(int eventType, String packageName, int userId) {
                }

                public long getAdjustUsedElapsedTime(int userId, String packageName) {
                    return 0L;
                }

                public boolean inPowerCheckBlacklist(String packageName, int uid) {
                    return false;
                }

                public void notifyRecentPSPShow(boolean show, boolean switchPerf, boolean fromRecent) {
                }

                public void notifyPowerAppSwitch(ActivityInfo activityInfo) {
                }

                public void notifyLimitedChanged() {
                }

                public void updatePowerAdvisorFeatureEnable(String tag, int state) {
                }

                public boolean inSleepMode() {
                    return false;
                }

                public boolean inDozeMode() {
                    return false;
                }
            });
        } catch (Exception e) {
        } finally {
            DebugSmtEx.printDefaultFunInfo(getClass());
        }
    }

    default IProcessIntercept getProcessIntercept() {
        return new IProcessIntercept() {
        };
    }

    default ISingle3DApp getSingle3DApp() {
        return new ISingle3DApp() {
        };
    }

    default ITaskDeepClean getTaskDeepClean() {
        return new ITaskDeepClean() {
        };
    }

    default IAddVrPrevious getAddVrPrevious() {
        return new IAddVrPrevious() {
        };
    }

    default IAppStartStatistics getAppStartStatistics() {
        return new IAppStartStatistics() {
        };
    }

    default IProcStatsSmt getProcStatsSmt() {
        return new IProcStatsSmt() {
        };
    }

    default void setActivityManagerService(ActivityManagerService activityManagerService) {
        DebugSmtEx.printDefaultFunInfo(getClass());
    }

    @Override
    default IActivityManagerOptEx getActivityManager(ActivityManagerService service) {
        return new IActivityManagerOptEx() {
        };
    }

    @Override
    default ISmartPowerData getSmartPowerDataInstance() {
        return new ISmartPowerData() {
        };
    }

    default ISmartisanPowerAdvisor getSmartisanPowerAdvisorInstance() {
        return new ISmartisanPowerAdvisor() {
        };
    }

    default IMemoryReclaimer getMemoryReclaimer() {
        return new IMemoryReclaimer() {
        };
    }

    default ITntProcessController getTntProcessControllerInstance() {
        return new ITntProcessController() {
        };
    }

    default ICollect3rdInfo getCollect3rdInfo() {
        return new ICollect3rdInfo() {
        };
    }

    default IBinderStat getBinderStat() {
        return new IBinderStat() {
        };
    }

    default IBootEventStat getBootEventStat() {
        return new IBootEventStat() {
        };
    }

    @Override
    default ISmartMonitorController getSmartMonitorController() {
        return new ISmartMonitorController() {
        };
    }

    @Override
    default ISmartService getSmartService() {
        return new ISmartService() {
        };
    }

    default ISmtMediaMonitorService getSmtMediaMonitorService(Context context) {
        return new ISmtMediaMonitorService() {
        };
    }

    default IPauseTimeoutDataUpload getPauseTimeoutDataUpload() {
        return new IPauseTimeoutDataUpload() {
        };
    }

    default IApplicationFreezer getApplicationFreezer() {
        return new IApplicationFreezer() {
        };
    }

    default IMemoryProcessController getMemoryProcessController() {
        return new IMemoryProcessController() {
        };
    }

    default ILowMemDetectorOptEx getLowMemDetectorOptEx() {
        return new ILowMemDetectorOptEx() {
        };
    }

    default IFreezeController getFreezeController() {
        return new IFreezeController() {
        };
    }

    default IPrimaryProfCollecter getPrimaryProfCollecter(PackageManagerService pm) {
        return new IPrimaryProfCollecter() {
        };
    }

    default ISmartisanBrainBridge getSmartisanBrainBridge(Context context) {
        return new ISmartisanBrainBridge() {
        };
    }

    default IFreezeStats createFreezeStats() {
        return new IFreezeStats() {
        };
    }

    default IOomAdjChecker getOomAdjChecker() {
        return new IOomAdjChecker() {
        };
    }

    default ISmartScenes getSmartScenes() {
        return new ISmartScenes() {
        };
    }

    default IKillingStats getKillingStats() {
        return new IKillingStats() {
        };
    }

    default IPrefetchStats getPrefetchStats() {
        return new IPrefetchStats() {
        };
    }

    default IMemMonitor getMemMonitor() {
        return new IMemMonitor() {
        };
    }

    default ISmtResourceControl getSmtResourceControl() {
        return new ISmtResourceControl() {
        };
    }

    @Override
    default ISmartAnaly getSmtAnalysis() {
        return new ISmartAnaly() {
        };
    }

    default IPowerManagerOptEx getPowerManager() {
        return new IPowerManagerOptEx() {
        };
    }

    default ILocationManagerOptEx getLocationManager() {
        return new ILocationManagerOptEx() {
        };
    }

    default INotificationManagerOptEx getNotificationManager(NotificationManagerService service) {
        return new INotificationManagerOptEx() {
        };
    }

    default IOomAdjusterOptEx getOomAdjusterOptEx(OomAdjuster oomAdjuster, ActivityManagerService ams) {
        return new IOomAdjusterOptEx() {
        };
    }

    default INetworkPolicyManagerServiceOptEx getPolicyServiceOptEx() {
        return new INetworkPolicyManagerServiceOptEx() {
        };
    }

    default INetworkManagementServiceOptEx getNetworkManagementServiceOptEx() {
        return new INetworkManagementServiceOptEx() {
        };
    }

    @Override
    default IBatteryStatsServiceOptEx getBatteryStatsServiceOptEx() {
        return new IBatteryStatsServiceOptEx() {
        };
    }

    @Override
    default IBatteryServiceOptEx getBatteryServiceOptEx() {
        return new IBatteryServiceOptEx() {
        };
    }

    default WindowManagerPolicyConstants.PointerEventListener getScenesPointerEventListener() {
        return new WindowManagerPolicyConstants.PointerEventListener() {
            public void onPointerEvent(MotionEvent motionEvent) {
                DebugSmtEx.printDefaultFunInfo(getClass());
            }
        };
    }

    default IAppForegroundHelperOptEx getAppForegroundHelperOptEx() {
        return new IAppForegroundHelperOptEx() {
        };
    }

    @Override
    default IAnrMonitor getAnrMonitor() {
        return new IAnrMonitor() {
        };
    }

    @Override
    default ISysMonitorService getSysMonitorService() {
        return new ISysMonitorService() {
        };
    }

    default IProcessListOptEx getProcessListOptEx() {
        return null;
    }

    default IActiveUidsOptEx getActiveUidsOptEx() {
        return null;
    }

    default IGameBalanceService getGameBalanceService() {
        return new IGameBalanceService() {
        };
    }

    default ISysPrefetchService getSysPrefetchService() {
        return new ISysPrefetchService() {
        };
    }

    default IQuickBootStateMachine getQBStateMachine() {
        return new IQuickBootStateMachine() {
        };
    }

    default IDisplayModeDirectorOptEx getDisplayModeDirectorOptEx() {
        return new IDisplayModeDirectorOptEx() {
        };
    }

    default IActivityTaskManagerOptEx getAtmOptEx() {
        return new IActivityTaskManagerOptEx() {
        };
    }

    default IActivityRecordOptEx createActivityRecordOptEx(ActivityRecord record) {
        return null;
    }

    @Override
    default IHandleMemoryLeak getHandleMemoryLeak() {
        return new IHandleMemoryLeak() {
        };
    }

    default IPrefetchManagerService getPrefetchManager() {
        return new IPrefetchManagerService() {
        };
    }
}
