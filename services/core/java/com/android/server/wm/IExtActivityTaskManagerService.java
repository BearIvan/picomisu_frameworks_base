// Copyright 2026 Picomisu contributors
// SPDX-License-Identifier: Apache-2.0
package com.android.server.wm;

import android.content.ComponentName;

import com.pico.util.IExtBase;

/**
 * PICO activity task manager service extension (factory PICO OS 5.13.7
 * com.android.server.wm.IExtActivityTaskManagerService).
 * @hide
 */
public interface IExtActivityTaskManagerService extends IExtBase {
    void init();

    boolean isBelongsToDefaultDisplay(ActivityRecord r, int requestedOrientation);

    void onSystemReady();

    void sendResumingActivityMsg(ActivityRecord r);

    void updateActivityUsageStats(ActivityRecord activity, int event);

    void updatePersistentConnection(ComponentName componentName, boolean connect);
}
