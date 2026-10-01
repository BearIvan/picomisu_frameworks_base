// Copyright 2026 Picomisu contributors
// SPDX-License-Identifier: Apache-2.0

package com.smartisanos.server;

import android.content.ComponentName;
import com.android.server.wm.ActivityStack;

/**
 * Reconstructed from the PICO OS 5.13.7 factory services. Nothing in the factory services,
 * sys-services or sysmonitor-services references it; it is carried for parity.
 *
 * @hide
 */
public interface IRootActivityContainerSmtExt {
    default boolean isTaskVisible(int excludedDisplayId, int taskId) {
        return false;
    }

    default int resolveVisibleTask(int excludedDisplayId, ComponentName componentName) {
        return -1;
    }

    default ActivityStack getTopDisplayFocusedStackIncludingMirror() {
        return null;
    }
}
