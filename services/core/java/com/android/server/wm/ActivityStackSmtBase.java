// Copyright 2026 Picomisu contributors
// SPDX-License-Identifier: Apache-2.0

package com.android.server.wm;

import android.os.PowerAdvisorInternal;
import android.util.Slog;
import com.android.server.LocalServices;

/**
 * Reconstructed from the PICO OS 5.13.7 factory services.
 *
 * @hide
 */
public class ActivityStackSmtBase extends ConfigurationContainerSmtBase {
    protected static final String TAG_STATES = "ActivityStackSmtBase";
    public boolean isPrefetch;
    protected ActivityStack mActivityStack;
    public final PowerAdvisorInternal mPowerAdvisorInternal;

    public ActivityStackSmtBase(ActivityStack activityStack) {
        super(activityStack);
        this.isPrefetch = false;
        this.mActivityStack = activityStack;
        this.mPowerAdvisorInternal = (PowerAdvisorInternal) LocalServices.getService(PowerAdvisorInternal.class);
    }

    void showStartingWindow(ActivityRecord prev, ActivityRecord next) {
        if (prev.getActivityRecordSmtEx().mIsSmartisanHome && (next.info.applicationInfo.getSmtEx().smartisanFlag & 32) == 1 && !prev.packageName.equals(next.packageName)) {
            Slog.e(TAG_STATES, "setAppStartingWindow prev finish");
            this.mActivityStack.mService.mWindowManager.getSmtEx().setIsStartFromHome(true);
            next.showStartingWindow(prev, true, true);
            this.mActivityStack.mService.mWindowManager.getSmtEx().setIsStartFromHome(false);
        }
    }

    void resetLaunchFlagSmt(ActivityRecord next) {
        next.info.applicationInfo.getSmtEx().smartisanFlag &= -17;
        next.info.applicationInfo.getSmtEx().smartisanFlag &= -33;
    }
}
