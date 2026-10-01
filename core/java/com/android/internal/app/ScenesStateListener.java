/*
 * Copyright 2026 Picomisu contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package com.android.internal.app;

import java.util.List;

/**
 * Listener of the PICO scene state kept by the system_server API layer (factory PICO OS 5.13.7
 * framework com.android.internal.app.ScenesStateListener): seethrough state, the 3D app shown by
 * the XR runtime and the running 2D apps.
 *
 * @hide
 */
public interface ScenesStateListener {
    void on3dAppDisplayStateChanged(String showing3dApp, int xrRuntimeDisplayState);

    void onRunning2dAppChanged(List<RunningAppInfo> visible2dAppList);

    void onSeethroughStateChanged(int seethroughState);
}
