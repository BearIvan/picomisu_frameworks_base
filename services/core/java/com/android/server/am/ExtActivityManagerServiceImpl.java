// Copyright 2026 Picomisu contributors
// SPDX-License-Identifier: Apache-2.0
package com.android.server.am;

import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.ResolveInfo;
import android.content.pm.ServiceInfo;
import android.os.Binder;
import android.pico.utils.PicoUtils;
import android.text.TextUtils;
import android.util.Log;
import android.util.Slog;

import com.android.server.SysDataSyncServiceManager;

import java.util.List;

/**
 * PICO activity manager extension (factory PICO OS 5.13.7
 * com.android.server.am.ExtActivityManagerServiceImpl): app deaths are reported to the PICO
 * sysdata_sync service, system services of the PICO_PERSISTENT_SERVICE category are kept bound
 * through the persistent connections of the activity task manager extension (at the end of
 * systemReady and when the user is unlocked), and a restarted persistent process picks up the
 * application info of an updated package.
 * @hide
 */
public class ExtActivityManagerServiceImpl implements IExtActivityManagerService {
    static final String TAG = "ActivityManager";
    private ActivityManagerService mBase;

    public ExtActivityManagerServiceImpl(ActivityManagerService service) {
        mBase = service;
    }

    @Override
    public void handleAppDiedLocked(ProcessRecord app) {
        int pid = app.pid;
        SysDataSyncServiceManager.onAppDied(pid);
    }

    @Override
    public void onSystemReadyFinished() {
        Slog.w(TAG, "onSystemReadyFinished, startPicoPersistentService");
        startPicoPersistentService();
    }

    @Override
    public void startPicoPersistentService() {
        Intent intent = new Intent();
        intent.setAction(Intent.ACTION_MAIN);
        intent.addCategory("android.intent.category.PICO_PERSISTENT_SERVICE");
        List<ResolveInfo> serviceList =
                mBase.mContext.getPackageManager().queryIntentServices(intent, 0);
        if (serviceList == null || serviceList.size() == 0) {
            return;
        }
        for (ResolveInfo info : serviceList) {
            if (info == null || info.serviceInfo == null
                    || info.serviceInfo.applicationInfo == null) {
                continue;
            }
            ServiceInfo serviceInfo = info.serviceInfo;
            if (PicoUtils.isSystemApp(serviceInfo.applicationInfo)) {
                ComponentName componentName = serviceInfo.getComponentName();
                Slog.w(TAG, "startPicoPersistentService [" + componentName + "]");
                mBase.mActivityTaskManager.getExt().updatePersistentConnection(componentName,
                        true);
            }
        }
    }

    @Override
    public void updatePersistentApplicationInfo(ProcessRecord app) {
        try {
            ApplicationInfo info = mBase.getPackageManagerInternalLocked().getApplicationInfo(
                    app.info.packageName, 0, Binder.getCallingUid(), 0);
            if (app.isPersistent() && info != null && app.info != null
                    && !TextUtils.equals(info.getCodePath(), app.info.getCodePath())) {
                Log.w(TAG, "update persistent application info");
                app.info = info;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
