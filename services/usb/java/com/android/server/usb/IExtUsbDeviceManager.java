// Copyright 2026 Picomisu contributors
// SPDX-License-Identifier: Apache-2.0
package com.android.server.usb;

import android.content.Context;
import android.provider.Settings;
import android.util.Slog;

import com.pico.util.IExtBase;

import java.util.HashMap;
import java.util.Map;
import java.util.StringJoiner;

/**
 * PICO USB device manager extension (factory PICO OS 5.13.7
 * com.android.server.usb.IExtUsbDeviceManager): accessory mode requested through
 * {@link android.hardware.usb.IUsbManager#startAccessory()}, deferred function switches while an
 * accessory mode request is pending, and the helpers the legacy USB handler uses for that.
 * @hide
 */
public interface IExtUsbDeviceManager extends IExtBase {
    String CIT_ADB_CLEAR_KEY = "adb_clear";
    Map<String, Long> FUNCTION_NAME_TO_CODE = new HashMap<>();
    String TAG = "IExtUsbDeviceManager";

    void checkPreAccessoryMode(String functions, int msg);

    void init(UsbDeviceManager.UsbHandler handler, boolean hasUsbAccessory);

    boolean isSettingsCaller(int uid);

    boolean startAccessory();

    /**
     * Like {@link android.hardware.usb.UsbManager#usbFunctionsFromString(String)}, but logs and
     * skips unknown functions instead of throwing.
     */
    static long usbFunctionsFromString(String functions) {
        long ret = 0;
        if (functions == null || functions.equals("none")) {
            return ret;
        }
        for (String function : functions.split(",")) {
            if (FUNCTION_NAME_TO_CODE.containsKey(function)) {
                ret |= FUNCTION_NAME_TO_CODE.get(function);
            } else if (function.length() > 0) {
                Slog.e(TAG, "usbFunctionsFromString error functions:" + functions);
            }
        }
        Slog.w(TAG, "usbFunctionsFromString ret:" + ret);
        return ret;
    }

    /** Turns adb off once if the CIT (factory test) app asked for it through adb_clear. */
    static void checkCitAdbClose(Context context) {
        int value = Settings.Global.getInt(context.getContentResolver(), CIT_ADB_CLEAR_KEY, 0);
        if (value == 1) {
            Settings.Global.putInt(context.getContentResolver(), CIT_ADB_CLEAR_KEY, 0);
            Settings.Global.putInt(context.getContentResolver(), Settings.Global.ADB_ENABLED, 0);
        }
    }

    static boolean containsFunction(String functions, String function) {
        int index = functions.indexOf(function);
        if (index < 0) return false;
        if (index > 0 && functions.charAt(index - 1) != ',') return false;
        int charAfter = index + function.length();
        if (charAfter < functions.length() && functions.charAt(charAfter) != ',') return false;
        return true;
    }

    /** Puts the functions in the order the gadget configuration expects. */
    static String adjustModeOrder(String functions) {
        StringJoiner joiner = new StringJoiner(",");
        if (functions.contains("mtp")) {
            joiner.add("mtp");
        }
        if (functions.contains("ptp")) {
            joiner.add("ptp");
        }
        if (functions.contains("diag")) {
            joiner.add("diag");
        }
        if (functions.contains("rndis")) {
            joiner.add("rndis");
        }
        if (functions.contains("midi")) {
            joiner.add("midi");
        }
        if (functions.contains("accessory")) {
            joiner.add("accessory");
        }
        if (functions.contains("audio_source")) {
            joiner.add("audio_source");
        }
        if (functions.contains("adb")) {
            joiner.add("adb");
        }
        return joiner.toString();
    }
}
