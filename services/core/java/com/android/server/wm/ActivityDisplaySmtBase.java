// Copyright 2026 Picomisu contributors
// SPDX-License-Identifier: Apache-2.0

package com.android.server.wm;

/**
 * Smartisan extension state of an {@link ActivityDisplay}. Reconstructed from the PICO OS
 * 5.13.7 factory services; only the members reached by the Smartisan
 * {@code IActivityManagerSmtEx} methods are present.
 *
 * @hide
 */
public class ActivityDisplaySmtBase {
    protected ActivityDisplay mActivityDisplay;
    protected ActivityTaskManagerService mTaskService;
    /** Top full screen stack of the display, maintained by the Smartisan stack tracking. */
    ActivityStack mTopFullScreenStack;

    public ActivityDisplaySmtBase(ActivityDisplay activityDisplay,
            ActivityTaskManagerService service) {
        mActivityDisplay = activityDisplay;
        mTaskService = service;
    }
}
