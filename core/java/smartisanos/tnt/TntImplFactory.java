// Copyright 2026 Picomisu contributors
// SPDX-License-Identifier: Apache-2.0

package smartisanos.tnt;

import android.app.ActivityThread;
import android.util.ReflectUtil;
import android.util.Slog;

import java.lang.reflect.Constructor;
import java.util.HashMap;

/**
 * Resolves and instantiates the implementations of the Smartisan {@link ITntBase} interfaces:
 * by naming convention ({@code IApiFoo} -> {@code ApiFooImpl}) or from the generated
 * {@code smartisanos.tnt.TntImplCollectorCore}/{@code TntImplCollectorServices} lists.
 * Reconstructed from the PICO OS 5.13.7 factory framework.
 *
 * @hide
 */
public final class TntImplFactory {
    private static final String TAG = "TntImplFactory";

    private static HashMap<Class, Class> sTntImpls = new HashMap<>();

    private static boolean sFrameworkImplLoaded = false;
    private static boolean sServiceImplLoaded = false;

    private static void tryToLoadAnnotatedImpl(ClassLoader classLoader) {
        if (sFrameworkImplLoaded && sServiceImplLoaded) {
            return;
        }
        if (ActivityThread.currentActivityThread() != null && !ActivityThread.isSystem()) {
            sServiceImplLoaded = true;
        }
        if (!sFrameworkImplLoaded) {
            try {
                Class collectorClass =
                        classLoader.loadClass("smartisanos.tnt.TntImplCollectorCore");
                HashMap<Class, Class> res = (HashMap<Class, Class>) ReflectUtil
                        .invokeStaticMethod(collectorClass, "initImplList", null, null);

                sTntImpls.putAll(res);
                sFrameworkImplLoaded = true;
            } catch (Throwable thr) {
                Slog.e(TAG, "init core impls error!", thr);
            }
        }
        if (!sServiceImplLoaded) {
            try {
                if (ActivityThread.isSystem()) {
                    Class collectorClass =
                            classLoader.loadClass("smartisanos.tnt.TntImplCollectorServices");
                    HashMap<Class, Class> res = (HashMap<Class, Class>) ReflectUtil
                            .invokeStaticMethod(collectorClass, "initImplList", null, null);

                    sTntImpls.putAll(res);
                    sServiceImplLoaded = true;
                }
            } catch (Throwable thr) {
                Slog.e(TAG, "init service impls error!", thr);
            }
        }
    }

    private static void put(Class base, Class impl) {
        sTntImpls.put(base, impl);
    }

    static Class get(Class<? extends ITntBase> tntInterface) {
        Class implClass = sTntImpls.get(tntInterface);
        if (implClass == null) {
            Class enclosingClass = tntInterface.getEnclosingClass();
            String implClassName;
            if (enclosingClass == null) {
                implClassName = tntInterface.getName().replace("IApi", "Api") + "Impl";
            } else {
                implClassName = enclosingClass.getName().replace("IApi", "Api") + "Impl$"
                        + tntInterface.getSimpleName().replace("IApi", "Api") + "Impl";
            }
            try {
                implClass = tntInterface.getClassLoader().loadClass(implClassName);
            } catch (ClassNotFoundException e) {
                Slog.w(TAG, "load impl class failed for " + tntInterface.getName(), e);
            }
            if (implClass == null) {
                tryToLoadAnnotatedImpl(tntInterface.getClassLoader());
                implClass = sTntImpls.get(tntInterface);
            }
            if (implClass == null) {
                throw new RuntimeException("could not find impl class for "
                        + tntInterface.toGenericString());
            }
            put(tntInterface, implClass);
        }
        return implClass;
    }

    public static <T extends ITntBase> T createImpl(Class<T> tntInterface, Object... objects) {
        Class implClass = get(tntInterface);

        Constructor[] constructors = implClass.getDeclaredConstructors();

        if (objects == null || objects.length == 0) {
            try {
                return (T) implClass.newInstance();
            } catch (Throwable thr) {
                Slog.e(TAG, "create new impl instance error! impl is " + implClass.getName(),
                        thr);
                throw new RuntimeException(thr);
            }
        }

        if (constructors.length == 1) {
            try {
                constructors[0].setAccessible(true);
                return (T) constructors[0].newInstance(objects);
            } catch (Throwable thr) {
                Slog.e(TAG, "call impl constructor error! impl is " + implClass.getName(), thr);
                throw new RuntimeException(thr);
            }
        }

        outer:
        for (Constructor constructor : constructors) {
            Class[] params = constructor.getParameterTypes();
            if (params.length != objects.length) {
                continue;
            }
            for (int i = 0; i < params.length; i++) {
                if (!params[i].isInstance(objects[i])) {
                    continue outer;
                }
            }

            try {
                constructor.setAccessible(true);
                return (T) constructor.newInstance(objects);
            } catch (Throwable thr) {
                Slog.e(TAG, "call matched impl constructor error! impl is "
                        + implClass.getName(), thr);
                throw new RuntimeException(thr);
            }
        }

        throw new RuntimeException("constructor match failed for " + implClass.getName());
    }
}
