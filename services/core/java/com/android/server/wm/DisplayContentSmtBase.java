// Copyright 2026 Picomisu contributors
// SPDX-License-Identifier: Apache-2.0

package com.android.server.wm;

import static android.view.Display.DEFAULT_DISPLAY;
import static android.view.Surface.ROTATION_270;
import static android.view.Surface.ROTATION_90;
import static android.view.WindowManager.LayoutParams.TYPE_APPLICATION;
import static android.view.WindowManager.LayoutParams.TYPE_APPLICATION_MEDIA;
import static android.view.WindowManager.LayoutParams.TYPE_APPLICATION_MEDIA_OVERLAY;
import static android.view.WindowManager.LayoutParams.TYPE_BASE_APPLICATION;

import static com.android.server.wm.WindowManagerDebugConfig.DEBUG_SCREENSHOT;
import static com.android.server.wm.WindowManagerDebugConfig.TAG_WM;
import static com.android.server.wm.WindowManagerService.TYPE_LAYER_MULTIPLIER;
import static com.android.server.wm.WindowManagerService.TYPE_LAYER_OFFSET;

import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.os.IBinder;
import android.util.Slog;
import android.view.SurfaceControl;
import android.view.SurfaceControlSmtBase;

import com.android.server.policy.PhoneWindowManager;

/**
 * Smartisan state of a {@link DisplayContent} (DisplayContent.getDisplayContentSmtBase()):
 * first-frame screenshot of an app for the blurred starting window. Reconstructed from the
 * factory PICO OS 5.13.7 services, where the screenshot itself was removed (SurfaceControl
 * has no layer-range screenshot any more), so screenshotAppFirstFrame() always ends with
 * "Screenshot failure" and returns null.
 *
 * @hide
 */
public class DisplayContentSmtBase extends WindowContainerSmtBase {
    public static final String TAG = "DisplayContentSmt";
    protected static final Configuration mTmpConfig = new Configuration();
    protected final String SYSTEM_DIALOG_REASON_RECENT_APPS =
            PhoneWindowManager.SYSTEM_DIALOG_REASON_RECENT_APPS;
    protected final String SYSTEM_DIALOG_REASON_KEYGUARD = "lock";
    private final DisplayContent.ScreenshotApplicationState mScreenshotAppFirstFrameState =
            new DisplayContent.ScreenshotApplicationState();
    protected DisplayContent mDisplayContent;
    protected final DisplayContent.NonAppWindowContainers mDockWindowContainers;

    public DisplayContentSmtBase(DisplayContent displayContent) {
        super(displayContent);
        mDisplayContent = displayContent;
        mDockWindowContainers = mDisplayContent.new NonAppWindowContainers(
                "mDockWindowContainers", mDisplayContent.mWmService);
    }

    protected void handleCaptionBar(WindowState appWin, Rect crop) {
    }

    public Bitmap screenshotAppFirstFrame(final IBinder appToken, int width, int height,
            float frameScale, boolean isExtDisplay, IBinder displayToken) {
        WindowManagerService mService = mDisplayContent.mWmService;
        int dw = mDisplayContent.getDisplayInfo().logicalWidth;
        int dh = mDisplayContent.getDisplayInfo().logicalHeight;
        if (dw == 0 || dh == 0) {
            if (DEBUG_SCREENSHOT) {
                Slog.i(TAG_WM, "Screenshot of " + appToken
                        + ": returning null. logical widthxheight=" + dw + "x" + dh);
            }
            return null;
        }

        Bitmap bm = null;

        mScreenshotAppFirstFrameState.reset(appToken == null);
        final Rect frame = new Rect();

        final int aboveAppLayer = (mService.mPolicy.getWindowLayerFromTypeLw(TYPE_APPLICATION)
                + 1) * TYPE_LAYER_MULTIPLIER + TYPE_LAYER_OFFSET;
        synchronized (mService.mGlobalLock) {
            // Figure out the part of the screen that is actually the app.
            mScreenshotAppFirstFrameState.appWin = null;
            mDisplayContent.forAllWindows(w -> {
                if (!w.mHasSurface) {
                    return false;
                }
                if ((w.mAttrs.type != TYPE_BASE_APPLICATION
                        && w.mAttrs.type != TYPE_APPLICATION_MEDIA
                        && w.mAttrs.type != TYPE_APPLICATION_MEDIA_OVERLAY)
                        || w.mWinAnimator == null) {
                    return false;
                }
                if (w.mLayer >= aboveAppLayer) {
                    return false;
                }
                if (appToken != null) {
                    if (w.mAppToken == null || w.mAppToken.token != appToken) {
                        return false;
                    }
                    mScreenshotAppFirstFrameState.appWin = w;
                }

                final WindowStateAnimator winAnim = w.mWinAnimator;
                int layer = winAnim.mSurfaceController.getLayer();
                if (mScreenshotAppFirstFrameState.maxLayer < layer) {
                    mScreenshotAppFirstFrameState.maxLayer = layer;
                }
                if (mScreenshotAppFirstFrameState.minLayer > layer) {
                    mScreenshotAppFirstFrameState.minLayer = layer;
                }

                final TaskStack stack = w.getStack();
                if (stack != null) {
                    stack.getBounds(frame);
                }
                frame.intersect(w.getFrameLw());

                final boolean foundTargetWs =
                        (w.mAppToken != null && w.mAppToken.token == appToken);
                if (foundTargetWs && winAnim.getShown()) {
                    mScreenshotAppFirstFrameState.screenshotReady = true;
                }

                // Factory form (explicit true/false returns).
                if (w.isObscuringDisplay()) {
                    return true;
                }
                return false;
            }, true /* traverseTopToBottom */);

            final WindowState appWin = mScreenshotAppFirstFrameState.appWin;
            final boolean screenshotReady = mScreenshotAppFirstFrameState.screenshotReady;
            final int maxLayer = mScreenshotAppFirstFrameState.maxLayer;
            final int minLayer = mScreenshotAppFirstFrameState.minLayer;

            if (appToken != null && appWin == null) {
                // Can't find a window to snapshot.
                if (DEBUG_SCREENSHOT) {
                    Slog.i(TAG_WM, "Screenshot: Couldn't find a surface matching " + appToken);
                }
                return null;
            }

            if (!screenshotReady) {
                Slog.i(TAG_WM, "Failed to capture screenshot of " + appToken
                        + " appWin=" + (appWin == null ? "null" : (appWin + " drawState="
                        + appWin.mWinAnimator.mDrawState)));
                return null;
            }

            // Screenshot hack: Constrain the layer to ensure we capture properly
            if (maxLayer == 0) {
                if (DEBUG_SCREENSHOT) {
                    Slog.i(TAG_WM, "Screenshot of " + appToken + ": returning null maxLayer="
                            + maxLayer);
                }
                return null;
            }

            // The screenshot API does not apply the current screen rotation.
            int rot = mDisplayContent.getDisplay().getRotation();
            if (rot == ROTATION_90 || rot == ROTATION_270) {
                final int tmp = width;
                width = height;
                height = tmp;
            }

            // Constrain frame to the screen size.
            if (!frame.intersect(0, 0, dw, dh)) {
                frame.setEmpty();
            }
            if (frame.isEmpty()) {
                return null;
            }

            if (width < 0) {
                width = (int) (frame.width() * frameScale);
            }
            if (height < 0) {
                height = (int) (frame.height() * frameScale);
            }

            // Tell surface flinger what part of the image to crop. Take the top
            // right part of the application, and crop the larger dimension to fit.
            Rect crop = new Rect(frame);
            if (width / (float) frame.width() < height / (float) frame.height()) {
                int cropWidth = (int) ((float) width / (float) height * frame.height());
                crop.right = crop.left + cropWidth;
                if (crop.right < frame.width()) {
                    crop.right = frame.width();
                }
            } else {
                int cropHeight = (int) ((float) height / (float) width * frame.width());
                crop.bottom = crop.top + cropHeight;
            }

            if (rot == ROTATION_90 || rot == ROTATION_270) {
                rot = (rot == ROTATION_90) ? ROTATION_270 : ROTATION_90;
            }

            handleCaptionBar(appWin, crop);

            // Surfaceflinger is not aware of orientation, so convert our logical
            // crop to surfaceflinger's portrait orientation.
            mDisplayContent.convertCropForSurfaceFlinger(crop, rot, dw, dh);

            if (DEBUG_SCREENSHOT) {
                Slog.i(TAG_WM, "Screenshot: " + dw + "x" + dh + " from " + minLayer + " to "
                        + maxLayer + " appToken=" + appToken);
                mDisplayContent.forAllWindows(w -> {
                    final WindowSurfaceController controller = w.mWinAnimator.mSurfaceController;
                    Slog.i(TAG_WM, w + ": " + w.mLayer + " surfaceLayer="
                            + ((controller == null) ? "null" : controller.getLayer()));
                }, false /* traverseTopToBottom */);
            }

            final ScreenRotationAnimation screenRotationAnimation =
                    mService.mAnimator.getScreenRotationAnimationLocked(DEFAULT_DISPLAY);
            final boolean inRotation = screenRotationAnimation != null
                    && screenRotationAnimation.isAnimating();
            if (DEBUG_SCREENSHOT && inRotation) {
                Slog.v(TAG_WM, "Taking screenshot while rotating");
            }

            // We force pending transactions to flush before taking
            // the screenshot by pushing an empty synchronous transaction.
            SurfaceControl.openTransaction();
            SurfaceControlSmtBase.closeTransactionSync();

            // Factory: the screenshot call is gone, bm stays null.
            if (bm == null) {
                Slog.w(TAG_WM, "Screenshot failure taking screenshot for (" + dw + "x" + dh
                        + ") to layer " + maxLayer);
                return null;
            }
            return bm;
        }
    }
}
