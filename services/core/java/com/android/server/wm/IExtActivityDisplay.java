// Copyright 2026 Picomisu contributors
// SPDX-License-Identifier: Apache-2.0
package com.android.server.wm;

import android.app.ActivityManager;
import android.view.Display;

import com.pico.util.IExtBase;

/**
 * PICO activity display extension (factory PICO OS 5.13.7
 * com.android.server.wm.IExtActivityDisplay).
 * @hide
 */
public interface IExtActivityDisplay extends IExtBase {
    int INVISIBLE = 2;
    int PENDING_INVISIBLE = 1;
    int PENDING_VISIBLE = 3;
    int VISIBLE = 4;

    boolean checkForTopTaskChanged();

    int getReqOrientation();

    int getTopTaskId();

    ActivityManager.RunningTaskInfo getTopTaskInfo();

    int getVisibility();

    void init(ActivityTaskManagerService atms, Display display);

    boolean isScreenOn();

    boolean isVr2dDisplay();

    void onDisplayChanged();

    void onTaskRemoved(int taskId);

    void setReqOrientation(int reqOrientation);

    void setVisibility(int visibility);
}
