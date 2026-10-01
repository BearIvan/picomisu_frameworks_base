// Copyright 2026 Picomisu contributors
// SPDX-License-Identifier: Apache-2.0
package com.android.server.display;

/**
 * PICO virtual display adapter extension (factory PICO OS 5.13.7
 * com.android.server.display.ExtVirtualDisplayAdapterImpl): the PICO virtual display creation
 * flags (1 << 14, 1 << 15, 1 << 16, 1 << 20) are kept in the DisplayDeviceInfo flags.
 */
public class ExtVirtualDisplayAdapterImpl implements IExtVirtualDisplayAdapter {
    private VirtualDisplayAdapter mBase;

    public ExtVirtualDisplayAdapterImpl(VirtualDisplayAdapter base) {
        mBase = base;
    }

    @Override
    public void adjustDisplayDeviceInfoFlags(DisplayDeviceInfo displayDeviceInfo, int flags) {
        if ((flags & 32768) != 0) {
            displayDeviceInfo.flags |= 32768;
        }
        if ((flags & 65536) != 0) {
            displayDeviceInfo.flags |= 65536;
        }
        if ((flags & 16384) != 0) {
            displayDeviceInfo.flags |= 16384;
        }
        if ((flags & 1048576) != 0) {
            displayDeviceInfo.flags |= 1048576;
        }
    }
}
