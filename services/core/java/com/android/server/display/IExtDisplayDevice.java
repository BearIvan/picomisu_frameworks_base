// Copyright 2026 Picomisu contributors
// SPDX-License-Identifier: Apache-2.0
package com.android.server.display;

import android.view.SurfaceControl;

import com.pico.util.IExtBase;

/**
 * PICO display device extension (factory PICO OS 5.13.7
 * com.android.server.display.IExtDisplayDevice).
 * @hide
 */
public interface IExtDisplayDevice extends IExtBase {
    void setDisplayFlagsLocked(SurfaceControl.Transaction t, int flags);
}
