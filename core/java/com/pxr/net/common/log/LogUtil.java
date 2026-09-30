// Copyright 2026 Picomisu contributors
// SPDX-License-Identifier: Apache-2.0

package com.pxr.net.common.log;

import android.util.Log;

/**
 * Logging helpers for the PICO Wi-Fi/network stack. All messages go to the "PxrWifi" tag,
 * optionally prefixed with a "[requestId] " marker.
 *
 * @hide
 */
public class LogUtil {
    private static final String TAG = "PxrWifi";

    public static void i(String requestId, String message) {
        Log.i(TAG, "[" + requestId + "] " + message);
    }

    public static void i(String message) {
        Log.i(TAG, message);
    }

    public static void d(String requestId, String message) {
        Log.d(TAG, "[" + requestId + "] " + message);
    }

    public static void d(String message) {
        Log.d(TAG, message);
    }

    public static void v(String requestId, String message) {
        Log.v(TAG, "[" + requestId + "] " + message);
    }

    public static void v(String message) {
        Log.v(TAG, message);
    }

    public static void e(String requestId, String message, Throwable t) {
        Log.e(TAG, "[" + requestId + "] " + message, t);
    }

    public static void e(String message, Throwable t) {
        Log.e(TAG, message, t);
    }

    public static void e(String requestId, String message) {
        Log.e(TAG, "[" + requestId + "] " + message);
    }

    public static void e(String message) {
        Log.e(TAG, message);
    }

    public static void w(String requestId, String message, Throwable t) {
        Log.w(TAG, "[" + requestId + "] " + message, t);
    }

    public static void w(String message, Throwable t) {
        Log.w(TAG, message, t);
    }

    public static void w(String requestId, String message) {
        Log.w(TAG, "[" + requestId + "] " + message);
    }

    public static void w(String message) {
        Log.w(TAG, message);
    }

    public static boolean isLoggable(int logLevel) {
        return Log.isLoggable(TAG, logLevel);
    }
}
