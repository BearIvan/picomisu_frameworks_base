// Copyright 2026 Picomisu contributors
// SPDX-License-Identifier: Apache-2.0
// Reconstructed from the factory PICO OS 5.13.7 DEX by tools/reconstruct-pico-aidl.py.
package com.pxr.bluetooth;

import com.pxr.bluetooth.BluetoothPxrDeviceProperty;
import com.pxr.bluetooth.IBluetoothPxrDeviceCallback;

/** @hide */
interface IBluetoothPxr {
    void registerDeviceStateCallback(IBluetoothPxrDeviceCallback cb);
    BluetoothPxrDeviceProperty[] getConnectedDevices();
}
