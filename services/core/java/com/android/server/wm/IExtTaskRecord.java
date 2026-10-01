// Copyright 2026 Picomisu contributors
// SPDX-License-Identifier: Apache-2.0
package com.android.server.wm;

import android.app.TaskInfo;

import com.pico.util.IExtBase;

/**
 * PICO task record extension (factory PICO OS 5.13.7 com.android.server.wm.IExtTaskRecord).
 * @hide
 */
public interface IExtTaskRecord extends IExtBase {
    String KEY_CALLING_PACKAGE = "callingPackage";

    void fillTaskInfo(TaskInfo info);
}
