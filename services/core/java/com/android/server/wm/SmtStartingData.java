// Copyright 2026 Picomisu contributors
// SPDX-License-Identifier: Apache-2.0

package com.android.server.wm;

import android.content.res.CompatibilityInfo;
import android.content.res.Configuration;
import com.android.server.policy.WindowManagerPolicy;

/**
 * Starting data of the Smartisan blurred starting window. The factory never constructs it.
 * Reconstructed from the PICO OS 5.13.7 factory services.
 *
 * @hide
 */
public class SmtStartingData extends SplashScreenStartingData {
    private final CompatibilityInfo mCompatInfo;
    private final int mIcon;
    private final int mLabelRes;
    private final int mLogo;
    private final Configuration mMergedOverrideConfiguration;
    private final CharSequence mNonLocalizedLabel;
    private final String mPkg;
    private final int mTheme;
    private final int mWindowFlags;

    SmtStartingData(WindowManagerService service, String pkg, int theme, CompatibilityInfo compatInfo, CharSequence nonLocalizedLabel, int labelRes, int icon, int logo, int windowFlags, Configuration mergedOverrideConfiguration) {
        super(service, pkg, theme, compatInfo, nonLocalizedLabel, labelRes, icon, logo, windowFlags, mergedOverrideConfiguration);
        this.mPkg = pkg;
        this.mTheme = theme;
        this.mCompatInfo = compatInfo;
        this.mNonLocalizedLabel = nonLocalizedLabel;
        this.mLabelRes = labelRes;
        this.mIcon = icon;
        this.mLogo = logo;
        this.mWindowFlags = windowFlags;
        this.mMergedOverrideConfiguration = mergedOverrideConfiguration;
    }

    @Override
    WindowManagerPolicy.StartingSurface createStartingSurface(AppWindowToken atoken) {
        this.mCompatInfo.getSmtEx().getSmartisanPreviewStartingWindowType();
        WindowManagerPolicy.StartingSurface ss = this.mService.mPolicy.getISmtEx().addSmartisanSplashScreen(atoken.token, this.mPkg, this.mTheme, this.mCompatInfo, this.mNonLocalizedLabel, this.mLabelRes, this.mIcon, this.mLogo, this.mWindowFlags, this.mMergedOverrideConfiguration, atoken.getDisplayContent().getDisplayId());
        if (ss == null) {
            return this.mService.mPolicy.addSplashScreen(atoken.token, this.mPkg, this.mTheme, this.mCompatInfo, this.mNonLocalizedLabel, this.mLabelRes, this.mIcon, this.mLogo, this.mWindowFlags, this.mMergedOverrideConfiguration, atoken.getDisplayContent().getDisplayId());
        }
        return ss;
    }

    CompatibilityInfo getCompatibilityInfo() {
        return this.mCompatInfo;
    }
}
