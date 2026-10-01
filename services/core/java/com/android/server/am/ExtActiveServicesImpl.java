// Copyright 2026 Picomisu contributors
// SPDX-License-Identifier: Apache-2.0
package com.android.server.am;

import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.os.SystemProperties;
import android.util.Slog;

/**
 * PICO service extension (factory PICO OS 5.13.7 com.android.server.am.ExtActiveServicesImpl):
 * with persist.pvr.startbootcompleted=1, a non-system app whose process is not running is not
 * started for a bind with the android.content.SyncAdapter action (the action of the last
 * bindService call).
 * @hide
 */
public class ExtActiveServicesImpl implements IExtActiveServices {
    public static final String SYS_PXR_START_BOOTCOMPLETED = "persist.pvr.startbootcompleted";
    private static final String TAG = "ActivityManager";
    String mAction;
    private ActiveServices mBase;

    public ExtActiveServicesImpl(ActiveServices base) {
        mBase = base;
    }

    @Override
    public void onBindServiceLocked(Intent service) {
        mAction = service.getAction();
    }

    @Override
    public boolean disableStartSyncAdapter(ServiceRecord r, ProcessRecord app) {
        boolean startBootCompleted =
                SystemProperties.getInt(SYS_PXR_START_BOOTCOMPLETED, -1) == 1;
        ApplicationInfo apps = r.appInfo;
        String action;
        if (app == null && startBootCompleted && (action = mAction) != null
                && action.equals("android.content.SyncAdapter")
                && (apps.flags & ApplicationInfo.FLAG_SYSTEM) <= 0) {
            Slog.e(TAG, r.appInfo.packageName
                    + " process is null ,does not allowed SyncAdapter action  to bind service ");
            return true;
        }
        return false;
    }
}
