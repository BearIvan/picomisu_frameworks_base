// Copyright 2026 Picomisu contributors
// SPDX-License-Identifier: Apache-2.0
package com.android.server.input;

import android.view.InputDevice;

import com.pico.util.IExtBase;

/**
 * PICO input manager service extension: virtual input device of the VR input devices.
 * @hide
 */
public interface IExtInputManagerService extends IExtBase {
    default InputDevice getVitualInputDevice(int deviceId) {
        return null;
    }
}
