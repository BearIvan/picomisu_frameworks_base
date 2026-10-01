// Copyright 2026 Picomisu contributors
// SPDX-License-Identifier: Apache-2.0
package com.android.server.wm;

import com.pico.util.IExtBase;

/**
 * PICO task change notification extension (factory PICO OS 5.13.7
 * com.android.server.wm.IExtTaskChangeNotificationController).
 * @hide
 */
public interface IExtTaskChangeNotificationController extends IExtBase {
    boolean disableNotifyActivityForcedResizable(ActivityStackSupervisor stackSupervisor,
            int taskId);
}
