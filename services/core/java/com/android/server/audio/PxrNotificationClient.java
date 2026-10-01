// Copyright 2026 Picomisu contributors
// SPDX-License-Identifier: Apache-2.0

package com.android.server.audio;

import android.os.IBinder;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.util.Log;

import com.pvr.pxrnotification.aidl.IPxrNotificationCallback;
import com.pvr.pxrnotification.aidl.IPxrNotificationService;

/**
 * Client of the PICO "pxr_notification" service, as in the PICO OS 5.13.7 factory services.
 * The service is waited for (500 ms polling) on first use.
 */
public class PxrNotificationClient implements IBinder.DeathRecipient {
    private static final String SERVICE_NAME = "pxr_notification";
    private static final int SLEEP_MS = 500;
    private static final String TAG = "PxrNotificationClient";
    private IPxrNotificationService mService;

    @Override
    public void binderDied() {
        onBinderDied();
    }

    private synchronized void onBinderDied() {
        mService = null;
    }

    private synchronized IPxrNotificationService getService() {
        while (mService == null) {
            try {
                IBinder binder = ServiceManager.getService(SERVICE_NAME);
                if (binder != null) {
                    binder.linkToDeath(this, 0);
                    mService = IPxrNotificationService.Stub.asInterface(binder);
                    break;
                }
                Log.e(TAG, "cannot get service " + SERVICE_NAME + ", wait " + SLEEP_MS + " ms");
                Thread.sleep(SLEEP_MS);
            } catch (RemoteException | InterruptedException e) {
                Log.e(TAG, e.toString());
            }
        }
        return mService;
    }

    public void addPxrCallback(String action, int id, IPxrNotificationCallback pcb)
            throws RemoteException {
        getService().addPxrCallback(action, id, pcb);
    }

    public void sendPxrMessage(String action, int id, String value1, int value2, String ext)
            throws RemoteException {
        getService().sendPxrMessage(action, id, value1, value2, ext);
    }

    public void removePxrCallback(String action) throws RemoteException {
        getService().removePxrCallback(action);
    }
}
