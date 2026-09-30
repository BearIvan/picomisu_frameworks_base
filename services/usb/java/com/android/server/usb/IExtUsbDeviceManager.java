// Copyright 2026 Picomisu contributors
// SPDX-License-Identifier: Apache-2.0
package com.android.server.usb;

import com.pico.util.IExtBase;

/**
 * PICO USB device manager extension: accessory mode requested through
 * {@link android.hardware.usb.IUsbManager#startAccessory()}.
 * @hide
 */
public interface IExtUsbDeviceManager extends IExtBase {
    String TAG = "IExtUsbDeviceManager";

    void init(UsbDeviceManager.UsbHandler handler, boolean hasUsbAccessory);
    boolean startAccessory();
}
