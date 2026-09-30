// Copyright 2026 Picomisu contributors
// SPDX-License-Identifier: Apache-2.0

package com.android.server;

import android.util.Slog;

import dalvik.system.BaseDexClassLoader;

/**
 * Loads the Smartisan system service optimization factory
 * ({@code com.android.server.am.SysSvsFactoryImpl}) from the optional sys services JAR and
 * falls back to the default implementations of {@link ISysSvsFactory} when it is absent.
 * Reconstructed from the PICO OS 5.13.7 factory services; only the members reached by the
 * ported factory code are present.
 *
 * @hide
 */
public class SysOptBridge {
    private static String TAG = "SysOptBridge";
    private static String SYSOPT_COMMON_CLASS_NAME = "com.android.server.am.SysSvsFactoryImpl";
    private static String SYSOPT_MULTIPLAT_CLASS_NAME = "com.android.server.MultiPlatFactoryImpl";
    private static ISysSvsFactory sISysSvsFactory;
    private static IMultiPlatSvsFactory sIMultiPlatSvsFactory;

    public static ISysSvsFactory getFactory() {
        if (sISysSvsFactory == null) {
            synchronized (ISysSvsFactory.class) {
                if (sISysSvsFactory == null) {
                    try {
                        BaseDexClassLoader loader =
                                (BaseDexClassLoader) SysOptBridge.class.getClassLoader();
                        sISysSvsFactory = (ISysSvsFactory) loader.loadClass(
                                SYSOPT_COMMON_CLASS_NAME).newInstance();
                        Slog.i(TAG, "SysOptBridge ISysSvsFactory: instance: " + sISysSvsFactory);
                    } catch (Exception e) {
                        sISysSvsFactory = new ISysSvsFactory() {};
                        Slog.e(TAG, "SysOptBridge ISysSvsFactory getInstance error: "
                                + e.toString());
                    }
                }
            }
        }
        return sISysSvsFactory;
    }

    /**
     * Returns the multi-platform service factory, or null (as in the factory) when
     * {@code com.android.server.MultiPlatFactoryImpl} cannot be loaded.
     */
    public static IMultiPlatSvsFactory getMultiPlatFactory() {
        if (sIMultiPlatSvsFactory == null) {
            synchronized (IMultiPlatSvsFactory.class) {
                if (sIMultiPlatSvsFactory == null) {
                    try {
                        BaseDexClassLoader loader =
                                (BaseDexClassLoader) SysOptBridge.class.getClassLoader();
                        sIMultiPlatSvsFactory = (IMultiPlatSvsFactory) loader.loadClass(
                                SYSOPT_MULTIPLAT_CLASS_NAME).newInstance();
                        Slog.i(TAG, "SysOptBridge IMultiPlatSvsFactory: instance: "
                                + sIMultiPlatSvsFactory);
                    } catch (Exception e) {
                        Slog.e(TAG, "SysOptBridge IMultiPlatSvsFactory getInstance error: "
                                + e.toString());
                    }
                }
            }
        }
        return sIMultiPlatSvsFactory;
    }
}
