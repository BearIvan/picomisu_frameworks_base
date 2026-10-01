// Copyright 2026 Picomisu contributors
// SPDX-License-Identifier: Apache-2.0
package com.pvr.pxrnotification;

import android.content.Context;
import android.os.Binder;
import android.os.IBinder;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.util.Log;
import android.util.SparseArray;

import com.pvr.pxrnotification.aidl.IPxrNotificationCallback;
import com.pvr.pxrnotification.aidl.IPxrNotificationService;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

/**
 * PXR notification service and client accessor (factory PICO OS 5.13.7 services.jar
 * com.pvr.pxrnotification.PxrNotificationService, statically linked PxrNotification library).
 *
 * <p>system_server only uses {@link #getInstance}: the "pxr_notification" binder is looked up
 * once, when the class is initialized, and that proxy (or null) is returned from then on.
 */
public class PxrNotificationService extends IPxrNotificationService.Stub {
    private static final String TAG = "PxrNotificationService";
    private static Context mContext;
    private Object mCallbackLock = new Object();
    private SparseArray<PxrCallbackRecord> mPvrCallbacks = new SparseArray<>();
    private ArrayList<PxrCallbackRecord> mTempPvrCallbacks = new ArrayList<>();
    private static List<HashMap<String, IPxrNotificationCallback>> mCallbackList = null;
    private static HashMap<String, IPxrNotificationCallback> mCallbackMap = null;
    private static IPxrNotificationService mIPxrNotificationService =
            IPxrNotificationService.Stub.asInterface(ServiceManager.getService("pxr_notification"));

    public static class LoadService {
        private static final PxrNotificationService pxr_service = new PxrNotificationService();
    }

    public PxrNotificationService() {
    }

    public PxrNotificationService(Context context) {
    }

    public static IPxrNotificationService getInstance(Context context) {
        mContext = context;
        if (mIPxrNotificationService == null) {
            Log.i(TAG, "getInstance sInst is null");
            synchronized (PxrNotificationService.class) {
            }
            return mIPxrNotificationService;
        }
        Log.i(TAG, "getInstance already init");
        return mIPxrNotificationService;
    }

    @Override
    public void addPxrCallback(String action, int type, IPxrNotificationCallback pcb)
            throws RemoteException {
        Log.i(TAG, "pcb is " + pcb + " ,action:" + action + " ,type:" + type);
        if (pcb == null) {
            Log.i(TAG, ">>addPxrCallback callback is null");
            return;
        }
        Log.i(TAG, ">>addPxrCallback action:" + action + ",type:" + type);
        synchronized (mCallbackLock) {
            int callingPid = Binder.getCallingPid();
            Log.v(TAG, "addCallBackToMap..callingPid:" + callingPid);
            HashMap<String, IPxrNotificationCallback> map = new HashMap<>();
            map.put(action, pcb);
            PxrCallbackRecord record = new PxrCallbackRecord(callingPid, map);
            try {
                IBinder binder = pcb.asBinder();
                binder.linkToDeath(record, 0);
                mPvrCallbacks.put(callingPid, record);
            } catch (RemoteException ex) {
                throw new RuntimeException(ex);
            }
        }
    }

    @Override
    public void sendPxrMessage(String action, int id, String value1, int value2, String ext) {
        Log.d(TAG, "sendPxrMessage  action " + action + " ,value1:" + value1 + " ,value2:" + value2
                + " ,ext:" + ext);
        StringBuilder sb = new StringBuilder();
        sb.append(" mPvrCallbacks.size():");
        sb.append(mPvrCallbacks.size());
        Log.d(TAG, sb.toString());
        synchronized (mCallbackLock) {
            int count = mPvrCallbacks.size();
            mTempPvrCallbacks.clear();
            for (int i = 0; i < count; i++) {
                if (mPvrCallbacks.valueAt(i).mMap.containsKey(action)
                        || PxrNotificationManager.ACTION_CONFIGURE_CHANGE_NOTIFY_ALL.equals(action)) {
                    mTempPvrCallbacks.add(mPvrCallbacks.valueAt(i));
                }
            }
            Log.d(TAG, "mTempPvrCallbacks size:" + mTempPvrCallbacks.size());
            int tempListSize = mTempPvrCallbacks.size();
            for (int i = 0; i < tempListSize; i++) {
                mTempPvrCallbacks.get(i).notifyCallbackEvent(action, value1, value2, ext);
            }
        }
        mTempPvrCallbacks.clear();
    }

    @Override
    public void removePxrCallback(String action) {
        int pid = Binder.getCallingPid();
        Log.d(TAG, "removePxrCallback pid: " + pid + ",action" + action);
        synchronized (mCallbackLock) {
            int count = mPvrCallbacks.size();
            for (int i = 0; i < count; i++) {
                if (mPvrCallbacks.valueAt(i).mPid == pid
                        && mPvrCallbacks.valueAt(i).mMap.containsKey(action)) {
                    mPvrCallbacks.valueAt(i).mMap.remove(action);
                    Log.d(TAG, "PvrCallbacks.valueAt(i).mMap.size : "
                            + mPvrCallbacks.valueAt(i).mMap.size());
                }
            }
        }
    }

    public void removeCallback(int pid) {
        synchronized (mCallbackLock) {
            mPvrCallbacks.remove(pid);
        }
    }

    private final class PxrCallbackRecord implements IBinder.DeathRecipient {
        private IPxrNotificationCallback mCallback;
        private HashMap<String, IPxrNotificationCallback> mMap;
        private int mPid;

        public PxrCallbackRecord(int pid, HashMap<String, IPxrNotificationCallback> map) {
            mPid = pid;
            mMap = map;
        }

        @Override
        public void binderDied() {
            Log.d(TAG, "PvrCallback  pid " + mPid + " died ");
            removeCallback(mPid);
        }

        public void notifyCallbackEvent(String action, String value1, int value2, String ext) {
            Log.d(TAG, "notifyCallbackEvent  action: " + action + " ,mMap.size:" + mMap.size()
                    + " ,value1:" + value1 + "value2:" + value2 + ext);
            Iterator<String> it = mMap.keySet().iterator();
            while (it.hasNext()) {
                String key = it.next().toString();
                String value = mMap.get(key).toString();
                Log.i(TAG, "mMap key:" + key + " ,value:" + value);
                try {
                    if (mMap.containsKey(action)
                            || PxrNotificationManager.ACTION_CONFIGURE_CHANGE_NOTIFY_ALL.equals(action)) {
                        Log.i(TAG, "mMap ready send: ,value:" + mMap.get(action)
                                + " ,actual send value:" + value);
                        mMap.get(key).onEventChanged(value1, value2, ext);
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Faid to notify process " + mPid
                            + " that event changed,assuming it died.", e);
                    binderDied();
                }
            }
        }
    }
}
