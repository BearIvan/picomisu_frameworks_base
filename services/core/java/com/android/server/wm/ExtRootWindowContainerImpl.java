/*
 * Copyright 2026 Picomisu contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package com.android.server.wm;

import static android.view.Display.INVALID_DISPLAY;

import android.graphics.Rect;
import android.os.IBinder;
import android.util.Slog;

import java.util.Arrays;
import java.util.List;

/**
 * PICO VR state of the window hierarchy root (factory PICO OS 5.13.7
 * com.android.server.wm.ExtRootWindowContainerImpl): the top focused display as reported to
 * SystemExt and used to re-target injected motion events, and the window the shown IME serves.
 */
public class ExtRootWindowContainerImpl {
    static final String TAG = "WindowManager";

    /** Apps whose display frame excludes 50 px at the bottom while they are the IME target. */
    private static final List<String> ADJUST_GET_DISPLAY_FRAME_PACKAGES =
            Arrays.asList("com.xwms.pplevel");

    private final RootWindowContainer mBase;
    private volatile int mTopFocusedDisplayId = INVALID_DISPLAY;
    private IBinder mImeTarget;
    private WindowState mImeTargetWindow;

    public ExtRootWindowContainerImpl(RootWindowContainer base) {
        mBase = base;
    }

    /**
     * RootWindowContainer.positionChildAt: the VR loading display always goes to the bottom, and
     * a display moved to the bottom goes just above it.
     */
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

    /** Window token of the current IME target (posted on the WM handler). */
    public void onImeTargetChanged(IBinder target) {
        synchronized (mBase.mWmService.mGlobalLock) {
            mImeTarget = target;
        }
    }

    /** The IME was shown (remember its target window) or hidden. */
    public void onImeVisibleChanged(boolean visible) {
        synchronized (mBase.mWmService.mGlobalLock) {
            if (mImeTarget == null) {
                return;
            }
            mImeTargetWindow = visible
                    ? mBase.mWmService.windowForClientLocked(null, mImeTarget, false) : null;
            Slog.w(TAG, "onImeVisibleChanged, imeTargetWindow [" + mImeTargetWindow + "]");
        }
    }

    public WindowState getInputMethodTargetWindow() {
        return mImeTargetWindow;
    }

    /** WindowManagerService.getWindowDisplayFrame, under the global lock. */
    public void adjustWindowDisplayFrame(WindowState win, Rect outDisplayFrame) {
        if (ADJUST_GET_DISPLAY_FRAME_PACKAGES.contains(win.getAttrs().packageName)
                && mImeTargetWindow == win) {
            outDisplayFrame.bottom -= 50;
        }
    }
}
