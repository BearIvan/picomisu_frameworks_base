// Copyright 2026 Picomisu contributors
// SPDX-License-Identifier: Apache-2.0
package com.android.server.wm;

import android.content.pm.ActivityInfo;

import com.pico.util.IExtBase;

/**
 * PICO activity task manager internal extension (factory PICO OS 5.13.7
 * com.android.server.wm.IExtActivityTaskManagerInternal).
 * @hide
 */
public interface IExtActivityTaskManagerInternal extends IExtBase {
    ActivityInfo getTopAppExt(int displayId);
}
