// Copyright 2026 Picomisu contributors
// SPDX-License-Identifier: Apache-2.0
package com.pxr.bluetooth;

import android.bluetooth.IBluetoothManager;
import android.bluetooth.IBluetoothStateChangeCallback;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Process;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.util.Log;

import java.util.HashMap;
import java.util.Map;

/**
 * Client entry point of the PICO Bluetooth service "com.pxr.bluetooth.IBluetoothPxr" (factory
 * PICO OS 5.13.7 com.pxr.bluetooth.BluetoothPxrAdapter): binds the service while Bluetooth is
 * up and hands out profile proxies (only {@link BluetoothPxrProfile#PROFILE_DEVICE_MGR}).
 * @hide
 */
public class BluetoothPxrAdapter {
    private final String TAG = "BluetoothPxrAdapter";
    private static final String BLUETOOTH_MANAGER_SERVICE = "bluetooth_manager";
    private static String SERVICE_ACTION = "com.pxr.bluetooth.IBluetoothPxr";
    private final int MSG_SERVICE_CONNECTED = 1;
    private final int MSG_SERVICE_DISCONNECTED = 2;
    private static BluetoothPxrAdapter sInstant;
    private static ServiceDeathRecipient mDeathRecipient;
    private Context mContext;
    private IBluetoothPxr mService;
    private IBluetoothManager mBluetoothManager;
    private Map<Integer, BluetoothPxrProfile> mProxyList = new HashMap<>();
    private Object mListLock = new Object();

    private ServiceConnection mConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            Log.d(TAG, "onServiceConnected");
            mService = IBluetoothPxr.Stub.asInterface(service);
            mHandler.sendEmptyMessage(MSG_SERVICE_CONNECTED);
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            Log.d(TAG, "onServiceDisconnected");
            mService = null;
            mHandler.sendEmptyMessage(MSG_SERVICE_DISCONNECTED);
        }
    };

    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_SERVICE_CONNECTED:
                    synchronized (mListLock) {
                        for (BluetoothPxrProfile profile : mProxyList.values()) {
                            profile.onProxyStateChanged(true, mService);
                            profile.notifyProxy();
                        }
                    }
                    break;
                case MSG_SERVICE_DISCONNECTED:
                    synchronized (mListLock) {
                        for (BluetoothPxrProfile profile : mProxyList.values()) {
                            profile.onProxyStateChanged(false, null);
                            profile.notifyProxy();
                        }
                    }
                    break;
            }
        }
    };

    private final IBluetoothStateChangeCallback mBluetoothStateChangeCallback =
            new IBluetoothStateChangeCallback.Stub() {
        @Override
        public void onBluetoothStateChange(boolean up) {
            if (up) {
                doBind();
            } else {
                doUnbind();
            }
        }
    };

    private class ServiceDeathRecipient implements IBinder.DeathRecipient {
        private ServiceDeathRecipient() {
        }

        @Override
        public void binderDied() {
            Log.d(TAG, "service is dead");
            mService = null;
            mHandler.sendEmptyMessage(MSG_SERVICE_DISCONNECTED);
        }
    }

    public static BluetoothPxrAdapter getBluetoothPxrAdapter(Context context) {
        if (sInstant == null) {
            IBinder b = ServiceManager.getService(BLUETOOTH_MANAGER_SERVICE);
            if (b != null) {
                IBluetoothManager managerService = IBluetoothManager.Stub.asInterface(b);
                sInstant = new BluetoothPxrAdapter(context, managerService);
            }
        }
        return sInstant;
    }

    BluetoothPxrAdapter(Context context, IBluetoothManager manager) {
        mContext = context;
        mDeathRecipient = new ServiceDeathRecipient();
        mBluetoothManager = manager;
        final IBluetoothManager bluetoothManager = mBluetoothManager;
        if (bluetoothManager != null) {
            try {
                bluetoothManager.registerStateChangeCallback(mBluetoothStateChangeCallback);
            } catch (RemoteException re) {
                Log.e(TAG, "Failed to register state change callback. " + re);
            }
        }
        doBind();
    }

    private boolean profileIdInvalid(int profile) {
        return profile == BluetoothPxrProfile.PROFILE_DEVICE_MGR;
    }

    public boolean getProfileProxy(int profile,
            BluetoothPxrProfile.ProfileProxyListener listener) {
        if (listener == null || !profileIdInvalid(profile)) {
            return false;
        }
        BluetoothPxrProfile proxy = null;
        synchronized (mListLock) {
            proxy = mProxyList.get(profile);
            if (proxy == null && profile == BluetoothPxrProfile.PROFILE_DEVICE_MGR) {
                proxy = new BluetoothPxrDeviceManager(mContext);
                mProxyList.put(BluetoothPxrProfile.PROFILE_DEVICE_MGR, proxy);
            }
        }
        if (proxy == null) {
            return false;
        }
        final IBluetoothPxr service = mService;
        if (service != null) {
            proxy.onProxyStateChanged(true, service);
        }
        proxy.setProxyListener(listener);
        proxy.notifyProxy();
        return true;
    }

    public boolean closeProfileProxy(int profile) {
        if (!profileIdInvalid(profile)) {
            return false;
        }
        BluetoothPxrProfile proxy;
        synchronized (mListLock) {
            proxy = mProxyList.get(profile);
        }
        if (proxy == null) {
            return false;
        }
        proxy.cleanup();
        synchronized (mListLock) {
            mProxyList.remove(profile);
        }
        if (mProxyList.isEmpty()) {
            Log.d(TAG, "Proxy list in empty, unbind service");
            final IBluetoothManager bluetoothManager = mBluetoothManager;
            if (bluetoothManager != null) {
                try {
                    bluetoothManager.unregisterStateChangeCallback(
                            mBluetoothStateChangeCallback);
                } catch (RemoteException re) {
                    Log.e(TAG, "Failed to unregister state change callback" + re);
                }
            }
            doUnbind();
        }
        return true;
    }

    private boolean doBind() {
        Log.d(TAG, "doBind");
        synchronized (mConnection) {
            if (mService == null) {
                Intent intent = new Intent(SERVICE_ACTION);
                ComponentName comp = intent.resolveSystemService(mContext.getPackageManager(), 0);
                intent.setComponent(comp);
                if (comp == null || !mContext.bindServiceAsUser(intent, mConnection, 0,
                        Process.myUserHandle())) {
                    Log.e(TAG, "Could not bind to BluetoothPxrService with " + intent);
                    return false;
                }
            }
        }
        return true;
    }

    private void doUnbind() {
        Log.d(TAG, "doUnbind");
        synchronized (mConnection) {
            if (mService != null) {
                Log.d(TAG, "Unbinding service...");
                try {
                    mContext.unbindService(mConnection);
                } catch (IllegalArgumentException ie) {
                    Log.e(TAG, "Unable to unbind service: " + ie);
                } finally {
                    mService = null;
                }
            }
        }
    }
}
