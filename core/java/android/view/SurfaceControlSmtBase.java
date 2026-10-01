// Copyright 2026 Picomisu contributors
// SPDX-License-Identifier: Apache-2.0

package android.view;

import android.util.Log;
import android.util.Slog;

/**
 * Smartisan extension of {@link SurfaceControl}: synchronous close of the global transaction
 * and a blur layer flag for the surface builder (factory PICO OS 5.13.7
 * {@code android.view.SurfaceControlSmtBase}; the PC mode, inset and blur setters are empty
 * on the factory).
 *
 * @hide
 */
public class SurfaceControlSmtBase {
    protected static final String TAG = "SurfaceControl";

    public static final int FX_SURFACE_BLUR_EXT = 0x00010000;

    protected SurfaceControl mSurfaceControl;

    public SurfaceControlSmtBase(SurfaceControl surfaceControl) {
        mSurfaceControl = surfaceControl;
    }

    /**
     * Like {@link SurfaceControl#closeTransaction} but applies the global transaction
     * synchronously.
     */
    public static void closeTransactionSync() {
        synchronized (SurfaceControl.class) {
            if (SurfaceControl.sTransactionNestCount == 0) {
                Log.e(TAG,
                        "Call to SurfaceControl.closeTransaction without matching openTransaction");
            } else if (--SurfaceControl.sTransactionNestCount > 0) {
                return;
            }
            SurfaceControl.sGlobalTransaction.apply(true);
        }
    }

    public void setPcMode(boolean pcMode) {
        mSurfaceControl.checkNotReleased();
        synchronized (SurfaceControl.class) {
        }
    }

    public void setSurfaceInset(float leftInset, float topInset, float rightInset,
            float bottomInset) {
        mSurfaceControl.checkNotReleased();
        synchronized (SurfaceControl.class) {
        }
    }

    public void setBlurAmount(float amount) {
        synchronized (SurfaceControl.class) {
        }
    }

    /** Smartisan part of {@link SurfaceControl.Builder}. */
    public static class BuilderSmtEx {
        private SurfaceControl.Builder mBuilder;

        public BuilderSmtEx(SurfaceControl.Builder builder) {
            mBuilder = builder;
        }

        public SurfaceControl.Builder setBlurLayer() {
            mBuilder.setFlags(FX_SURFACE_BLUR_EXT, SurfaceControl.FX_SURFACE_MASK);
            Slog.w(TAG, "FX_SURFACE_BLUR_EXT | FX_SURFACE_MASK " + mBuilder.mFlags);
            return mBuilder;
        }
    }
}
