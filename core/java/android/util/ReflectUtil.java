// Copyright 2026 Picomisu contributors
// SPDX-License-Identifier: Apache-2.0

package android.util;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * Smartisan reflection helpers (factory PICO OS 5.13.7 framework.jar android.util.ReflectUtil).
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

    public static Object invokeStaticMethod(String targetClassName, String methodName,
            Class[] paramTypes, Object[] paramObjects) {
        try {
            return invokeStaticMethod(Class.forName(targetClassName), methodName, paramTypes,
                    paramObjects);
        } catch (Throwable thr) {
            Log.e(TAG, "invokeMethod error!" + targetClassName + "." + methodName, thr);
        }
        return null;
    }

    public static Object invokeMethod(Object targetObject, String methodName, Class[] paramTypes,
            Object[] paramObjects) {
        try {
            Method targetMethod;
            if (paramTypes == null) {
                targetMethod = targetObject.getClass().getDeclaredMethod(methodName, new Class[0]);
            } else {
                targetMethod = targetObject.getClass().getDeclaredMethod(methodName, paramTypes);
            }
            targetMethod.setAccessible(true);
            if (paramObjects == null) {
                return targetMethod.invoke(targetObject, new Object[0]);
            }
            return targetMethod.invoke(targetObject, paramObjects);
        } catch (Throwable thr) {
            Log.e(TAG, "invokeMethod error!" + targetObject.getClass().getName() + "."
                    + methodName, thr);
        }
        return null;
    }

    public static Object acquireStaticField(Class<?> targetClass, String fieldName) {
        try {
            Field targetField = targetClass.getDeclaredField(fieldName);
            targetField.setAccessible(true);
            return targetField.get(null);
        } catch (Throwable thr) {
            Log.e(TAG, "acquireField error!" + targetClass.getName() + "." + fieldName, thr);
        }
        return null;
    }

    public static Object acquireStaticField(String targetClassName, String fieldName) {
        try {
            return acquireStaticField(Class.forName(targetClassName), fieldName);
        } catch (Throwable thr) {
            Log.e(TAG, "acquireField error!" + targetClassName + "." + fieldName, thr);
        }
        return null;
    }

    public static Object acquireField(Object targetObject, String fieldName) {
        try {
            Field targetField = targetObject.getClass().getDeclaredField(fieldName);
            targetField.setAccessible(true);
            return targetField.get(targetObject);
        } catch (Throwable thr) {
            Log.e(TAG, "acquireField error!" + targetObject.getClass().getName() + "."
                    + fieldName, thr);
        }
        return null;
    }

    public static Object createNewInstance(String targetClassName, Class[] paramTypes,
            Object[] paramObjects) {
        try {
            return Class.forName(targetClassName).getDeclaredConstructor(paramTypes)
                    .newInstance(paramObjects);
        } catch (ClassNotFoundException | IllegalAccessException | InstantiationException
                | NoSuchMethodException | InvocationTargetException e) {
            e.printStackTrace();
        }
        return null;
    }
}
