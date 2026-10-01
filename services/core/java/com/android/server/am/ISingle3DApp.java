// Copyright 2026 Picomisu contributors
// SPDX-License-Identifier: Apache-2.0
package com.android.server.am;

import android.os.DebugSmtEx;

import com.android.server.wm.ActivityRecord;

import java.util.ArrayList;

/**
 * Single 3D app policy of the Smartisan sys services JAR (factory PICO OS 5.13.7
 * com.android.server.am.ISingle3DApp; implemented there by Single3DApp, obtained through
 * {@code SysOptBridge.getFactory().getSingle3DApp()}). The defaults do nothing.
 *
 * @hide
 */
public interface ISingle3DApp {
    default void init(ActivityManagerService ams) {
        DebugSmtEx.printDefaultFunInfo(getClass());
    }

    default void updateTopResumedActivity(ActivityRecord r, int displayType) {
        DebugSmtEx.printDefaultFunInfo(getClass());
    }

    default void stopMarkedPkg(String pkg, int uid, ActivityRecord ar) {
        DebugSmtEx.printDefaultFunInfo(getClass());
    }

    default void updatePendingLaunchActivity(ArrayList<String> targetPackages,
            ArrayList<Integer> targetUids, ArrayList<String> sourcePackages,
            ArrayList<Integer> sourceUids) {
        DebugSmtEx.printDefaultFunInfo(getClass());
    }
}
