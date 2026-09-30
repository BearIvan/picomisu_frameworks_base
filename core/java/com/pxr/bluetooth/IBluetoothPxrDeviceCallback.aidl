// Copyright 2026 Picomisu contributors
// SPDX-License-Identifier: Apache-2.0
// Reconstructed from the factory PICO OS 5.13.7 DEX by tools/reconstruct-pico-aidl.py.
package com.pxr.bluetooth;

import com.pxr.bluetooth.BluetoothPxrDeviceProperty;

/** @hide */
interface IBluetoothPxrDeviceCallback {
    void onDeviceConnectionStateChanged(in BluetoothPxrDeviceProperty prop, int preState, int newState);
    void onDeviceBondStateChanged(in BluetoothPxrDeviceProperty prop, int preState, int newState);
}
