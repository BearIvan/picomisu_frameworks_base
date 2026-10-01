// Copyright 2026 Picomisu contributors
// SPDX-License-Identifier: Apache-2.0

package com.android.internal.dynamicanimation;

/**
 * Hide this for now, in case we want to change the API in the future
 * (factory PICO OS 5.13.7 {@code com.android.internal.dynamicanimation.Force}, from AndroidX
 * dynamicanimation).
 *
 * @hide
 */
interface Force {
    // Acceleration based on position.
    float getAcceleration(float position, float velocity);

    boolean isAtEquilibrium(float value, float velocity);
}
