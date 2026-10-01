// Copyright 2026 Picomisu contributors
// SPDX-License-Identifier: Apache-2.0
package com.android.server.wm;

import android.pico.utils.Features;

/**
 * PICO task change notification extension (factory PICO OS 5.13.7
 * com.android.server.wm.ExtTaskChangeNotificationControllerImpl): no "app may not work in
 * split screen / on secondary display" notification for tasks on virtual displays.
 */
public class ExtTaskChangeNotificationControllerImpl implements
        IExtTaskChangeNotificationController {
    private TaskChangeNotificationController mBase;

    public ExtTaskChangeNotificationControllerImpl(TaskChangeNotificationController base) {
        mBase = base;
    }

    @Override
    public boolean disableNotifyActivityForcedResizable(ActivityStackSupervisor stackSupervisor,
            int taskId) {
        boolean isDefaultDisplay = true;
        TaskRecord task = stackSupervisor.mRootActivityContainer.anyTaskForId(taskId, 0);
        if (task != null && task.getStack() != null
                && task.getStack().getDisplay().mDisplay.getType() == 5) {
            isDefaultDisplay = false;
        }
        return Features.disableShowInAuxiliaryDisplayToast(isDefaultDisplay);
    }
}
