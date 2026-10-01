// Copyright 2026 Picomisu contributors
// SPDX-License-Identifier: Apache-2.0
package com.android.server;

import android.content.Context;
import android.os.Handler;

import com.pico.util.IExtBase;

/**
 * PICO AlarmManagerService extension (factory PICO OS 5.13.7
 * com.android.server.IExtAlarmManagerService).
 */
public interface IExtAlarmManagerService extends IExtBase {
    void resetSystemBuildTime(Handler handler, Context context);
}
