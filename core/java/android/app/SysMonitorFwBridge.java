// Copyright 2026 Picomisu contributors
// SPDX-License-Identifier: Apache-2.0

package android.app;

import android.util.Slog;

/**
 * Loads the Smartisan sysmonitor framework extension factory from the optional sysmonitor
 * framework JAR and falls back to the default implementations of {@link ISysMonitorFwFactory}
 * when it is absent. Reconstructed from the PICO OS 5.13.7 factory framework.
 *
 * @hide
 */
public class SysMonitorFwBridge {
    private static String TAG = "SysMonitorFwBridge";
    private static String CLASS_NAME = "android.app.SysMonitorFwFactoryImpl";
    private static ISysMonitorFwFactory sISysMonitorFwFactory;

    public static ISysMonitorFwFactory getFactory() {
        if (sISysMonitorFwFactory == null) {
            synchronized (ISysMonitorFwFactory.class) {
                if (sISysMonitorFwFactory == null) {
                    try {
                        ClassLoader classLoader = ClassLoader.getSystemClassLoader();
                        if (classLoader != null) {
                            sISysMonitorFwFactory = (ISysMonitorFwFactory) classLoader.loadClass(
                                    CLASS_NAME).newInstance();
                            Slog.i(TAG, "sISysMonitorFwFactory = " + sISysMonitorFwFactory);
                        }
                    } catch (Exception e) {
                        sISysMonitorFwFactory = new ISysMonitorFwFactory() {};
                        Slog.e(TAG, "getInstance error = " + e.toString());
                    }
                }
            }
        }
        return sISysMonitorFwFactory;
    }
}
