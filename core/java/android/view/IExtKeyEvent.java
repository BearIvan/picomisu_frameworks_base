// Copyright 2026 Picomisu contributors
// SPDX-License-Identifier: Apache-2.0

package android.view;

import com.pico.util.IExtBase;

/**
 * PICO key codes (PICO OS 5.13.7 factory framework): controller HOME keys, the defined confirm
 * keys, recenter and the virtual confirm + volume down combination key.
 *
 * @hide
 */
public interface IExtKeyEvent extends IExtBase {
    int KEYCODE_LCONTROLLER_HOME = 901;
    int KEYCODE_RCONTROLLER_HOME = 902;
    int KEYCODE_DEFINE_CONFIRM = 1001;
    int KEYCODE_DEFINE_CONTROLLER_CONFIRM = 1002;
    int KEYCODE_DEFINE_DPINOUT = 1003;
    int KEYCODE_RECENTER = 1004;
    int VIRTUAL_KEY_TRIGGER_COMBINATION_KEY_CONFIRM_AND_VOLUME_DOWN = 1005;
}
