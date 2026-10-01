// Copyright 2026 Picomisu contributors
// SPDX-License-Identifier: Apache-2.0

package android.content.res;

import android.content.pm.ApplicationInfoSmtBase;

/**
 * Smartisan extension of {@link CompatibilityInfo}: starting window type/flags and TNT mode
 * (factory PICO OS 5.13.7 {@code android.content.res.CompatibilityInfoSmtEx}).
 *
 * @hide
 */
public class CompatibilityInfoSmtEx {
    private CompatibilityInfo mInfo;
    int mSmartisanFlag;
    String mSmartisanPreviewStartingWindowType;
    private boolean mIsTNTMode = false;
    private int mSmtStartingWindowType = -1;
    private int mSmtStartingWindowFlag = 0;

    public CompatibilityInfoSmtEx(CompatibilityInfo info, ApplicationInfoSmtBase appInfoSmtEx) {
        mInfo = info;
        mSmartisanFlag = appInfoSmtEx.smartisanFlag;
        mSmartisanPreviewStartingWindowType = appInfoSmtEx.smtStartingWindowType;
    }

    public boolean isTNTMode() {
        return mIsTNTMode;
    }

    public void setTNTMode(boolean TNTMode) {
        mIsTNTMode = TNTMode;
    }

    public int getAppInfoSmartisanFlag() {
        return mSmartisanFlag;
    }

    public String getSmartisanPreviewStartingWindowType() {
        return mSmartisanPreviewStartingWindowType;
    }

    public void setSmtStartingWindowType(int startingWindowType) {
        mSmtStartingWindowType = startingWindowType;
    }

    public int getSmtStartingWindowType() {
        return mSmtStartingWindowType;
    }

    public int getSmtStartingWindowFlag() {
        return mSmtStartingWindowFlag;
    }

    public void setSmtStartingWindowFlag(int mSmtStartingWindowFlag) {
        this.mSmtStartingWindowFlag = mSmtStartingWindowFlag;
    }
}
