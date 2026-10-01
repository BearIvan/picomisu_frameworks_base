// Copyright 2026 Picomisu contributors
// SPDX-License-Identifier: Apache-2.0
package com.android.server.wm;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;

import com.pico.util.IExtBase;

/**
 * PICO activity starter extension (factory PICO OS 5.13.7
 * com.android.server.wm.IExtActivityStarter).
 * @hide
 */
public interface IExtActivityStarter extends IExtBase {
    boolean helpRequestPermission(ActivityInfo aInfo, Context context, int userId);

    boolean interruptStartActivity(String callingPackage, Intent intent);

    boolean isPrefetchAppRequestPermission(WindowProcessController caller, ActivityInfo aInfo);

    boolean refuseStartLauncher(Intent intent, int callingUid);

    void sendActivityStartingMsg(ActivityInfo aInfo);
}
