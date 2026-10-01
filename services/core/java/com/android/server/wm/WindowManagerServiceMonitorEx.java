// Copyright 2026 Picomisu contributors
// SPDX-License-Identifier: Apache-2.0

package com.android.server.wm;

/**
 * Reconstructed from the PICO OS 5.13.7 factory services. Nothing in the factory services,
 * sys-services or sysmonitor-services references it; it is carried for parity.
 *
 * @hide
 */
public class WindowManagerServiceMonitorEx {
    private WindowManagerService mWindowManagerServiceMonitorEx;

    public WindowManagerServiceMonitorEx(WindowManagerService windowManagerService) {
        this.mWindowManagerServiceMonitorEx = windowManagerService;
    }
}
