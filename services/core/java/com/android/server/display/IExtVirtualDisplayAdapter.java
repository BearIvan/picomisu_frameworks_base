// Copyright 2026 Picomisu contributors
// SPDX-License-Identifier: Apache-2.0
package com.android.server.display;

import com.pico.util.IExtBase;

/**
 * PICO virtual display adapter extension (factory PICO OS 5.13.7
 * com.android.server.display.IExtVirtualDisplayAdapter).
 * @hide
 */
public interface IExtVirtualDisplayAdapter extends IExtBase {
    void adjustDisplayDeviceInfoFlags(DisplayDeviceInfo displayDeviceInfo, int flags);
}
