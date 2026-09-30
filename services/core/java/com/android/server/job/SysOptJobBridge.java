// Copyright 2026 Picomisu contributors
// SPDX-License-Identifier: Apache-2.0

package com.android.server.job;

import android.util.Slog;

import dalvik.system.BaseDexClassLoader;

/**
 * Loads the Smartisan job scheduler optimization factory
 * ({@code com.android.server.job.SysJobFactoryImpl}) from the optional sys services JAR and
 * falls back to the default implementations of {@link ISysJobFactory} when it is absent.
 * Reconstructed from the PICO OS 5.13.7 factory services.
 *
 * @hide
 */
public class SysOptJobBridge {
    private static String TAG = "SysOptJobBridge";
    private static String SYSJOB_CLASS_NAME = "com.android.server.job.SysJobFactoryImpl";
    private static ISysJobFactory sISysJobFactory;

    public static ISysJobFactory getFactory() {
        if (sISysJobFactory == null) {
            synchronized (ISysJobFactory.class) {
                if (sISysJobFactory == null) {
                    try {
                        BaseDexClassLoader loader =
                                (BaseDexClassLoader) SysOptJobBridge.class.getClassLoader();
                        sISysJobFactory = (ISysJobFactory) loader.loadClass(
                                SYSJOB_CLASS_NAME).newInstance();
                        Slog.i(TAG, "SysOptJobBridge ISysJobFactory: instance: "
                                + sISysJobFactory);
                    } catch (Exception e) {
                        sISysJobFactory = new ISysJobFactory() {};
                        Slog.e(TAG, "SysOptJobBridge ISysJobFactory getInstance error: "
                                + e.toString());
                    }
                }
            }
        }
        return sISysJobFactory;
    }
}
