// Copyright 2026 Picomisu contributors
// SPDX-License-Identifier: Apache-2.0

package com.smartisanos.server;

import android.view.SurfaceControl;

/**
 * Reconstructed from the PICO OS 5.13.7 factory services. Nothing in the factory services,
 * sys-services or sysmonitor-services references it; it is carried for parity.
 *
 * @hide
 */
public interface ITaskStackSmtExt {
    default void removeImmediately() {
    }

    default int getTaskMirrorDisplay() {
        return -1;
    }

    default void onMirroredToDisplay(SurfaceControl surfaceControl, int displayId) {
    }

    default void onDetachMirror() {
    }

    default boolean isBeingMirrored() {
        return false;
    }

    default SurfaceControl getMirrorSurface() {
        return null;
    }
}
