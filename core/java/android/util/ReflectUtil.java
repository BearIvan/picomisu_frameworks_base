// Copyright 2026 Picomisu contributors
// SPDX-License-Identifier: Apache-2.0

package android.util;

import java.lang.reflect.Method;

/**
 * Smartisan reflection helpers. Reconstructed from the PICO OS 5.13.7 factory framework; only
 * the members reached by the ported factory code are present.
 *
 * @hide
 */
public final class ReflectUtil {
    private static final String TAG = "ReflectUtil";

    private ReflectUtil() {
    }

    public static Object invokeStaticMethod(Class<?> targetClass, String methodName,
            Class[] paramTypes, Object[] paramObjects) {
        try {
            Method targetMethod;
            if (paramTypes == null) {
                targetMethod = targetClass.getDeclaredMethod(methodName, new Class[0]);
            } else {
                targetMethod = targetClass.getDeclaredMethod(methodName, paramTypes);
            }
            targetMethod.setAccessible(true);
            return targetMethod.invoke(null, paramObjects);
        } catch (Throwable thr) {
            Log.e(TAG, "invokeMethod error!" + targetClass.getName() + "." + methodName, thr);
        }
        return null;
    }
}
