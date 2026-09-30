// Copyright 2026 Picomisu contributors
// SPDX-License-Identifier: Apache-2.0

package com.android.server;

import android.os.Bundle;
import android.os.ServiceManager;
import android.util.Slog;

import com.pvr.ISysDataSyncService;

/**
 * Client of the PICO "sysdata_sync" data reporting service. Reconstructed from the PICO OS
 * 5.13.7 factory framework. The service is registered by a PICO system app; while it is absent
 * every call is a logged no-op.
 *
 * @hide
 */
public class SysDataSyncServiceManager {
    private static String TAG = SysDataSyncServiceManager.class.getSimpleName();

    private static final String SYSDATA_SYNC_NAME = "sysdata_sync";

    public static String EVENT_INSTANCE_ID = "INSTANCE_ID";
    public static String EVENT_PID = "EVENT_PID";
    public static String EVENT_PACKAGE_NAME = "PACKAGE_NAME";
    public static String EVENT_ACTIVITY_NAME = "ACTIVITY_NAME";
    public static String EVENT_TIMESTAMP = "TIMESTAMP";
    public static String EVENT_TYPE = "EventType";
    public static String EVENT_METHOD = "EventMethod";
    public static String METHOD_APP_DIED = "OnAppDied";
    public static String METHOD_REPORT_EVENT = "ReportEvent";

    private static ISysDataSyncService sSysDataSyncService;

    public static void onAppDied(int pid) {
        ISysDataSyncService service = getSysDataSyncService();

        if (service != null) {
            try {
                Bundle bundle = new Bundle();
                bundle.putInt(EVENT_PID, pid);
                bundle.putString(EVENT_METHOD, METHOD_APP_DIED);
                service.onAppDied(bundle);
            } catch (Exception e) {
                Slog.e(TAG, "Call method onAppDied error: ", e);
            }
        }
    }

    public static void onUsageEvent(int event, String className, int instance, int pid,
            String pkg, long timestamp) {
        ISysDataSyncService service = getSysDataSyncService();

        if (service != null) {
            try {
                Bundle bundle = new Bundle();
                bundle.putInt(EVENT_TYPE, event);
                bundle.putString(EVENT_ACTIVITY_NAME, className);
                bundle.putInt(EVENT_INSTANCE_ID, instance);
                bundle.putInt(EVENT_PID, pid);
                bundle.putString(EVENT_PACKAGE_NAME, pkg);
                bundle.putLong(EVENT_TIMESTAMP, timestamp);
                bundle.putString(EVENT_METHOD, METHOD_REPORT_EVENT);

                service.onUsageEvent(bundle);
            } catch (Exception e) {
                Slog.e(TAG, "Call method onActivityUsageEvent error: ", e);
            }
        }
    }

    public static void onReportAid(String pkg, String aid) {
        ISysDataSyncService service = getSysDataSyncService();

        if (service != null) {
            try {
                service.onReportAid(pkg, aid);
            } catch (Exception e) {
                Slog.e(TAG, "Call method onReportAid error: ", e);
            }
        }
    }

    public static int queryProfile(String profileType, String packageName, String versionName,
            String versionCode, String filePath) {
        ISysDataSyncService service = getSysDataSyncService();

        if (service != null) {
            try {
                return service.queryProfile(profileType, packageName, versionName, versionCode,
                        filePath);
            } catch (Exception e) {
                Slog.e(TAG, "Call method queryProfile error: ", e);
                return 3;
            }
        }
        return 3;
    }

    public static boolean uploadProfile(String profileType, String packageName,
            String versionName, String versionCode, String filePath) {
        ISysDataSyncService service = getSysDataSyncService();

        if (service != null) {
            try {
                return service.uploadProfile(profileType, packageName, versionName, versionCode,
                        filePath);
            } catch (Exception e) {
                Slog.e(TAG, "Call method uploadProfile error: ", e);
                return false;
            }
        }
        return false;
    }

    public static void onTeaTrackerEvent(String event, String appid, String params) {
        ISysDataSyncService service = getSysDataSyncService();

        if (service != null) {
            try {
                service.onTeaTrackerEvent(event, appid, params);
            } catch (Exception e) {
                Slog.e(TAG, "Call method onTeaTrackerEvent error: ", e);
            }
        }
    }

    public static void onMetricEvent(String metricEvent, String params) {
        ISysDataSyncService service = getSysDataSyncService();

        if (service != null) {
            try {
                service.onMetricEvent(metricEvent, params);
            } catch (Exception e) {
                Slog.e(TAG, "Call method onMetricEvent error: ", e);
            }
        }
    }

    public static void onSlardarEvent(String event, String params) {
        ISysDataSyncService service = getSysDataSyncService();

        if (service != null) {
            try {
                service.onSlardarEvent(event, params);
            } catch (Exception e) {
                Slog.e(TAG, "Call method onSlardarEvent error: ", e);
            }
        }
    }

    public static void flushTeaTrackerEvents() {
        ISysDataSyncService service = getSysDataSyncService();

        if (service != null) {
            try {
                service.flushTeaTrackerEvents();
            } catch (Exception e) {
                Slog.e(TAG, "Call method flushTeaTrackerEvents error: ", e);
            }
        }
    }

    private static ISysDataSyncService getSysDataSyncService() {
        if (sSysDataSyncService != null && sSysDataSyncService.asBinder() != null
                && !sSysDataSyncService.asBinder().isBinderAlive()) {
            Slog.e(TAG, "Method onUsageEvent, Process sysdata_sync Service is no alive.");
            sSysDataSyncService = null;
        }

        if (sSysDataSyncService == null) {
            if (ServiceManager.getService(SYSDATA_SYNC_NAME) != null) {
                sSysDataSyncService = ISysDataSyncService.Stub.asInterface(
                        ServiceManager.getService(SYSDATA_SYNC_NAME));
            } else {
                Slog.w(TAG, "sysdata_sync has not been added to ServiceManager,do nothing.");
            }
        }
        return sSysDataSyncService;
    }
}
