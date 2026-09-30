// Copyright 2026 Picomisu contributors
// SPDX-License-Identifier: Apache-2.0

package com.android.server.am;

import android.util.Slog;

import com.android.server.ISysMonitorSvcFactory;

import dalvik.system.BaseDexClassLoader;

/**
 * Loads the Smartisan system monitor service factory
 * ({@code com.android.server.am.SysMonitorSvcFactoryImpl}) from the optional sysmonitor
 * services JAR and falls back to the default implementations of {@link ISysMonitorSvcFactory}
 * when it is absent. Reconstructed from the PICO OS 5.13.7 factory services.
 *
 * @hide
 */
public class SysMonitorSvcBridge {
    private static String TAG = "SysMonitorSvcBridge";
    private static String SYSOPT_COMMON_CLASS_NAME =
            "com.android.server.am.SysMonitorSvcFactoryImpl";
    private static ISysMonitorSvcFactory sISysMonitorSvcFactory;

    public static ISysMonitorSvcFactory getFactory() {
        if (sISysMonitorSvcFactory == null) {
            synchronized (ISysMonitorSvcFactory.class) {
                if (sISysMonitorSvcFactory == null) {
                    try {
                        BaseDexClassLoader loader =
                                (BaseDexClassLoader) SysMonitorSvcBridge.class.getClassLoader();
                        sISysMonitorSvcFactory = (ISysMonitorSvcFactory) loader.loadClass(
                                SYSOPT_COMMON_CLASS_NAME).newInstance();
                        Slog.i(TAG, "SysMonitorSvcBridge ISysMonitorSvcFactory: instance: "
                                + sISysMonitorSvcFactory);
                    } catch (Exception e) {
                        sISysMonitorSvcFactory = new ISysMonitorSvcFactory() {};
                        Slog.e(TAG, "SysMonitorSvcBridge ISysMonitorSvcFactory getInstance error: "
                                + e.toString());
                    }
                }
            }
        }
        return sISysMonitorSvcFactory;
    }
}
