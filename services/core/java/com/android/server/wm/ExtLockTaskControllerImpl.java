// Copyright 2026 Picomisu contributors
// SPDX-License-Identifier: Apache-2.0
package com.android.server.wm;

import java.util.Arrays;
import java.util.List;

/**
 * PICO lock task controller extension (factory PICO OS 5.13.7
 * com.android.server.wm.ExtLockTaskControllerImpl): the seethrough settings app may start
 * while a task is locked.
 */
public class ExtLockTaskControllerImpl implements IExtLockTaskController {
    private static final List<String> sLockTaskModeWhitelist =
            Arrays.asList("com.pvr.seethrough.setting");
    private LockTaskController mBase;

    public ExtLockTaskControllerImpl(LockTaskController base) {
        mBase = base;
    }

    @Override
    public boolean ignoreLockTaskModeCheck(TaskRecord task) {
        if (task == null || task.intent == null) {
            return false;
        }
        if (sLockTaskModeWhitelist.contains(task.intent.getPackage())) {
            return true;
        }
        return task.intent.getComponent() != null
                && sLockTaskModeWhitelist.contains(task.intent.getComponent().getPackageName());
    }
}
