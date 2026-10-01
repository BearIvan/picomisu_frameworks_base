// Copyright 2026 Picomisu contributors
// SPDX-License-Identifier: Apache-2.0
package com.android.server.wm;

import com.pico.util.IExtBase;

/**
 * PICO activity stack supervisor extension (factory PICO OS 5.13.7
 * com.android.server.wm.IExtActivityStackSupervisor).
 * @hide
 */
public interface IExtActivityStackSupervisor extends IExtBase {
    void setFocusDisplay(ActivityRecord topResumedActivity);
}
