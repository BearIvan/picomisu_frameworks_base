// Copyright 2026 Picomisu contributors
// SPDX-License-Identifier: Apache-2.0
package com.pxr.bluetooth;

import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.os.RemoteException;
import android.util.Log;

import java.util.HashMap;
import java.util.Map;

/**
 * Device-manager profile of the PICO Bluetooth service (factory PICO OS 5.13.7
 * com.pxr.bluetooth.BluetoothPxrDeviceManager): connected PICO peripherals and their
 * connection/bond state callbacks. As in the factory, bond changes are reported to per-device
 * listeners through onConnectionStateChanged.
 * @hide
 */
public class BluetoothPxrDeviceManager implements BluetoothPxrProfile {
    private final String TAG = "PxrDeviceManager";
    private Context mContext;
    private IBluetoothPxr mBinder;
    private ProfileProxyListener mProxyListener;
    private PxrBluetoothDeviceStateListener mMacroStateListener;
    private Map<BluetoothDevice, PxrBluetoothDeviceStateListener> mDeviceListenerMap =
            new HashMap<>();
    private Object mLock = new Object();

    public interface PxrBluetoothDeviceStateListener {
        void onConnectionStateChanged(BluetoothPxrDeviceProperty property, int preState,
                int newState);

        void onBondStateChanged(BluetoothPxrDeviceProperty property, int preState, int newState);
    }

    private IBluetoothPxrDeviceCallback mPxrDeviceStateChanged =
            new IBluetoothPxrDeviceCallback.Stub() {
        @Override
        public void onDeviceConnectionStateChanged(BluetoothPxrDeviceProperty property,
                int preState, int newState) {
            Log.d(TAG, "onDeviceConnectionStateChanged");
            synchronized (mLock) {
                if (mMacroStateListener != null) {
                    mMacroStateListener.onConnectionStateChanged(property, preState, newState);
                }
            }
            synchronized (mLock) {
                for (BluetoothDevice dev : mDeviceListenerMap.keySet()) {
                    if (property.getDevice().getAddress().equals(dev.getAddress())) {
                        PxrBluetoothDeviceStateListener listener = mDeviceListenerMap.get(dev);
                        if (listener != null) {
                            listener.onConnectionStateChanged(property, preState, newState);
                        }
                    }
                }
            }
        }

        @Override
        public void onDeviceBondStateChanged(BluetoothPxrDeviceProperty property, int preState,
                int newState) {
            Log.d(TAG, "onDeviceBondStateChanged");
            synchronized (mLock) {
                if (mMacroStateListener != null) {
                    mMacroStateListener.onBondStateChanged(property, preState, newState);
                }
            }
            synchronized (mLock) {
                for (BluetoothDevice dev : mDeviceListenerMap.keySet()) {
                    if (property.getDevice().getAddress().equals(dev.getAddress())) {
                        PxrBluetoothDeviceStateListener listener = mDeviceListenerMap.get(dev);
                        if (listener != null) {
                            listener.onConnectionStateChanged(property, preState, newState);
                        }
                    }
                }
            }
        }
    };

    BluetoothPxrDeviceManager(Context context) {
        mContext = context;
    }

    @Override
    public void setProxyListener(ProfileProxyListener listener) {
        synchronized (mLock) {
            mProxyListener = listener;
        }
    }

    @Override
    public void notifyProxy() {
        notifyProxyInternal();
    }

    @Override
    public void onProxyStateChanged(boolean isConnect, IBluetoothPxr binder) {
        setBinderInternal(binder);
        if (isConnect) {
            onProxyStateChangedInternal();
        }
    }

    @Override
    public void cleanup() {
        synchronized (mLock) {
            mBinder = null;
            mDeviceListenerMap.clear();
            mMacroStateListener = null;
            mProxyListener = null;
        }
    }

    private synchronized void setBinderInternal(IBluetoothPxr binder) {
        Log.i(TAG, "setBinder");
        synchronized (mLock) {
            mBinder = binder;
        }
    }

    private synchronized void notifyProxyInternal() {
        boolean isConnected = mBinder != null;
        synchronized (mLock) {
            if (mProxyListener != null) {
                if (isConnected) {
                    mProxyListener.onProxyConnected(PROFILE_DEVICE_MGR, this);
                } else {
                    mProxyListener.onProxyDisconnected(PROFILE_DEVICE_MGR);
                }
            }
        }
    }

    private synchronized void onProxyStateChangedInternal() {
        if (mBinder == null) {
            return;
        }
        synchronized (mBinder) {
            try {
                mBinder.registerDeviceStateCallback(mPxrDeviceStateChanged);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
    }

    public void setDeviceStateListener(PxrBluetoothDeviceStateListener listener) {
        Log.d(TAG, "setDeviceStateListener macro");
        synchronized (mLock) {
            mMacroStateListener = listener;
        }
    }

    public void setDeviceStateListener(BluetoothDevice device,
            PxrBluetoothDeviceStateListener listener) {
        Log.d(TAG, "setDeviceStateListener device");
        synchronized (mLock) {
            if (listener != null) {
                mDeviceListenerMap.put(device, listener);
            } else if (mDeviceListenerMap.containsKey(device)) {
                mDeviceListenerMap.remove(device);
            }
        }
    }

    public BluetoothPxrDeviceProperty[] getConnectedDevices() {
        Log.d(TAG, "getConnectedDevices");
        final IBluetoothPxr binder = mBinder;
        if (binder == null) {
            return null;
        }
        synchronized (binder) {
            try {
                return mBinder.getConnectedDevices();
            } catch (RemoteException e) {
                e.printStackTrace();
                return null;
            }
        }
    }
}
