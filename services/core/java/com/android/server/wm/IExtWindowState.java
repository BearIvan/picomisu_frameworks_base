// Copyright 2026 Picomisu contributors
// SPDX-License-Identifier: Apache-2.0
package com.android.server.wm;

import android.graphics.Rect;

import com.pico.util.IExtBase;

/**
 * PICO window extension (factory PICO OS 5.13.7 com.android.server.wm.IExtWindowState).
 * @hide
 */
public interface IExtWindowState extends IExtBase {
    void adjustWindowFrame(WindowFrames windowFrames, Rect displayFrame);

    boolean disableReceiveKeys();
}
