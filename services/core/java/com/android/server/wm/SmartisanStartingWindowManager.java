// Copyright 2026 Picomisu contributors
// SPDX-License-Identifier: Apache-2.0

package com.android.server.wm;

import android.annotation.TargetApi;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.os.IBinder;
import android.os.Process;
import android.os.SystemClock;
import android.view.View;

import smartisanos.util.FeatLog;

/**
 * Caches the screenshots of the Smartisan blurred starting window
 * ({@link BlurStartingWindowUtils}). Reconstructed from the factory PICO OS 5.13.7 services.
 *
 * @hide
 */
public class SmartisanStartingWindowManager {
    private static final SmartisanStartingWindowManager single =
            new SmartisanStartingWindowManager();

    private SmartisanStartingWindowManager() {
    }

    public static SmartisanStartingWindowManager getInstance() {
        return single;
    }

    public void cacheStartingWindow(final String packageName, View view,
            final String folderType) {
        try {
            if (!"".equals(BlurStartingWindowUtils.checkFileExist(packageName,
                    BlurStartingWindowUtils.TYPE_DEFAULT_STARTING_WINDOW))) {
                return;
            }
            final Bitmap srcBm = view.getSmtEx().createSnapshot(Bitmap.Config.ARGB_8888, 0,
                    false, 0.5f);
            if (srcBm != null && !srcBm.isRecycled()) {
                try {
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            boolean isAllBlack = BlurStartingWindowUtils.checkImageAllBlack(srcBm);
                            if (isAllBlack) {
                                srcBm.recycle();
                                return;
                            }
                            BlurStartingWindowUtils.saveScreenshotBitmap(packageName, srcBm,
                                    BlurStartingWindowUtils.COMPRESS_RATIO, folderType);
                            if (srcBm != null && !srcBm.isRecycled()) {
                                srcBm.recycle();
                            }
                        }
                    }).start();
                } catch (Exception e) {
                    FeatLog.e(BlurStartingWindowUtils.TAG, "FEAT_STARTING_WINDOW", 0,
                            "save screenshot bitmap exception", e);
                    if (!srcBm.isRecycled()) {
                        srcBm.recycle();
                    }
                }
            } else {
                FeatLog.e(BlurStartingWindowUtils.TAG, "FEAT_STARTING_WINDOW", 0,
                        "bitmap null or recycled " + packageName + ", return");
            }
        } catch (Exception e) {
            FeatLog.e(BlurStartingWindowUtils.TAG, "FEAT_STARTING_WINDOW", 0,
                    "view create snapshot exception", e);
        }
    }

    void clearStartingWindowFiles() {
        try {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    BlurStartingWindowUtils.deleteAllBswFiles();
                }
            }).start();
        } catch (Exception e) {
            FeatLog.e(BlurStartingWindowUtils.TAG, "FEAT_STARTING_WINDOW", 0,
                    "clearStartingWindowFiles() exception", e);
        }
    }

    @TargetApi(3)
    public void cacheFirstFrame(final String packageName, final String folderType,
            final AppWindowToken appToken, final boolean isExtDisplay,
            final IBinder displayToken) {
        AsyncTask<Void, Void, Void> task = new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... voids) {
                Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND);
                long startTime = SystemClock.elapsedRealtime();
                try {
                    Thread.sleep(BlurStartingWindowUtils.SLEEP_TIME);
                } catch (Exception e) {
                    FeatLog.e(BlurStartingWindowUtils.TAG, "FEAT_STARTING_WINDOW", 0,
                            "Thread.sleep() exception", e);
                }
                synchronized (single) {
                    if (!"".equals(BlurStartingWindowUtils.checkFileExist(packageName,
                            isExtDisplay
                                    ? BlurStartingWindowUtils.TYPE_DEFAULT_TNT_STARTING_WINDOW
                                    : BlurStartingWindowUtils.TYPE_DEFAULT_STARTING_WINDOW))) {
                        return null;
                    }
                    if (appToken != null && appToken.getDisplayContent() != null) {
                        float frameScale = 1.0f;
                        if (!isExtDisplay) {
                            frameScale /= BlurStartingWindowUtils.BITMAP_SCALE;
                        }
                        Bitmap bmShot = appToken.getDisplayContent().getDisplayContentSmtBase()
                                .screenshotAppFirstFrame(appToken.token, -1, -1, frameScale,
                                        isExtDisplay, displayToken);
                        if (bmShot == null) {
                            return null;
                        }
                        WindowState windowState = appToken.findMainWindow();
                        if (windowState != null && (windowState.mWinAnimator == null
                                || !windowState.isAnimating())) {
                            boolean isAllBlack = BlurStartingWindowUtils.checkImageAllBlack(bmShot);
                            if (BlurStartingWindowUtils.DEBUG_BSW) {
                                BlurStartingWindowUtils.checkTime(startTime,
                                        "checkImageAllBlack packagename:" + packageName);
                            }
                            if (isAllBlack) {
                                if (BlurStartingWindowUtils.DEBUG_BSW) {
                                    FeatLog.w("WindowManager", "FEAT_STARTING_WINDOW", 0,
                                            "Screenshot " + packageName + " was monochrome!");
                                }
                                bmShot.recycle();
                                return null;
                            }
                            if (!isExtDisplay) {
                                Bitmap afterbm = BlurStartingWindowUtils.fastblur(bmShot,
                                        BlurStartingWindowUtils.BLUR_PARAMETER);
                                BlurStartingWindowUtils.saveScreenshotBitmap(packageName, afterbm,
                                        0, folderType);
                                bmShot.recycle();
                            } else {
                                BlurStartingWindowUtils.saveScreenshotBitmap(packageName, bmShot,
                                        0, folderType);
                            }
                            if (BlurStartingWindowUtils.DEBUG_BSW) {
                                BlurStartingWindowUtils.checkTime(startTime,
                                        "blur bitmap:" + packageName);
                            }
                        } else {
                            // As on the factory: the string concatenation binds tighter than
                            // the null check, so this always logs "true".
                            FeatLog.d("WindowManager", "FEAT_STARTING_WINDOW", 0,
                                    "windowState.isAnimating(): " + windowState == null
                                            ? "null" : "true");
                            bmShot.recycle();
                        }
                    }
                    return null;
                }
            }
        };
        task.execute(new Void[0]);
    }
}
