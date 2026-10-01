// Copyright 2026 Picomisu contributors
// SPDX-License-Identifier: Apache-2.0
package com.android.server.wm;

import android.content.Context;

import com.pico.util.IExtBase;

/**
 * PICO launch params controller extension (factory PICO OS 5.13.7
 * com.android.server.wm.IExtLaunchParamsController).
 * @hide
 */
public interface IExtLaunchParamsController extends IExtBase {
    void modifyResultDisplayId(Context context, ActivityRecord record,
            LaunchParamsController.LaunchParams result);
}
