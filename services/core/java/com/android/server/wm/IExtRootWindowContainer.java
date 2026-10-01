// Copyright 2026 Picomisu contributors
// SPDX-License-Identifier: Apache-2.0
package com.android.server.wm;

import android.os.IBinder;

import com.pico.util.IExtBase;

/**
 * PICO extension of the window hierarchy root (factory PICO OS 5.13.7
 * com.android.server.wm.IExtRootWindowContainer).
 * @hide
 */
public interface IExtRootWindowContainer extends IExtBase {
    WindowState getInputMethodTargetWindow();

    int getTopFocusedDisplayId();

    void onImeTargetChanged(IBinder target);

    void onImeVisibleChanged(boolean visible);

    void onTopFocusedDisplayIdChanged(int displayId);

    int redirectPositionWhenPositionChildAt(int position, DisplayContent child);
}
