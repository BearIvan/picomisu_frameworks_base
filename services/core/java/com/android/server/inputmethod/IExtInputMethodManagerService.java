// Copyright 2026 Picomisu contributors
// SPDX-License-Identifier: Apache-2.0
package com.android.server.inputmethod;

import android.os.IBinder;

import com.android.server.wm.WindowManagerInternal;
import com.pico.util.IExtBase;

/**
 * PICO input method manager service extension (factory PICO OS 5.13.7
 * com.android.server.inputmethod.IExtInputMethodManagerService).
 * @hide
 */
public interface IExtInputMethodManagerService extends IExtBase {
    int CODE_ON_DISPLAY_CONTENT_DESTROY = 10001;

    int computeImeDisplayIdForTarget(int displayId,
            InputMethodManagerService.ImeDisplayValidator checker);

    boolean disableCheckClientState();

    boolean disableResetDefaultIme(String curMethodId);

    void dispatchImeVisibleStatusToNS(int displayId);

    void onDisplayContentDestroy(int displayId);

    void onHideCurrentInput();

    void onShowCurrentInput(IBinder curFocusedWindow, WindowManagerInternal windowManagerInternal);

    void updateCurrentFocusedWindow(IBinder currentFocusWin);
}
