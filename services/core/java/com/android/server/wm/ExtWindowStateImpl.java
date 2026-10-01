// Copyright 2026 Picomisu contributors
// SPDX-License-Identifier: Apache-2.0
package com.android.server.wm;

import android.graphics.Rect;

/**
 * PICO window extension (factory PICO OS 5.13.7 com.android.server.wm.ExtWindowStateImpl).
 */
public class ExtWindowStateImpl implements IExtWindowState {
    private WindowState mBase;
    private boolean mHasInit;
    private boolean mIsXrRuntimeDialog;

    public ExtWindowStateImpl(WindowState base) {
        mBase = base;
    }

    /**
     * WindowState.applyGravityAndUpdateFrame: on displays with flag 1 << 15 a non-IME window
     * that covers the whole display frame is inset by one pixel on each side.
     */
    @Override
    public void adjustWindowFrame(WindowFrames windowFrames, Rect displayFrame) {
        if (mBase.getDisplayContent() != null
                && mBase.getDisplayContent().getDisplay() != null
                && (mBase.getDisplayContent().getDisplay().getFlags() & 32768) != 0
                && !mBase.isInputMethodWindow()
                && displayFrame.equals(windowFrames.mFrame)) {
            windowFrames.mFrame.inset(1, 1);
        }
    }

    /** WindowState.canReceiveKeys: the XR runtime dialog takes no keys while display 0 is top. */
    @Override
    public boolean disableReceiveKeys() {
        if (!mHasInit) {
            mHasInit = true;
            mIsXrRuntimeDialog = "PicoXR_CustomDialog".equals(mBase.mAttrs.getTitle());
        }
        return mIsXrRuntimeDialog
                && mBase.mWmService.mRoot.getExt().getTopFocusedDisplayId() == 0;
    }
}
