// Copyright 2026 Picomisu contributors
// SPDX-License-Identifier: Apache-2.0
package com.android.server.wm;

import android.graphics.Rect;
import android.os.IBinder;
import android.os.Parcel;

import com.pico.util.IExtBase;

/**
 * PICO window manager service extension (factory PICO OS 5.13.7
 * com.android.server.wm.IExtWindowManagerService).
 * @hide
 */
public interface IExtWindowManagerService extends IExtBase {
    int CODE_ENABLE_DEBUG = 10001;

    boolean adjustGetWindowDisplayFrame(WindowState win, Rect outDisplayFrame);

    boolean disableShowStrictModeViolation();

    void notifyImeTargetChanged(IBinder target);

    void notifyImeVisibleChanged(boolean visible);

    boolean onTransact(int code, Parcel data, Parcel reply, int flags);

    void updateDisplayFocus(int displayId, String reason);
}
