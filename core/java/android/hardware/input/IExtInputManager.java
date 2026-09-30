// Copyright 2026 Picomisu contributors
// SPDX-License-Identifier: Apache-2.0
package android.hardware.input;

import android.view.InputDevice;

import com.pico.util.IExtBase;

/**
 * PICO input manager extension: virtual input device standing in for the VR input devices.
 * @hide
 */
public interface IExtInputManager extends IExtBase {
    /** Gesture input device of the left hand. */
    int DEVICE_GESTURE_LEFT_HAND = 20001;
    /** Gesture input device of the right hand. */
    int DEVICE_GESTURE_RIGHT_HAND = 20002;
    /** Lowest device id of the head-control handles. */
    int DEVICE_HEAD_CONTROL_HANDLE_MIN = 100000;
    /** Joystick input device. */
    int DEVICE_JOYSTICK = 10003;

    default InputDevice getVirtualInputDeviceIfNeed(int id) {
        return null;
    }
}
