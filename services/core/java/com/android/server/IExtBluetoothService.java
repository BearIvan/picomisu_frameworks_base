// Copyright 2026 Picomisu contributors
// SPDX-License-Identifier: Apache-2.0
package com.android.server;

import com.pico.util.IExtBase;

/**
 * PICO BluetoothService extension (factory PICO OS 5.13.7
 * com.android.server.IExtBluetoothService).
 */
public interface IExtBluetoothService extends IExtBase {
    void bindUnbindPeripheralServiceIfNeed(boolean bind);
}
