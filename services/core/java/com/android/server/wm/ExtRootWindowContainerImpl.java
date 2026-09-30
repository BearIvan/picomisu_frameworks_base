/*
 * Copyright 2026 Picomisu contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package com.android.server.wm;

import static android.view.Display.INVALID_DISPLAY;

/**
 * PICO VR state of the window hierarchy root (factory PICO OS 5.13.7
 * com.android.server.wm.ExtRootWindowContainerImpl): the top focused display as reported to
 * SystemExt and used to re-target injected motion events.
 */
public class ExtRootWindowContainerImpl {
    static final String TAG = "WindowManager";

    private final RootWindowContainer mBase;
    private volatile int mTopFocusedDisplayId = INVALID_DISPLAY;

    public ExtRootWindowContainerImpl(RootWindowContainer base) {
        mBase = base;
    }

    /** RootWindowContainer.updateFocusedWindowLocked picked a new top focused display. */
    public void onTopFocusedDisplayIdChanged(int displayId) {
        mTopFocusedDisplayId = displayId;
        final ActivityStartController controller =
                mBase.mWmService.mAtmService.getActivityStartController();
        if (controller != null) {
            controller.getExt().getSystemExt().notifyFocusDisplayChanged(displayId);
        }
    }

    /** The last top focused display, -1 before the first focus update. Read without the lock. */
    public int getTopFocusedDisplayId() {
        return mTopFocusedDisplayId;
    }
}
