// Copyright 2026 Picomisu contributors
// SPDX-License-Identifier: Apache-2.0

package com.android.server.wm;

/**
 * Smartisan extension state of an {@link ActivityDisplay}. Reconstructed from the PICO OS
 * 5.13.7 factory services (abstract base of {@link ActivityDisplaySmtEx}).
 *
 * @hide
 */
public abstract class ActivityDisplaySmtBase {
    protected static final long mAcquirePerfLastInterval = 300;
    protected static final int mAcquirePerfMaxTimes = 200;
    protected static long mAcquirePerfLockLastTime = 0;
    protected static int mAcquirePerfTimes = 0;
    protected static boolean mPrintSwitch = true;
    protected ActivityDisplay mActivityDisplay;
    protected ActivityTaskManagerService mTaskService;
    /** Top full screen stack of the display, maintained by the Smartisan stack tracking. */
    ActivityStack mTopFullScreenStack;

    public ActivityDisplaySmtBase(ActivityDisplay activityDisplay,
            ActivityTaskManagerService service) {
        mActivityDisplay = activityDisplay;
        mTaskService = service;
    }

    /** Marks the launch of {@code r} as a cold launch (Smartisan flag 16 of its application). */
    void setColdLaunchFlag(ActivityTaskManagerService mTaskService, ActivityRecord r) {
        mTaskService.getSmtEx().setIsColdLaunch(true);
        r.info.applicationInfo.getSmtEx().smartisanFlag |= 16;
    }
}
