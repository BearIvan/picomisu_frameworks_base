// Copyright 2026 Picomisu contributors
// SPDX-License-Identifier: Apache-2.0
package android.hardware.input;

import android.app.ActivityThread;
import android.app.Application;
import android.os.RemoteException;
import android.util.Log;
import android.view.Display;
import android.view.InputDevice;
import android.view.InputEvent;
import android.view.KeyEvent;

/**
 * PICO input manager extension.
 * @hide
 */
public class ExtInputManagerImpl implements IExtInputManager {
    private static final String TAG = "InputManager";
    private InputManager mBase;
    private InputDevice mVirtualInputDevice;

    public ExtInputManagerImpl(InputManager base) {
        mBase = base;
    }

    /**
     * Factory PICO OS 5.13.7: a BACK key that an app on a 2D panel (virtual display) injects for
     * display 0 goes to the app's own display instead, so it closes the panel's activity and not
     * the VR scene. Motion events are re-targeted in system_server
     * (WindowManagerService.injectInputAfterTransactionsApplied).
     */
    @Override
    public void adjustInjectInputEventIfNeeded(InputEvent event, int mode) {
        if (!(event instanceof KeyEvent) || event.getDisplayId() != Display.DEFAULT_DISPLAY) {
            return;
        }
        if (((KeyEvent) event).getKeyCode() != KeyEvent.KEYCODE_BACK) {
            return;
        }
        final Application app = ActivityThread.currentApplication();
        if (app == null) {
            return;
        }
        final int displayId = app.getDisplayId();
        if (displayId != Display.DEFAULT_DISPLAY && displayId != Display.INVALID_DISPLAY) {
            Log.w(TAG, "injectInputEvent redirect event displayId to [" + displayId + "] "
                    + event);
            event.setDisplayId(displayId);
        }
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
