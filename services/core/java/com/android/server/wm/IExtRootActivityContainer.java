// Copyright 2026 Picomisu contributors
// SPDX-License-Identifier: Apache-2.0
package com.android.server.wm;

import com.pico.util.IExtBase;

/**
 * PICO root activity container extension (factory PICO OS 5.13.7
 * com.android.server.wm.IExtRootActivityContainer).
 * @hide
 */
public interface IExtRootActivityContainer extends IExtBase {
    void onRemoveChild(ActivityDisplay activityDisplay);
}
