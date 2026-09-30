// Copyright 2026 Picomisu contributors
// SPDX-License-Identifier: Apache-2.0
package com.android.server.usb;

import android.util.Slog;

/**
 * PICO USB device manager extension.
 * @hide
 */
public class ExtUsbDeviceManagerImpl implements IExtUsbDeviceManager {
    private static final int MSG_ACCESSORY_MODE_ENTER_TIMEOUT = 8;
    private static boolean mHasUsbAccessory;
    private UsbDeviceManager.UsbHandler mHandler;
    private UsbDeviceManager mUsbDeviceManager;

    public ExtUsbDeviceManagerImpl(UsbDeviceManager usbDeviceManager) {
        mUsbDeviceManager = usbDeviceManager;
    }

    @Override
    public void init(UsbDeviceManager.UsbHandler handler, boolean hasUsbAccessory) {
        mHandler = handler;
        mHasUsbAccessory = hasUsbAccessory;
    }

    /**
     * Enters accessory mode unless the device has no USB accessory support or an accessory
     * mode request is still pending.
     */
    @Override
    public boolean startAccessory() {
        Slog.i(TAG, "startAccessory");
        if (mHasUsbAccessory && !mHandler.hasMessages(MSG_ACCESSORY_MODE_ENTER_TIMEOUT)) {
            mUsbDeviceManager.startAccessoryMode();
            return true;
        }
        Slog.w(TAG, "startAccessory fail mHasUsbAccessory: " + mHasUsbAccessory);
        return false;
    }
}
