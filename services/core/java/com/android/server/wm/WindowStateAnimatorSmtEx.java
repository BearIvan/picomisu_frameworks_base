// Copyright 2026 Picomisu contributors
// SPDX-License-Identifier: Apache-2.0

package com.android.server.wm;

import android.content.res.CompatibilityInfo;

/**
 * Smartisan starting window helper of a {@link WindowStateAnimator}. The factory never
 * constructs it.
 * Reconstructed from the PICO OS 5.13.7 factory services.
 *
 * @hide
 */
public class WindowStateAnimatorSmtEx {
    private WindowStateAnimator mWindowStateAnimator;

    public WindowStateAnimatorSmtEx(WindowStateAnimator windowStateAnimator) {
        this.mWindowStateAnimator = windowStateAnimator;
    }

    void stopFreezingScreen() {
        AppWindowToken atoken = this.mWindowStateAnimator.mWin.mAppToken;
        if (atoken != null && (atoken.mStartingData instanceof SmtStartingData)) {
            CompatibilityInfo compatInfo = ((SmtStartingData) atoken.mStartingData).getCompatibilityInfo();
            if (this.mWindowStateAnimator.mWin == atoken.startingWindow && atoken.mStartingData != null && atoken.isFreezingScreen() && compatInfo != null) {
                String startingWindowType = compatInfo.getSmtEx().getSmartisanPreviewStartingWindowType();
                String packageName = this.mWindowStateAnimator.mWin.getOwningPackage();
                if (packageName != null && !"".equals(packageName) && startingWindowType != null && !"".equals(startingWindowType) && !"default".equals(startingWindowType)) {
                    String startingWindowTypePath = BlurStartingWindowUtils.checkFileExist(packageName, startingWindowType);
                    if (!"".equals(startingWindowTypePath)) {
                        atoken.stopFreezingScreen(true, true);
                    }
                }
            }
        }
    }

    WindowSurfaceController getSurfaceControl() {
        return this.mWindowStateAnimator.mSurfaceController;
    }
}
