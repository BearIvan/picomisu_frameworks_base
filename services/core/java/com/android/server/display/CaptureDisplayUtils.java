// Copyright 2026 Picomisu contributors
// SPDX-License-Identifier: Apache-2.0
package com.android.server.display;

import android.os.Binder;
import android.os.IBinder;
import android.os.SystemProperties;
import android.util.Slog;

/**
 * PICO capture display classification (factory PICO OS 5.13.7
 * com.android.server.display.CaptureDisplayUtils).
 *
 * <p>When the OpenXR runtime screen capture service is available
 * ({@link CaptureSurfaceAgent#isNewCaptureEnable}), a presentation virtual display (or a
 * Wi-Fi display with flag 64) named like a recording / casting / RTC / MRC display is not
 * composed by SurfaceFlinger: it gets a plain binder token and its surface is handed to the
 * runtime through {@link CaptureSurfaceAgent}.
 */
public class CaptureDisplayUtils {
    public static final int ADAPTER_TYPE_VIRTUAL = 1;
    public static final int ADAPTER_TYPE_WIFI = 0;
    public static final int CAPTURE_TYPE_CAPTURE = 1;
    public static final int CAPTURE_TYPE_CASTING = 3;
    public static final int CAPTURE_TYPE_DOUBLE_EYE_NON_DISTORTION = 5;
    public static final int CAPTURE_TYPE_INVALIDATE = -1;
    public static final int CAPTURE_TYPE_MRC = 4;
    public static final int CAPTURE_TYPE_RECORDING = 2;
    public static final int CAST_BUSINESS_MODE_DOUBLE = 1;
    public static final int CAST_BUSINESS_MODE_SINGLE = 0;
    public static final int CAST_MODE_DOUBLE = 0;
    public static final int CAST_MODE_SINGLE = 1;
    public static final String DISPLAY_NAME_CASTING = "ScreenCastThread-display";
    public static final String DISPLAY_NAME_MRC = "MrcDisplay";
    public static final String DISPLAY_NAME_NATIVESHELL = "ns_";
    public static final String DISPLAY_NAME_PICO_TV = "picotv";
    public static final String DISPLAY_NAME_RTC = "RTCScreenCapture";
    public static final String DISPLAY_NAME_SCRCPY = "scrcpy";
    public static final String DISPLAY_NAME_SCREENCAPTURE = "PvrScreenRecord";
    public static final String PROP_BUSINESS_CAST_MODE = "persist.pvr.cast.binocular";
    public static final String PROP_CAST_MODE = "persist.pxr.wfd.enable";
    public static final String TAG = "CaptureDisplayUtils";

    public static int getCaptureType(String name, int flags, int adapterType) {
        if (!isCaptureDisplay(name, flags, adapterType)) {
            Slog.d(TAG, "not a capture display: " + name);
            return CAPTURE_TYPE_INVALIDATE;
        }
        switch (name) {
            case DISPLAY_NAME_SCREENCAPTURE:
                return CAPTURE_TYPE_RECORDING;
            case DISPLAY_NAME_CASTING:
            case DISPLAY_NAME_RTC:
                return CAPTURE_TYPE_CASTING;
            case DISPLAY_NAME_MRC:
                return CAPTURE_TYPE_MRC;
            default:
                return CAPTURE_TYPE_RECORDING;
        }
    }

    public static IBinder generateCaptureDisplayToken() {
        return new CaptureDisplayToken();
    }

    private static boolean isCaptureDisplay(String name, int flags, int adapterType) {
        boolean ret;
        if (!isNewCaptureEnable() || name.contains(DISPLAY_NAME_PICO_TV)
                || name.equals(DISPLAY_NAME_SCRCPY)
                || name.toLowerCase().startsWith(DISPLAY_NAME_NATIVESHELL)) {
            return false;
        }
        if (getBussinessCastMode() == CAST_BUSINESS_MODE_DOUBLE && (flags & 16) == 16) {
            Slog.i(TAG, "enterprise with dual cast: " + name);
            return false;
        }
        if (getCastModeStatus() == CAST_MODE_DOUBLE && !isCastingDisplay(name)) {
            Slog.i(TAG, "double mode and not a casting display...");
            return false;
        }
        if (name.contains(DISPLAY_NAME_MRC)) {
            return true;
        }
        if (adapterType == ADAPTER_TYPE_WIFI) {
            ret = (flags & 64) == 64;
        } else if (adapterType == ADAPTER_TYPE_VIRTUAL) {
            ret = (flags & 2) == 2;
        } else {
            ret = false;
        }
        Slog.i(TAG, "has presentation? " + ret + " name: " + name);
        return ret;
    }

    private static boolean isCastingDisplay(String name) {
        return DISPLAY_NAME_CASTING.equals(name) || DISPLAY_NAME_RTC.equals(name)
                || DISPLAY_NAME_MRC.equals(name);
    }

    private static int getCastModeStatus() {
        return SystemProperties.getInt(PROP_CAST_MODE, CAST_MODE_SINGLE);
    }

    private static int getBussinessCastMode() {
        return SystemProperties.getInt(PROP_BUSINESS_CAST_MODE, CAST_BUSINESS_MODE_SINGLE);
    }

    private static boolean isNewCaptureEnable() {
        return CaptureSurfaceAgent.isNewCaptureEnable();
    }

    private static final class CaptureDisplayToken extends Binder {
        private CaptureDisplayToken() {
        }
    }
}
