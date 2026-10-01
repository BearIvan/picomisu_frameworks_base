// Copyright 2026 Picomisu contributors
// SPDX-License-Identifier: Apache-2.0

package android.view;

import android.graphics.Point;

/**
 * Smartisan extension base of a {@link MotionEvent}
 * (factory PICO OS 5.13.7 {@code android.view.MotionEventSmtBase}; the factory has no subclass,
 * so it is never instantiated).
 *
 * @hide
 */
public abstract class MotionEventSmtBase {
    protected MotionEvent mMotionEvent;
    protected Point mOffset = new Point(0, 0);

    public MotionEventSmtBase(MotionEvent motionEvent) {
        mMotionEvent = motionEvent;
    }

    public void setOffset(Point offset) {
        if (offset == null) {
            mOffset.set(0, 0);
        } else {
            mOffset.set(offset.x, offset.y);
        }
    }

    public boolean isGeneratedGesture() {
        final int flags = mMotionEvent.getFlags();
        return (flags & MotionEvent.FLAG_IS_GENERATED_GESTURE) != 0;
    }
}
