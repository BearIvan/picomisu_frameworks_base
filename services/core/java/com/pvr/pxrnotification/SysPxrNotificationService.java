// Copyright 2026 Picomisu contributors
// SPDX-License-Identifier: Apache-2.0
package com.pvr.pxrnotification;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

/**
 * Bound service of the PXR notification service app (factory PICO OS 5.13.7 services.jar
 * com.pvr.pxrnotification.SysPxrNotificationService, statically linked PxrNotification
 * library; unused inside system_server).
 */
public class SysPxrNotificationService extends Service {
    private static final String TAG = "SysPxrNotificationService";
    private PxrNotificationService mInst;

    @Override
    public void onCreate() {
        Log.i(TAG, ">>onCreate");
        super.onCreate();
    }

    @Override
    public IBinder onBind(Intent intent) {
        Log.i(TAG, Thread.currentThread().getName() + " onBind");
        if (mInst == null) {
            mInst = PxrNotificationManagerApplication.getPvrService();
        }
        PxrNotificationService pxrNotificationService = mInst;
        if (pxrNotificationService != null) {
            return pxrNotificationService;
        }
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_REDELIVER_INTENT;
    }
}
