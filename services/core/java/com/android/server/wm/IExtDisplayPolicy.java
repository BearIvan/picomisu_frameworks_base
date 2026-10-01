// Copyright 2026 Picomisu contributors
// SPDX-License-Identifier: Apache-2.0
package com.android.server.wm;

import android.graphics.Rect;

import com.pico.util.IExtBase;

/**
 * PICO display policy extension (factory PICO OS 5.13.7
 * com.android.server.wm.IExtDisplayPolicy).
 * @hide
 */
public interface IExtDisplayPolicy extends IExtBase {
    void calculateFrameWhenLayoutWindowLw(WindowState win, DisplayFrames displayFrames, Rect pf,
            Rect df, Rect of, Rect cf, Rect vf, Rect dcf, Rect sf);
}
