/*
 * Copyright (C) 2026 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.server.pico;

import android.os.ServiceManager;
import android.util.Slog;

import com.android.internal.app.ISysTransServer;

/**
 * Minimal "systransserver" (factory sys-services.jar SysTransServer). The factory service fronts
 * the Smartisan SmartRefreshRate and SmtResourceControl layers, which are not ported: the VR
 * runtime owns the refresh rate on the headset. Clients (libgui SurfaceMonitor, libhwui,
 * libsysperftracker) look the service up with a blocking getService, so every VR app start
 * waited 5 s while it was missing. Requests are accepted and dropped; getCalculationConfig
 * returns null like the factory service without a pushed calculation config.
 */
public final class SysTransServer extends ISysTransServer.Stub {
    private static final String TAG = "SysTransServer";
    private static final String SERVICE = "systransserver";

    public static void publish() {
        ServiceManager.addService(SERVICE, new SysTransServer());
        Slog.i(TAG, "Published minimal " + SERVICE);
    }

    @Override
    public void requestChangeDisplayFps(int pid, int mode, int requestType) {
    }

    @Override
    public void notifyDisplayFpsResult(int oldMode, int newMode, int result) {
    }

    @Override
    public void notifyDisplayTpResult(int oldMode, int newMode, int result) {
    }

    @Override
    public int[] getCalculationConfig() {
        return null;
    }

    @Override
    public void setProcessRunningCpuset(int pid, int cpusetLevel, long timeOut, boolean force) {
    }

    @Override
    public void notifyVirtualDisplaySurfaceChanged(int displayId, String surfaceName) {
    }
}
