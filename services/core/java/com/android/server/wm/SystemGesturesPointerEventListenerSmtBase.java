// Copyright 2026 Picomisu contributors
// SPDX-License-Identifier: Apache-2.0

package com.android.server.wm;

/**
 * Reconstructed from the PICO OS 5.13.7 factory services. Nothing in the factory services,
 * sys-services or sysmonitor-services references it; it is carried for parity.
 *
 * @hide
 */
public abstract class SystemGesturesPointerEventListenerSmtBase {
    protected DisplayContent mDisplayContent;
    protected boolean mFirstClickTopOrBottom;
    protected SystemGesturesPointerEventListener mListener;
    protected int mOffset = 5;
    boolean mScrollFired = false;
    protected int mSidebarIgnoreEdge;

    public SystemGesturesPointerEventListenerSmtBase(SystemGesturesPointerEventListener listener) {
        this.mListener = listener;
    }

    void onFlingBoost(SystemGesturesPointerEventListener.Callbacks mCallbacks, int duration, float velocityX, float velocityY) {
        if (Math.abs(velocityY) >= Math.abs(velocityX)) {
            mCallbacks.onVerticalFling(duration);
        } else {
            mCallbacks.onHorizontalFling(duration);
        }
    }

    void onScroll(SystemGesturesPointerEventListener.Callbacks mCallbacks) {
        if (!this.mScrollFired) {
            mCallbacks.onScroll(true);
            this.mScrollFired = true;
        }
    }

    void onActionCancel(SystemGesturesPointerEventListener.Callbacks mCallbacks) {
        if (this.mScrollFired) {
            mCallbacks.onScroll(false);
        }
        this.mScrollFired = false;
    }
}
