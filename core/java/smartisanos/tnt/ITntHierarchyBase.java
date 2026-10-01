// Copyright 2026 Picomisu contributors
// SPDX-License-Identifier: Apache-2.0

package smartisanos.tnt;

import java.util.HashMap;

/**
 * TNT extension interface resolved along the class hierarchy of the stub object: for a stub of
 * class {@code a.b.C} the implementation interface is {@code a.b.IApiC} (or
 * {@code a.b.IApiOuter$IApiC} for a nested class), looked up for the superclasses until found
 * (factory PICO OS 5.13.7 {@code smartisanos.tnt.ITntHierarchyBase}).
 *
 * @hide
 */
public interface ITntHierarchyBase extends ITntBase {
    HashMap<Class, Class> sTntBase = new HashMap<>();

    static ITntBase createImpl(Object... objects) throws ClassNotFoundException {
        if (objects == null || objects.length == 0 || objects[0] == null) {
            throw new IllegalArgumentException(
                    "Invalid base class object, you must pass me the base class object.");
        }
        ClassLoader classLoader = objects[0].getClass().getClassLoader();
        Object stubObject = objects[0];
        Class superclass = stubObject.getClass();
        Class clsLoadClass = sTntBase.get(superclass);
        if (clsLoadClass == null) {
            try {
                clsLoadClass = classLoader.loadClass(getInterfaceClass(superclass));
            } catch (ClassNotFoundException e) {
            }
            if (clsLoadClass != null && !ITntHierarchyBase.class.isAssignableFrom(clsLoadClass)) {
                clsLoadClass = null;
            }
            while (clsLoadClass == null) {
                superclass = superclass.getSuperclass();
                if (superclass == null) {
                    break;
                }
                try {
                    clsLoadClass = classLoader.loadClass(getInterfaceClass(superclass));
                } catch (ClassNotFoundException e) {
                }
                if (clsLoadClass != null
                        && !ITntHierarchyBase.class.isAssignableFrom(clsLoadClass)) {
                    clsLoadClass = null;
                }
            }
            if (superclass == null) {
                throw new RuntimeException(
                        "Could not find tnt class for " + stubObject.getClass().getName());
            }
            sTntBase.put(superclass, clsLoadClass);
        }
        return TntImplFactory.createImpl(clsLoadClass, objects);
    }

    static String getInterfaceClass(Class stubClass) {
        Class enclosingClass = stubClass.getEnclosingClass();
        if (enclosingClass != null) {
            return new StringBuilder()
                    .append(enclosingClass.getPackage() == null
                            ? "" : enclosingClass.getPackage().getName())
                    .append(".IApi")
                    .append(enclosingClass.getSimpleName())
                    .append("$IApi")
                    .append(stubClass.getSimpleName())
                    .toString();
        }
        return new StringBuilder()
                .append(stubClass.getPackage() == null ? "" : stubClass.getPackage().getName())
                .append(".IApi")
                .append(stubClass.getSimpleName())
                .toString();
    }
}
