// Copyright 2026 Picomisu contributors
// SPDX-License-Identifier: Apache-2.0
package com.android.server.input;

import android.hardware.input.IExtInputManager;
import android.text.TextUtils;
import android.view.InputDevice;

/**
 * PICO input manager service extension.
 * @hide
 */
public class ExtInputManagerServiceImpl implements IExtInputManagerService {
    private InputManagerService mBase;

    public ExtInputManagerServiceImpl(InputManagerService base) {
        mBase = base;
    }

    /**
     * Returns the input device named "virtual_input_device" for the ids of the VR head-control
     * handles, gesture hands and joystick, null for any other id or when it is not attached.
     */
    @Override
    public InputDevice getVitualInputDevice(int deviceId) {
        if (deviceId < IExtInputManager.DEVICE_HEAD_CONTROL_HANDLE_MIN
                && deviceId != IExtInputManager.DEVICE_GESTURE_LEFT_HAND
                && deviceId != IExtInputManager.DEVICE_GESTURE_RIGHT_HAND
                && deviceId != IExtInputManager.DEVICE_JOYSTICK) {
            return null;
        }
        synchronized (mBase.mInputDevicesLock) {
            for (InputDevice inputDevice : mBase.getInputDevices()) {
                if (TextUtils.equals("virtual_input_device", inputDevice.getName())) {
                    return inputDevice;
                }
            }
        }
        return null;
    }
}
