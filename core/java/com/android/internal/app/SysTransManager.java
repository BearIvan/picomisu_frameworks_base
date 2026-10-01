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

package com.android.internal.app;

import android.os.IBinder;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.util.Singleton;

/**
 * Client of the PICO "systransserver" service (implemented by the factory sys-services.jar
 * SysTransServer). Factory PICO OS 5.13.7 framework.jar class.
 *
 * @hide
 */
public class SysTransManager {
    private static final Singleton<ISysTransServer> ISysTransManagerSingleton =
            new Singleton<ISysTransServer>() {
                @Override
                protected ISysTransServer create() {
                    final IBinder b = ServiceManager.getService("systransserver");
                    final ISysTransServer server = ISysTransServer.Stub.asInterface(b);
                    return server;
                }
            };

    SysTransManager() {
    }

    public static ISysTransServer getService() {
        return ISysTransManagerSingleton.get();
    }

    public static void notifyVirtualDisplaySurfaceChanged(int displayId, String surfaceName) {
        try {
            getService().notifyVirtualDisplaySurfaceChanged(displayId, surfaceName);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }
}
