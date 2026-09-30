// Copyright 2026 Picomisu contributors
// SPDX-License-Identifier: Apache-2.0
package android.hardware.input;

import android.os.RemoteException;
import android.view.InputDevice;

/**
 * PICO input manager extension.
 * @hide
 */
public class ExtInputManagerImpl implements IExtInputManager {
    private InputManager mBase;
    private InputDevice mVirtualInputDevice;

    public ExtInputManagerImpl(InputManager base) {
        mBase = base;
    }

    /**
     * Returns the virtual input device of the input manager service for the ids of the VR
     * head-control handles, gesture hands and joystick, null for any other id. The device is
     * queried once and cached.
     */
    @Override
    public InputDevice getVirtualInputDeviceIfNeed(int id) {
        if (id < DEVICE_HEAD_CONTROL_HANDLE_MIN && id != DEVICE_GESTURE_LEFT_HAND
                && id != DEVICE_GESTURE_RIGHT_HAND && id != DEVICE_JOYSTICK) {
            return null;
        }
        if (mVirtualInputDevice == null) {
            try {
                mVirtualInputDevice = mBase.mIm.getVitualInputDevice(id);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
        return mVirtualInputDevice;
    }
}
