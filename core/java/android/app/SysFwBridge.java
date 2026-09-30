// Copyright 2026 Picomisu contributors
// SPDX-License-Identifier: Apache-2.0

package android.app;

import android.util.Slog;

/**
 * Loads the Smartisan framework extension factory from the optional sys framework JAR and falls
 * back to the default implementations of {@link ISysFwFactory} when it is absent.
 * Reconstructed from the PICO OS 5.13.7 factory framework.
 *
 * @hide
 */
public class SysFwBridge {
    private static String TAG = "SysFwBridge";
    private static String CLASS_NAME = "android.app.SysFwFactoryImpl";
    protected static ISysFwFactory sInstance;

    public static ISysFwFactory getFactory() {
        if (sInstance == null) {
            try {
                ClassLoader classLoader = ClassLoader.getSystemClassLoader();
                if (classLoader != null) {
                    sInstance = (ISysFwFactory) classLoader.loadClass(CLASS_NAME).newInstance();
                    Slog.i(TAG, "sInstance: = " + sInstance);
                }
            } catch (Exception e) {
                Slog.e(TAG, "getInstance error: " + e.toString());
                sInstance = new ISysFwFactory() {};
            }
        }
        return sInstance;
    }
}
