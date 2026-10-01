// Copyright 2026 Picomisu contributors
// SPDX-License-Identifier: Apache-2.0
package com.android.server.display;

/**
 * PICO display device info extension (factory PICO OS 5.13.7
 * com.android.server.display.ExtDisplayDeviceInfoImpl; empty there as well).
 */
public class ExtDisplayDeviceInfoImpl implements IExtDisplayDeviceInfo {
    private DisplayDeviceInfo mBase;

    public ExtDisplayDeviceInfoImpl(DisplayDeviceInfo base) {
        mBase = base;
    }
}
