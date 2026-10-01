// Copyright 2026 Picomisu contributors
// SPDX-License-Identifier: Apache-2.0
package com.android.server.usb;

import android.hardware.usb.UsbManager;
import android.os.SystemProperties;
import android.util.Slog;

/**
 * PICO USB device manager extension (factory PICO OS 5.13.7
 * com.android.server.usb.ExtUsbDeviceManagerImpl).
 * @hide
 */
public class ExtUsbDeviceManagerImpl implements IExtUsbDeviceManager {
    private static final boolean DEBUG = true;
    private static final int MSG_ACCESSORY_MODE_ENTER_TIMEOUT = 8;
    private static boolean mHasUsbAccessory;
    private UsbDeviceManager.UsbHandler mHandler;
    private UsbDeviceManager mUsbDeviceManager;

    static {
        FUNCTION_NAME_TO_CODE.put(UsbManager.USB_FUNCTION_MTP, UsbManager.FUNCTION_MTP);
        FUNCTION_NAME_TO_CODE.put(UsbManager.USB_FUNCTION_PTP, UsbManager.FUNCTION_PTP);
        FUNCTION_NAME_TO_CODE.put(UsbManager.USB_FUNCTION_RNDIS, UsbManager.FUNCTION_RNDIS);
        FUNCTION_NAME_TO_CODE.put(UsbManager.USB_FUNCTION_MIDI, UsbManager.FUNCTION_MIDI);
        FUNCTION_NAME_TO_CODE.put(UsbManager.USB_FUNCTION_ACCESSORY,
                UsbManager.FUNCTION_ACCESSORY);
        FUNCTION_NAME_TO_CODE.put(UsbManager.USB_FUNCTION_AUDIO_SOURCE,
                UsbManager.FUNCTION_AUDIO_SOURCE);
        FUNCTION_NAME_TO_CODE.put(UsbManager.USB_FUNCTION_ADB, UsbManager.FUNCTION_ADB);
    }

    public ExtUsbDeviceManagerImpl(UsbDeviceManager usbDeviceManager) {
        mUsbDeviceManager = usbDeviceManager;
    }

    @Override
    public void init(UsbDeviceManager.UsbHandler handler, boolean hasUsbAccessory) {
        mHandler = handler;
        mHasUsbAccessory = hasUsbAccessory;
    }

    @Override
    public void checkPreAccessoryMode(String functions, int msg) {
        if (mHandler.hasMessages(msg)) {
            Slog.w(TAG, "checkPreAccessoryMode exit accessory");
            mHandler.removeMessages(msg);
            mHandler.notifyAccessoryModeExit();
        }
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

    /** Whether a request for charging-only functions comes from the system (Settings). */
    @Override
    public boolean isSettingsCaller(int uid) {
        Slog.i(TAG, "setCurrentFunctions callerUid = " + uid);
        return uid == android.os.Process.SYSTEM_UID;
    }

    /** Per-handler part of the extension (factory ExtUsbDeviceManagerImpl$UsbHandlerExt). */
    static class UsbHandlerExt {
        private UsbDeviceManager.UsbHandler mUsbHandler;
        protected long mUsbModeNext = UsbManager.FUNCTION_NONE;
        protected boolean mHasNextMode = false;

        public UsbHandlerExt(UsbDeviceManager.UsbHandler handler) {
            mUsbHandler = handler;
        }

        /** Rewrites the persistent USB config with the current adb state (setAdbEnabled). */
        public void setSystemProperties() {
            String newFunction = "";
            if (this instanceof UsbHandlerLegacyExt) {
                newFunction = ((UsbDeviceManager.UsbHandlerLegacy) mUsbHandler).applyAdbFunction(
                        SystemProperties.get(UsbDeviceManager.UsbHandler
                                .USB_PERSISTENT_CONFIG_PROPERTY, UsbManager.USB_FUNCTION_NONE));
            } else {
                newFunction = SystemProperties.get(
                        UsbDeviceManager.UsbHandler.USB_PERSISTENT_CONFIG_PROPERTY,
                        UsbManager.USB_FUNCTION_NONE);
            }
            Slog.v(TAG, "setAdbEnabled,newFunction:" + newFunction);
            SystemProperties.set(UsbDeviceManager.UsbHandler.USB_PERSISTENT_CONFIG_PROPERTY,
                    newFunction);
            SystemProperties.set("persist.vendor.usb.config", newFunction);
        }

        /** Accessory attached: applies the functions that were requested meanwhile. */
        void removeAccessoryEnterTimeout() {
            mUsbHandler.removeMessages(MSG_ACCESSORY_MODE_ENTER_TIMEOUT);
            if (mHasNextMode) {
                Slog.w(TAG, "set delay usb mode: " + mUsbModeNext);
                mHasNextMode = false;
                mUsbHandler.setEnabledFunctions(mUsbModeNext, false);
            }
        }

        /** Connected with charging-only functions: applies the persistent USB config. */
        public void updateEnabledFunctions(boolean connected, long currentFunctions) {
            Slog.w(TAG, "updateEnabledFunctions connected:" + connected + " currentFunctions:"
                    + currentFunctions);
            if (connected && currentFunctions == UsbManager.FUNCTION_NONE) {
                String persistFunctions = mUsbHandler.getSystemProperty(
                        UsbDeviceManager.UsbHandler.USB_PERSISTENT_CONFIG_PROPERTY,
                        UsbManager.USB_FUNCTION_NONE);
                Slog.w(TAG, "persistFunctions:" + persistFunctions);
                try {
                    mUsbHandler.setEnabledFunctions(
                            UsbManager.usbFunctionsFromString(persistFunctions), false);
                } catch (IllegalArgumentException e) {
                    Slog.w(TAG, "IllegalArgumentException occurred when usbFunctionsFromString.",
                            e);
                }
            }
        }
    }

    /** Legacy (sys.usb.config) handler part (factory ExtUsbDeviceManagerImpl$UsbHandlerLegacyExt). */
    static final class UsbHandlerLegacyExt extends UsbHandlerExt {
        private UsbDeviceManager.UsbHandlerLegacy mUsbHandlerLegacy;

        UsbHandlerLegacyExt(UsbDeviceManager.UsbHandler legacy) {
            super(legacy);
            mUsbHandlerLegacy = (UsbDeviceManager.UsbHandlerLegacy) legacy;
        }

        /**
         * Adds the accessory function (and arms the accessory mode timeout) when the device
         * supports accessories and the functions are mtp, diag or charging only.
         */
        private String applyAccessoryFunction(String functions, int msg, int timeOut,
                boolean adbEnable) {
            Slog.w(TAG, "applyAccessoryFunction, functions is " + functions
                    + ", mHasUsbAccessory is " + mHasUsbAccessory + ", adbEnable is " + adbEnable);
            if (mHasUsbAccessory) {
                if (mUsbHandlerLegacy.containsFunction(functions, UsbManager.USB_FUNCTION_MTP)
                        || mUsbHandlerLegacy.containsFunction(functions, "diag")
                        || (adbEnable ? functions.equals(UsbManager.USB_FUNCTION_ADB)
                                : functions.equals(UsbManager.USB_FUNCTION_NONE))) {
                    functions = UsbDeviceManager.UsbHandlerLegacy.addFunction(functions,
                            UsbManager.USB_FUNCTION_ACCESSORY);
                    mUsbHandlerLegacy.sendMessageDelayed(mUsbHandlerLegacy.obtainMessage(msg),
                            timeOut);
                }
            }
            return functions;
        }

        String updateFunctions(String functions, int msgAccessoryModeEnterTimeout,
                int accessoryRequestTimeout, boolean adbEnabled, boolean isSettingsSetNone) {
            if (isSettingsSetNone) {
                functions = UsbDeviceManager.UsbHandlerLegacy.removeFunction(functions,
                        UsbManager.USB_FUNCTION_MTP);
                Slog.i(TAG, "trySetEnabledFunctions removeFunction mtp, functions = "
                        + functions);
            }
            functions = applyAccessoryFunction(functions, msgAccessoryModeEnterTimeout,
                    accessoryRequestTimeout, adbEnabled);
            Slog.w(TAG, "updateFunctions before mUsbHandlerLegacy.mCurrentFunctions "
                    + mUsbHandlerLegacy.mCurrentFunctions);
            mUsbHandlerLegacy.mCurrentFunctions =
                    IExtUsbDeviceManager.usbFunctionsFromString(functions);
            return IExtUsbDeviceManager.adjustModeOrder(functions);
        }

        /** Defers a function switch while an accessory mode request is pending. */
        public boolean hasAccessoryEnterTimeOutMessage(long usbFunctions) {
            if (mUsbHandlerLegacy.hasMessages(MSG_ACCESSORY_MODE_ENTER_TIMEOUT)) {
                Slog.w(TAG, "setEnabledFunctions pre accessory don't completed ,functions="
                        + usbFunctions);
                mHasNextMode = true;
                mUsbModeNext = usbFunctions;
                return true;
            }
            return false;
        }
    }
}
