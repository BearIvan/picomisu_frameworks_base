// Copyright 2026 Picomisu contributors
// SPDX-License-Identifier: Apache-2.0

package android.view;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;

/**
 * Smartisan extension of a {@link View} (its {@code mViewSmtEx}, see {@link View#getSmtEx}):
 * scaled snapshots used by the Smartisan starting window cache
 * (factory PICO OS 5.13.7 {@code android.view.ViewSmtBase}).
 *
 * @hide
 */
public class ViewSmtBase {
    protected View mView;

    public ViewSmtBase(View view) {
        mView = view;
    }

    public interface OnForceTouchListener {
        void onForceTouch();
    }

    public interface OnLongClickAndMoveListener {
        boolean onLongClickAndMove(View v);
    }

    public interface OnShowContextMenuListener {
        boolean OnShowContextMenu(View v, float x, float y);
    }

    abstract static class ListenerInfoSmtBase {
        public OnForceTouchListener mForceTouchListener;
        protected OnLongClickAndMoveListener mOnLongClickAndMoveListener;
        protected OnShowContextMenuListener mOnShowContextMenuListener;
    }

    /**
     * Draws the view into a new bitmap scaled by {@code scale} (the factory variant of the old
     * {@code View.createSnapshot}).
     */
    public Bitmap createSnapshot(Bitmap.Config quality, int backgroundColor, boolean skipChildren,
            float scale) {
        int width = mView.mRight - mView.mLeft;
        int height = mView.mBottom - mView.mTop;

        final View.AttachInfo attachInfo = mView.mAttachInfo;
        width = (int) ((width * scale) + 0.5f);
        height = (int) ((height * scale) + 0.5f);

        final Resources resources = mView.getResources();
        Bitmap bitmap = Bitmap.createBitmap(resources != null ? resources.getDisplayMetrics()
                : null, width > 0 ? width : 1, height > 0 ? height : 1, quality);
        if (bitmap == null) {
            throw new OutOfMemoryError();
        }

        if (resources != null) {
            bitmap.setDensity(resources.getDisplayMetrics().densityDpi);
        }

        Canvas canvas;
        if (attachInfo != null) {
            canvas = attachInfo.mCanvas;
            if (canvas == null) {
                canvas = new Canvas();
            }
            canvas.setBitmap(bitmap);
            // Temporarily clobber the cached Canvas in case one of our children
            // is also using a drawing cache.
            attachInfo.mCanvas = null;
        } else {
            // This case should hopefully never or seldom happen
            canvas = new Canvas(bitmap);
        }

        if ((backgroundColor & 0xff000000) != 0) {
            bitmap.eraseColor(backgroundColor);
        }

        mView.computeScroll();
        final int restoreCount = canvas.save();
        canvas.scale(scale, scale);
        canvas.translate(-mView.mScrollX, -mView.mScrollY);

        // Temporarily remove the dirty mask
        int flags = mView.mPrivateFlags;
        mView.mPrivateFlags &= ~View.PFLAG_DIRTY_MASK;

        // Fast path for layouts with no backgrounds
        if ((mView.mPrivateFlags & View.PFLAG_SKIP_DRAW) == View.PFLAG_SKIP_DRAW) {
            mView.dispatchDraw(canvas);
            if (mView.mOverlay != null && !mView.mOverlay.isEmpty()) {
                mView.mOverlay.getOverlayView().draw(canvas);
            }
        } else {
            mView.draw(canvas);
        }

        mView.mPrivateFlags = flags;

        canvas.restoreToCount(restoreCount);
        canvas.setBitmap(null);

        if (attachInfo != null) {
            // Restore the cached Canvas for our siblings
            attachInfo.mCanvas = canvas;
        }

        return bitmap;
    }
}
