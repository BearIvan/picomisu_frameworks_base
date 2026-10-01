// Copyright 2026 Picomisu contributors
// SPDX-License-Identifier: Apache-2.0
package com.android.server.devicepolicy;

import android.content.ComponentName;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.os.RemoteException;
import android.os.SystemProperties;
import android.text.TextUtils;
import android.util.Pair;
import android.util.Slog;

import com.android.internal.os.BackgroundThread;
import com.pvr.pxrnotification.PxrNotificationService;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * PICO device policy extension (factory PICO OS 5.13.7
 * com.android.server.devicepolicy.ExtDevicePolicyManagerServiceImpl).
 *
 * <p>On ToB devices (ro.pxr.externalfunc) ManagedProvisioning and the ToB service may provision a
 * device owner after setup. On other devices a non-system app becomes an active admin only while
 * persist.sys.tob.dpm.enabled is true (default), and admin changes are reported to the PICO
 * tracker through pxr_notification.
 */
public class ExtDevicePolicyManagerServiceImpl implements IExtDevicePolicyManagerService {
    private static final String DPM_ENABLED_KEY = "persist.sys.tob.dpm.enabled";
    private static final String EVENT_ENABLED_KEY = "is_disabled";
    private static final String EVENT_KEY = "change_device_owner_status";
    private static final String EVENT_NAME_KEY = "display_name";
    private static final String EVENT_PKG_KEY = "package_name";
    private static final String EVENT_TYPE_KEY = "type";
    protected static final String LOG_TAG = "DevicePolicyManager";
    private static final String TEA_TRACKER_ACTION = "teatracker_event_action";
    private DevicePolicyManagerService mBase;

    public ExtDevicePolicyManagerServiceImpl(DevicePolicyManagerService service) {
        mBase = service;
    }

    @Override
    public boolean tobForceEnableDeviceOwnerProvisioning() {
        if (IS_TOB_DEVICE) {
            try {
                int callerUid = mBase.mInjector.binderGetCallingUid();
                String[] pkgs = mBase.mInjector.getIPackageManager().getPackagesForUid(callerUid);
                for (String pkg : pkgs) {
                    if ("com.android.managedprovisioning".equals(pkg)
                            || "com.pvr.tobservice".equals(pkg)) {
                        return true;
                    }
                }
            } catch (RemoteException e) {
                Slog.e(LOG_TAG,
                        "RemoteException when checkDeviceOwnerProvisioningPreConditionLocked.", e);
            }
        }
        return false;
    }

    @Override
    public boolean isPackageAvailable(String pkgName, int userId) {
        try {
            if (mBase.mIPackageManager != null
                    && mBase.mIPackageManager.isPackageAvailable(pkgName, userId)) {
                return true;
            }
        } catch (RemoteException e) {
            Slog.e(LOG_TAG, "RemoteException when check package:" + pkgName + " available.", e);
        }
        return false;
    }

    @Override
    public boolean canSetActiveAdmin(String packageName) {
        if (IS_TOB_DEVICE) {
            return true;
        }
        Pair<String, Boolean> pair = getAppInfo(packageName);
        sendTrackerEvent(packageName, pair.first, "set");
        return pair.second || SystemProperties.getBoolean(DPM_ENABLED_KEY, true);
    }

    private Pair<String, Boolean> getAppInfo(String packageName) {
        PackageInfo packageInfo = getPackageInfo(packageName);
        boolean isSystemApp = packageInfo != null && packageInfo.applicationInfo != null
                && ((packageInfo.applicationInfo.flags & ApplicationInfo.FLAG_SYSTEM) != 0
                        || (packageInfo.applicationInfo.flags
                                & ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0);
        String appName = getAppName(packageInfo);
        return new Pair<>(appName, isSystemApp);
    }

    private PackageInfo getPackageInfo(String packageName) {
        if (TextUtils.isEmpty(packageName)) {
            return null;
        }
        try {
            return mBase.mInjector.getIPackageManager().getPackageInfo(packageName, 0, 0);
        } catch (RemoteException e) {
            Slog.e(LOG_TAG, "Exception when getPackageInfo:" + packageName, e);
        }
        return null;
    }

    @Override
    public void sendBootEventTrack(DevicePolicyManagerService.DevicePolicyData data) {
        if (IS_TOB_DEVICE) {
            return;
        }
        if (data == null || data.mAdminList.size() == 0) {
            Slog.i(LOG_TAG, "sendBootEventTrack no policy data");
            return;
        }
        DevicePolicyManagerService.ActiveAdmin activeAdmin = data.mAdminList.get(0);
        if (activeAdmin != null && activeAdmin.info != null) {
            String packageName = activeAdmin.info.getPackageName();
            Pair<String, Boolean> pair = getAppInfo(packageName);
            Slog.i(LOG_TAG, "sendBootEventTrack policy is " + packageName);
            sendTrackerEvent(packageName, pair.first, "set");
        }
    }

    @Override
    public void deleteActiveAdmin(ComponentName admin) {
        if (IS_TOB_DEVICE || admin == null) {
            return;
        }
        Pair<String, Boolean> pair = getAppInfo(admin.getPackageName());
        sendTrackerEvent(admin.getPackageName(), pair.first, "delete");
    }

    private void sendTrackerEvent(final String packageName, final String appName,
            final String type) {
        BackgroundThread.getHandler().post(new Runnable() {
            @Override
            public void run() {
                if (PxrNotificationService.getInstance(mBase.mContext) == null) {
                    Slog.w(LOG_TAG, "senTrackerEvent PxrNotificationService is null!");
                    return;
                }
                try {
                    JSONObject jsonObject = new JSONObject();
                    jsonObject.put(EVENT_PKG_KEY, packageName);
                    jsonObject.put(EVENT_NAME_KEY, appName);
                    jsonObject.put(EVENT_ENABLED_KEY,
                            SystemProperties.getBoolean(DPM_ENABLED_KEY, true) ? 0 : 1);
                    jsonObject.put(EVENT_TYPE_KEY, type);
                    PxrNotificationService.getInstance(mBase.mContext).sendPxrMessage(
                            TEA_TRACKER_ACTION, 0, EVENT_KEY, 0, jsonObject.toString());
                } catch (RemoteException | JSONException e) {
                    Slog.e(LOG_TAG, "Exception when check dmp feature:" + packageName, e);
                }
            }
        });
    }

    private String getAppName(PackageInfo packageInfo) {
        if (packageInfo == null || packageInfo.applicationInfo == null) {
            return null;
        }
        return (String) packageInfo.applicationInfo.loadLabel(
                mBase.mContext.getPackageManager());
    }
}
