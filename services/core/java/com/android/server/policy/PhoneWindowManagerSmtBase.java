// Copyright 2026 Picomisu contributors
// SPDX-License-Identifier: Apache-2.0

package com.android.server.policy;

import android.content.Context;
import android.content.res.CompatibilityInfo;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.drawable.GradientDrawable;
import android.os.Handler;
import android.os.IBinder;
import android.view.View;
import android.view.WindowManager;
import com.android.internal.policy.PhoneWindow;
import smartisanos.util.FeatLog;

/**
 * Smartisan starting window hooks of {@link PhoneWindowManager} (getISmtEx()). On the factory
 * addSmartisanSplashScreen returns null, so {@code SmtStartingData} falls back to addSplashScreen.
 * Reconstructed from the PICO OS 5.13.7 factory services.
 *
 * @hide
 */
public class PhoneWindowManagerSmtBase {
    static final int LONG_PRESS_HOME_RECENT_SYSTEM_UI = 1;
    static final String TAG = "WindowManager";
    static final GradientDrawable mStartingWindowBg = new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM, new int[]{-1513240, -855310});
    protected Handler mHandler;
    protected PhoneWindowManager mPhoneWindowManager;

    public PhoneWindowManagerSmtBase(PhoneWindowManager manager, Handler handler) {
        this.mPhoneWindowManager = manager;
        this.mHandler = handler;
    }

    public WindowManagerPolicy.StartingSurface addSmartisanSplashScreen(IBinder appToken, String packageName, int theme, CompatibilityInfo compatInfo, CharSequence nonLocalizedLabel, int labelRes, int icon, int logo, int windowFlags, Configuration overrideConfig, int displayId) {
        return null;
    }

    protected void handleCustomizedSmartisanStatusBar(String packageName) {
    }

    protected View handleTntMode(Context context, CompatibilityInfo compatInfo, Bitmap bm, PhoneWindow win, WindowManager.LayoutParams params) {
        return null;
    }

    WindowManagerPolicy.StartingSurface getStartingSurfaceSmt(CompatibilityInfo compatInfo, int flags, View view, IBinder appToken) {
        String swTypeFolder = compatInfo.getSmtEx().getSmartisanPreviewStartingWindowType();
        if (swTypeFolder != null && !"".equals(swTypeFolder)) {
            if ("default".equals(swTypeFolder) && compatInfo.getSmtEx().getSmtStartingWindowType() != 1) {
                compatInfo.getSmtEx().setSmtStartingWindowFlag(flags);
                FeatLog.d(TAG, "FEAT_STARTING_WINDOW", 0, "getStartingSurfaceSmt set flag=" + flags);
            }
            if (view.getParent() != null) {
                return new SmtStartingSurface(view, appToken);
            }
            return null;
        }
        if (view.getParent() != null) {
            return new SplashScreenSurface(view, appToken);
        }
        return null;
    }
}
