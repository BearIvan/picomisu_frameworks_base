// Copyright 2026 Picomisu contributors
// SPDX-License-Identifier: Apache-2.0

package com.android.internal.dynamicanimation;

import android.util.FloatProperty;

/**
 * <p>FloatPropertyCompat is an abstraction that can be used to represent a mutable float value
 * that is held in a host object. To use this class, a setter and a getter must be implemented.
 * (factory PICO OS 5.13.7 {@code com.android.internal.dynamicanimation.FloatPropertyCompat}, from
 * AndroidX dynamicanimation)
 *
 * @param <T> the class on which the property is declared
 * @hide
 */
public abstract class FloatPropertyCompat<T> {
    final String mPropertyName;

    /**
     * A constructor that takes an identifying name for the property. This name will be used
     * for debugging purposes only.
     *
     * @param name a name for the property
     */
    public FloatPropertyCompat(String name) {
        mPropertyName = name;
    }

    /**
     * Create a {@link FloatPropertyCompat} wrapper for a {@link FloatProperty} object. The new
     * {@link FloatPropertyCompat} instance will access and modify the property value of
     * {@link FloatProperty} through the {@link FloatProperty} instance's setter and getter.
     *
     * @param property FloatProperty instance to be wrapped
     * @param <T> the class on which the Property is declared
     * @return a new {@link FloatPropertyCompat} wrapper for the given {@link FloatProperty} object
     */
    public static <T> FloatPropertyCompat<T> createFloatPropertyCompat(
            final FloatProperty<T> property) {
        return new FloatPropertyCompat<T>(property.getName()) {
            @Override
            public float getValue(T object) {
                return property.get(object);
            }

            @Override
            public void setValue(T object, float value) {
                property.setValue(object, value);
            }
        };
    }

    /**
     * Returns the current value that this property represents on the given <code>object</code>.
     *
     * @param object object which this property represents
     * @return the current property value of the given object
     */
    public abstract float getValue(T object);

    /**
     * Sets the value on <code>object</code> which this property represents.
     *
     * @param object object which this property represents
     * @param value new value of the property
     */
    public abstract void setValue(T object, float value);
}
