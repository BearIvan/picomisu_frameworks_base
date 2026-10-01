// Copyright 2026 Picomisu contributors
// SPDX-License-Identifier: Apache-2.0

package smartisanos.util;

import android.os.Build;

import java.util.HashSet;
import java.util.Set;

/**
 * Smartisan phone models, detected from the build board/device
 * (factory PICO OS 5.13.7 {@code smartisanos.util.DeviceType}; {@link #THIS} is
 * {@link #UNKNOWN} on the headset).
 *
 * @hide
 */
public enum DeviceType {
    T1,
    T2,
    U1,
    M1,
    M1L,
    A1,
    BONO,
    ODIN,
    OSBORN,
    OSCAR,
    TRIDENT,
    OCEAN,
    DELTA,
    ATOLL,
    DARWIN,
    UNKNOWN;

    private static Set<DeviceType> SMARTKEY_DEVICE = new HashSet<>();

    static {
        SMARTKEY_DEVICE.add(OSBORN);
        SMARTKEY_DEVICE.add(OSCAR);
        SMARTKEY_DEVICE.add(TRIDENT);
        SMARTKEY_DEVICE.add(OCEAN);
        SMARTKEY_DEVICE.add(DELTA);
        SMARTKEY_DEVICE.add(ATOLL);
        SMARTKEY_DEVICE.add(DARWIN);
    }

    public static final DeviceType THIS = getDeviceType();

    public static boolean is(DeviceType target) {
        return THIS == target;
    }

    public static boolean isOneOf(DeviceType... deviceList) {
        if (deviceList == null || deviceList.length == 0) {
            return false;
        }
        for (DeviceType type : deviceList) {
            if (is(type)) {
                return true;
            }
        }
        return false;
    }

    private static DeviceType getDeviceType() {
        DeviceType type;
        String board = Build.BOARD.toLowerCase();
        String device = Build.DEVICE.toLowerCase();
        if ("msm8974".equals(board)) {
            type = T1;
        } else if ("msm8916".equals(board)) {
            type = U1;
        } else if ("msm8992".equals(board)) {
            type = T2;
        } else if ("msm8996".equals(board)) {
            if ("colombo".equals(device)) {
                type = M1L;
            } else {
                type = M1;
            }
        } else if ("msm8952".equals(board)) {
            type = A1;
        } else if ("mt6797".equals(board) || "s10".equals(device)) {
            type = BONO;
        } else if (device.contains("oscar")) {
            type = OSCAR;
        } else if (device.contains("ocean") || device.contains("aries")
                || "sdm710".equals(board)) {
            type = OCEAN;
        } else if ("sdm660".equals(board) || "osborn".equals(device)) {
            type = OSBORN;
        } else if ("msm8953".equals(board) || "odin".equals(device)) {
            type = ODIN;
        } else if (device.contains("trident")) {
            type = TRIDENT;
        } else if (device.contains("delta")) {
            type = DELTA;
        } else if (device.contains("atoll")) {
            type = ATOLL;
        } else if (device.contains("darwin")) {
            type = DARWIN;
        } else {
            type = UNKNOWN;
        }
        return type;
    }

    public static boolean isSmartKeyProduct() {
        return SMARTKEY_DEVICE.contains(THIS);
    }
}
