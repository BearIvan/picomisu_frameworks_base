// Copyright 2026 Picomisu contributors
// SPDX-License-Identifier: Apache-2.0
package com.pxr.bluetooth;

/**
 * A profile proxy of the PICO Bluetooth service (factory PICO OS 5.13.7
 * com.pxr.bluetooth.BluetoothPxrProfile).
 * @hide
 */
public interface BluetoothPxrProfile {
    int PROFILE_DEVICE_MGR = 1;

    int STATE_UNUSED = -1;
    int STATE_DISCONNECTED = 0;
    int STATE_CONNECTING = 1;
    int STATE_CONNECTED = 2;
    int STATE_DISCONNECTING = 3;

    int BOND_UNUSED = -1;
    int BOND_NONE = 10;
    int BOND_BONDING = 11;
    int BOND_BONDED = 12;

    int TYPE_UNKNOWN = 0;
    int TYPE_AUDIO = 1;
    int TYPE_KEYBOARD = 2;
    int TYPE_MOUSE = 3;
    int TYPE_SWIFT = 4;

    interface ProfileProxyListener {
        void onProxyConnected(int profile, BluetoothPxrProfile proxy);

        void onProxyDisconnected(int profile);
    }

    void setProxyListener(ProfileProxyListener listener);

    void notifyProxy();

    void onProxyStateChanged(boolean isConnect, IBluetoothPxr binder);

    void cleanup();
}
