// Copyright 2026 Picomisu contributors
// SPDX-License-Identifier: Apache-2.0
package android.view;

import android.graphics.Canvas;
import android.util.Log;

/**
 * PICO software-canvas extension with a retained native Surface while locked.
 * @hide
 */
public class ExtSurfaceImpl implements IExtSurface {
    private static final String TAG = "ExtSurfaceImpl";
    private final Surface mBase;
    private final Canvas mBaseCanvas;
    private long mLockedObject;

    public ExtSurfaceImpl(Surface base, Canvas canvas) {
        mBase = base;
        mBaseCanvas = canvas;
    }

    private static native long nativeLockCanvasFor2DVr(long nativeObject, Canvas canvas);
    private static native void nativeUnlockCanvasAndPostFor2DVr(long nativeObject, Canvas canvas);
    private static native void nativeReleaseSurfaceObject(long nativeObject);

    private void checkNotReleasedLocked() {
        if (mBase.mNativeObject == 0) {
            throw new IllegalStateException("Surface has already been released.");
        }
    }

    @Override
    public Canvas lockCanvasFor2DVr() {
        synchronized (mBase.mLock) {
            checkNotReleasedLocked();
            if (mLockedObject != 0) {
                throw new IllegalArgumentException("Surface was already locked for lockCanvasFor2DVr");
            }
            mLockedObject = nativeLockCanvasFor2DVr(mBase.mNativeObject, mBaseCanvas);
            return mBaseCanvas;
        }
    }

    @Override
    public void unlockCanvasAndPostFor2DVr(Canvas canvas) {
        synchronized (mBase.mLock) {
            checkNotReleasedLocked();
            unlockSwCanvasAndPostFor2DVr(canvas);
        }
    }

    private void unlockSwCanvasAndPostFor2DVr(Canvas canvas) {
        if (canvas != mBaseCanvas) {
            throw new IllegalArgumentException("canvas object must be the same instance that was previously returned by lockCanvas");
        }
        if (mBase.mNativeObject != mLockedObject) {
            Log.w(TAG, "WARNING: Surface's mNativeObject (0x"
                    + Long.toHexString(mBase.mNativeObject) + ") != mLockedObject (0x"
                    + Long.toHexString(mLockedObject) + ")");
        }
        if (mLockedObject == 0) {
            throw new IllegalStateException("Surface was not locked");
        }
        try {
            nativeUnlockCanvasAndPostFor2DVr(mLockedObject, canvas);
        } finally {
            nativeReleaseSurfaceObject(mLockedObject);
            mLockedObject = 0;
        }
    }
}
