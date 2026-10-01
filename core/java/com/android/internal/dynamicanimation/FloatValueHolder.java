// Copyright 2026 Picomisu contributors
// SPDX-License-Identifier: Apache-2.0

package com.android.internal.dynamicanimation;

/**
 * <p>FloatValueHolder holds a float value. FloatValueHolder provides a setter and a getter (
 * i.e. {@link #setValue(float)} and {@link #getValue()}) to access this float value. Animations
 * can be performed on a FloatValueHolder instance. During each frame of the animation, the
 * FloatValueHolder will have its value updated via {@link #setValue(float)}. The caller can
 * obtain the up-to-date animation value via {@link FloatValueHolder#getValue()}.
 * (factory PICO OS 5.13.7 {@code com.android.internal.dynamicanimation.FloatValueHolder}, from
 * AndroidX dynamicanimation)
 *
 * @hide
 */
public final class FloatValueHolder {
    private float mValue = 0.0f;

    /**
     * Constructs a holder for a float value that is initialized to 0.
     */
    public FloatValueHolder() {
    }

    /**
     * Constructs a holder for a float value that is initialized to the input value.
     *
     * @param value the value to initialize the value held in the FloatValueHolder
     */
    public FloatValueHolder(float value) {
        setValue(value);
    }

    /**
     * Sets the value held in the FloatValueHolder instance.
     *
     * @param value float value held in the FloatValueHolder instance
     */
    public void setValue(float value) {
        mValue = value;
    }

    /**
     * Returns the float value held in the FloatValueHolder instance.
     *
     * @return float value held in the FloatValueHolder instance
     */
    public float getValue() {
        return mValue;
    }
}
