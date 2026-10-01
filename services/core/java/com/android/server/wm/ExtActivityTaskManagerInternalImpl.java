// Copyright 2026 Picomisu contributors
// SPDX-License-Identifier: Apache-2.0
package com.android.server.wm;

import android.content.pm.ActivityInfo;

/**
 * PICO activity task manager internal extension (factory PICO OS 5.13.7
 * com.android.server.wm.ExtActivityTaskManagerInternalImpl).
 */
public class ExtActivityTaskManagerInternalImpl implements IExtActivityTaskManagerInternal {
    private ActivityTaskManagerService mBase;

    public ExtActivityTaskManagerInternalImpl(ActivityTaskManagerService base) {
        mBase = base;
    }

    /** Info of the resumed activity of a display, or null. */
    @Override
    public ActivityInfo getTopAppExt(int displayId) {
        synchronized (mBase.mGlobalLock) {
            try {
                WindowManagerService.boostPriorityForLockedSection();
                ActivityDisplay activityDisplay =
                        mBase.mRootActivityContainer.getActivityDisplay(displayId);
                if (activityDisplay == null) {
                    return null;
                }
                ActivityRecord top = activityDisplay.getResumedActivity();
                return top != null ? top.info : null;
            } finally {
                WindowManagerService.resetPriorityAfterLockedSection();
            }
        }
    }
}
