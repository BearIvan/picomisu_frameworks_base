// Copyright 2026 Picomisu contributors
// SPDX-License-Identifier: Apache-2.0
package android.view;

import android.graphics.Canvas;
import com.pico.util.IExtBase;

/**
 * PICO software-canvas surface extension.
 * @hide
 */
public interface IExtSurface extends IExtBase {
    Canvas lockCanvasFor2DVr();
    void unlockCanvasAndPostFor2DVr(Canvas canvas);
}
