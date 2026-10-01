// Copyright 2026 Picomisu contributors
// SPDX-License-Identifier: Apache-2.0
package com.android.server;

import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.UserHandle;

/**
 * PICO AlarmManagerService extension (factory PICO OS 5.13.7
 * com.android.server.ExtAlarmManagerServiceImpl): after the clock was advanced to the build
 * time at start-up, TIME_SET is broadcast 2 s later.
 */
public class ExtAlarmManagerServiceImpl implements IExtAlarmManagerService {
    private static final String TAG = "AlarmManagerService";
    private AlarmManagerService mBase;

    public ExtAlarmManagerServiceImpl(AlarmManagerService base) {
        mBase = base;
    }

    @Override
    public void resetSystemBuildTime(Handler handler, final Context context) {
        handler.postDelayed(() -> {
            Intent intent = new Intent(Intent.ACTION_TIME_CHANGED);
            intent.addFlags(Intent.FLAG_RECEIVER_REPLACE_PENDING
                    | Intent.FLAG_RECEIVER_REGISTERED_ONLY_BEFORE_BOOT
                    | Intent.FLAG_RECEIVER_INCLUDE_BACKGROUND
                    | Intent.FLAG_RECEIVER_VISIBLE_TO_INSTANT_APPS);
            context.sendBroadcastAsUser(intent, UserHandle.ALL);
        }, 2000);
    }
}
