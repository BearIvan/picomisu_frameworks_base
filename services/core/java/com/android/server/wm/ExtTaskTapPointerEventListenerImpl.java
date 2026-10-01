// Copyright 2026 Picomisu contributors
// SPDX-License-Identifier: Apache-2.0
package com.android.server.wm;

/**
 * PICO task tap listener extension (factory PICO OS 5.13.7
 * com.android.server.wm.ExtTaskTapPointerEventListenerImpl).
 */
public class ExtTaskTapPointerEventListenerImpl implements IExtTaskTapPointerEventListener {
    private TaskTapPointerEventListener mBase;

    public ExtTaskTapPointerEventListenerImpl(TaskTapPointerEventListener base) {
        mBase = base;
    }

    /** No task resize pointer icons on hover. */
    @Override
    public boolean disableHover() {
        return true;
    }
}
