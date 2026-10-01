// Copyright 2026 Picomisu contributors
// SPDX-License-Identifier: Apache-2.0

package com.pico.util;

import android.util.Slog;

import java.lang.reflect.Constructor;

/**
 * Creates the implementation of a PICO extension interface, as in the PICO OS 5.13.7 factory
 * framework: the implementation of {@code a.b.IExtFoo} is {@code a.b.ExtFooImpl} (of a nested
 * {@code Outer.IExtFoo}, {@code OuterImpl$ExtFooImpl}), loaded with the class loader of the
 * interface and constructed with the given arguments.
 *
 * @hide
 */
public class ExtImplFactory {
    private static final String TAG = "ExtImplFactory";

    static Class get(Class<? extends IExtBase> extInterface) {
        String implClassName;
        Class<?> enclosingClass = extInterface.getEnclosingClass();
        if (enclosingClass == null) {
            implClassName = extInterface.getName().replace("IExt", "Ext") + "Impl";
        } else {
            implClassName = enclosingClass.getName().replace("IExt", "Ext") + "Impl$"
                    + extInterface.getSimpleName().replace("IExt", "Ext") + "Impl";
        }
        Class<?> implClass = null;
        try {
            implClass = extInterface.getClassLoader().loadClass(implClassName);
        } catch (ClassNotFoundException e) {
            Slog.w(TAG, "load impl class failed for " + extInterface.getName(), e);
        }
        if (implClass == null) {
            throw new RuntimeException("could not find impl class for "
                    + extInterface.toGenericString());
        }
        return implClass;
    }

    public static <T extends IExtBase> T getImpl(Class<T> extInterface, Object... objects) {
        Class implClass = get(extInterface);
        Constructor<?>[] declaredConstructors = implClass.getDeclaredConstructors();
        if (objects == null || objects.length == 0) {
            try {
                return (T) implClass.newInstance();
            } catch (Throwable t) {
                Slog.e(TAG, "create new impl instance error! impl is " + implClass.getName(), t);
                throw new RuntimeException(t);
            }
        }
        if (declaredConstructors.length == 1) {
            try {
                declaredConstructors[0].setAccessible(true);
                return (T) declaredConstructors[0].newInstance(objects);
            } catch (Throwable t) {
                Slog.e(TAG, "call impl constructor error! impl is " + implClass.getName(), t);
                throw new RuntimeException(t);
            }
        }
        nextConstructor:
        for (Constructor<?> constructor : declaredConstructors) {
            Class<?>[] parameterTypes = constructor.getParameterTypes();
            if (parameterTypes.length != objects.length) {
                continue;
            }
            for (int i = 0; i < parameterTypes.length; i++) {
                if (!parameterTypes[i].isInstance(objects[i])) {
                    continue nextConstructor;
                }
            }
            try {
                constructor.setAccessible(true);
                return (T) constructor.newInstance(objects);
            } catch (Throwable t) {
                Slog.e(TAG, "call matched impl constructor error! impl is "
                        + implClass.getName(), t);
                throw new RuntimeException(t);
            }
        }
        throw new RuntimeException("constructor match failed for " + implClass.getName());
    }
}
