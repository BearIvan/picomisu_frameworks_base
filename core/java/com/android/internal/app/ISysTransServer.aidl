// Copyright 2026 Picomisu contributors
// SPDX-License-Identifier: Apache-2.0
// Reconstructed from the factory PICO OS 5.13.7 DEX by tools/reconstruct-pico-aidl.py.
package com.android.internal.app;

/** @hide */
interface ISysTransServer {
    oneway void requestChangeDisplayFps(int pid, int mode, int requestType);
    oneway void notifyDisplayFpsResult(int oldMode, int newMode, int result);
    oneway void notifyDisplayTpResult(int oldMode, int newMode, int result);
    int[] getCalculationConfig();
    oneway void setProcessRunningCpuset(int pid, int cpusetLevel, long timeOut, boolean force);
    oneway void notifyVirtualDisplaySurfaceChanged(int displayId, String surfaceName);
}
