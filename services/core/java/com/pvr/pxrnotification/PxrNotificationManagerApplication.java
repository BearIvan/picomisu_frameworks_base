// Copyright 2026 Picomisu contributors
// SPDX-License-Identifier: Apache-2.0
package com.pvr.pxrnotification;

import android.app.Application;
import android.os.ServiceManager;
import android.util.Log;

/**
 * Application of the PXR notification service app (factory PICO OS 5.13.7 services.jar
 * com.pvr.pxrnotification.PxrNotificationManagerApplication, statically linked
 * PxrNotification library; unused inside system_server).
 */
public class PxrNotificationManagerApplication extends Application {
    static final String TAG = "PxrNotificationManagerApplication";
    public static PxrNotificationService mService;

    @Override
    public void onCreate() {
        super.onCreate();
        Log.i(TAG, "PxrNotificationManagerApplication onCreate");
        mService = new PxrNotificationService(this);
        ServiceManager.addService("pxr_notification", mService);
    }

    public static PxrNotificationService getPvrService() {
        return mService;
    }
}
