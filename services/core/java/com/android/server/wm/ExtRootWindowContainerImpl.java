/*
 * Copyright 2026 Picomisu contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package com.android.server.wm;

import android.os.IBinder;
import android.util.Slog;

/**
 * PICO VR state of the window hierarchy root (factory PICO OS 5.13.7
 * com.android.server.wm.ExtRootWindowContainerImpl): the top focused display as reported to
 * SystemExt and used to re-target injected motion events, and the window the shown IME serves.
 */
public class ExtRootWindowContainerImpl implements IExtRootWindowContainer {
    static final String TAG = "WindowManager";

    private RootWindowContainer mBase;
    private IBinder mImeTarget;
    private WindowState mImeTargetWindow;
    private int mTopFocusedDisplayId = -1;

    public ExtRootWindowContainerImpl(RootWindowContainer base) {
        mBase = base;
    }

    /**
     * RootWindowContainer.positionChildAt: the VR loading display always goes to the bottom, and
     * a display moved to the bottom goes just above it.
     */
    @Override
    public int redirectPositionWhenPositionChildAt(int position, DisplayContent child) {
        if (child.getDisplay().getExt().isVrLoadingDisplay()) {
            return Integer.MIN_VALUE;
        }
        if ((position == Integer.MIN_VALUE || position == 0) && mBase.getChildCount() > 1
                && mBase.getChildAt(0).getDisplay().getExt().isVrLoadingDisplay()) {
            return 1;
        }
        return position;
    }

    /** Window token of the current IME target (posted on the WM handler). */
    @Override
    public void onImeTargetChanged(IBinder target) {
        synchronized (mBase.mWmService.mGlobalLock) {
            try {
                WindowManagerService.boostPriorityForLockedSection();
                mImeTarget = target;
            } finally {
                WindowManagerService.resetPriorityAfterLockedSection();
            }
        }
    }

    /**
     * The IME was shown (remember its target window) or hidden. Called by
     * InputMethodManagerService without the window manager lock, as on the factory.
     */
    @Override
    public void onImeVisibleChanged(boolean visible) {
        if (mImeTarget == null) {
            return;
        }
        mImeTargetWindow = visible
                ? mBase.mWmService.windowForClientLocked((Session) null, mImeTarget, false)
                : null;
        Slog.w(TAG, "onImeVisibleChanged, imeTargetWindow [" + mImeTargetWindow + "]");
    }

    @Override
    public WindowState getInputMethodTargetWindow() {
        return mImeTargetWindow;
    }

    /** RootWindowContainer.updateFocusedWindowLocked picked a new top focused display. */
    @Override
    public void onTopFocusedDisplayIdChanged(int displayId) {
        mTopFocusedDisplayId = displayId;
        mBase.mWmService.mAtmService.getActivityStartController().getExt().getSystemExt()
                .notifyFocusDisplayChanged(displayId);
    }

    /** The last top focused display, -1 before the first focus update. */
    @Override
    public int getTopFocusedDisplayId() {
        return mTopFocusedDisplayId;
    }
}
