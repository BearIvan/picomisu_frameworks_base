// Copyright 2026 Picomisu contributors
// SPDX-License-Identifier: Apache-2.0
package com.android.server.wm;

import android.os.SystemProperties;

/**
 * PICO activity stack supervisor extension (factory PICO OS 5.13.7
 * com.android.server.wm.ExtActivityStackSupervisorImpl).
 */
public class ExtActivityStackSupervisorImpl implements IExtActivityStackSupervisor {
    private ActivityStackSupervisor mBase;

    public ExtActivityStackSupervisorImpl(ActivityStackSupervisor base) {
        mBase = base;
    }

    /**
     * ActivityStackSupervisor.updateTopResumedActivityIfNeeded: publishes the display of the top
     * resumed activity in pvr.focused.display.id (android.app.Dialog shows system dialogs there).
     */
    @Override
    public void setFocusDisplay(ActivityRecord topResumedActivity) {
        if (topResumedActivity != null) {
            int displayId = topResumedActivity.getDisplayId();
            SystemProperties.set("pvr.focused.display.id",
                    displayId == -1 ? String.valueOf(0) : String.valueOf(displayId));
        }
    }
}
