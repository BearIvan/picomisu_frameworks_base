// Copyright 2026 Picomisu contributors
// SPDX-License-Identifier: Apache-2.0

package com.android.server.wm;

import com.android.server.am.SysMonitorSvcBridge;

/**
 * Reconstructed from the PICO OS 5.13.7 factory services.
 *
 * @hide
 */
public class ActivityRecordMonitorEx {
    public IActivityLaunchTimeStatistics launchTimeStatistics;
    private ActivityRecord mActivityRecord;

    public ActivityRecordMonitorEx(ActivityRecord activityRecord) {
        this.mActivityRecord = activityRecord;
        this.launchTimeStatistics = SysMonitorSvcBridge.getFactory().getActivityLaunchTimeStatistics(activityRecord);
    }
}
