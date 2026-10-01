// Copyright 2026 Picomisu contributors
// SPDX-License-Identifier: Apache-2.0
package com.android.server;

import android.content.Context;

import com.pico.util.IExtBase;

/**
 * PICO DeviceIdleController extension (factory PICO OS 5.13.7
 * com.android.server.IExtDeviceIdleController).
 */
public interface IExtDeviceIdleController extends IExtBase {
    default boolean isDisableIdle(Context context) {
        return false;
    }

    default void registerDisableIdle(Context context) {
    }
}
