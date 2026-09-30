// Copyright 2026 Picomisu contributors
// SPDX-License-Identifier: Apache-2.0

package smartisanos.tnt;

import android.util.Slog;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Iterator;
import java.util.LinkedList;

/**
 * Base of the Smartisan "tnt" API interfaces whose implementations live in the optional
 * (vrex/sys) JARs and are resolved by {@link TntImplFactory}. Reconstructed from the PICO OS
 * 5.13.7 factory framework.
 *
 * @hide
 */
public interface ITntBase {
    /**
     * Calls the static method of the implementation of {@code tntInterface} that has the name of
     * the calling method and matches {@code objects}.
     */
    static Object callImplStaticMethod(Class<? extends ITntBase> tntInterface,
            Object... objects) {
        StackTraceElement element = Thread.currentThread().getStackTrace()[3];
        String methodName = element.getMethodName();

        try {
            Class implClass = TntImplFactory.get(tntInterface);
            Method[] methods = implClass.getDeclaredMethods();
            LinkedList<Method> matchedMethods = new LinkedList<>();
            for (Method method : methods) {
                if ((method.getModifiers() & Modifier.STATIC) == 0) {
                    continue;
                }
                if (method.getName().equals(methodName)) {
                    method.setAccessible(true);
                    matchedMethods.add(method);
                }
            }

            if (matchedMethods.size() == 0) {
                throw new RuntimeException("method not found!");
            }

            if (matchedMethods.size() == 1) {
                return matchedMethods.get(0).invoke(null, objects);
            }

            Iterator<Method> methodIterator = matchedMethods.iterator();
            while (methodIterator.hasNext()) {
                Method method = methodIterator.next();
                if (method.getParameterCount() != objects.length) {
                    methodIterator.remove();
                }
            }

            if (matchedMethods.size() == 0) {
                throw new RuntimeException("method not found, param count not match!");
            }

            if (matchedMethods.size() == 1) {
                return matchedMethods.get(0).invoke(null, objects);
            }

            outer:
            for (Method method : matchedMethods) {
                Class[] params = method.getParameterTypes();
                for (int i = 0; i < params.length; i++) {
                    if (!params[i].isInstance(objects[i])
                            && !(params[i].isPrimitive()
                                    && isPrimitiveTypeMatch(params[i], objects[i]))) {
                        continue outer;
                    }
                }

                try {
                    return method.invoke(null, objects);
                } catch (Throwable thr) {
                    Slog.e("ITntBase", "call matched impl constructor error! impl is "
                            + implClass.getName(), thr);
                    throw new RuntimeException(thr);
                }
            }

            return null;
        } catch (Throwable thr) {
            throw new RuntimeException("callImplStaticMethod error! method is "
                    + tntInterface.getName() + "." + methodName, thr);
        }
    }

    static boolean isPrimitiveTypeMatch(Class primitiveClass, Object object) {
        if (object == null) {
            return false;
        }

        try {
            return object.getClass().getDeclaredField("TYPE").get(null) == primitiveClass;
        } catch (Exception e) {
        }

        return false;
    }
}
