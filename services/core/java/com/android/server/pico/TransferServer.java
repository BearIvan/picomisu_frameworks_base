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

import android.os.Binder;
import android.os.Parcel;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.util.Slog;

/**
 * Minimal "transferserver" (factory sysmonitor-services.jar TransferServer, interface
 * com.android.internal.app.ITransferServer). The factory performance monitor is not ported;
 * its clients (libsysperfeventmonitor in the tracking service, SurfaceFlinger's SurfaceMonitor,
 * stabd, stationservice) only need the service to exist and answer. Report calls are accepted
 * and dropped; getMuduleEventInfo returns an empty event list ({0}: zero events) because
 * libsysperfeventmonitor dereferences the first element of the returned vector.
 */
public final class TransferServer extends Binder {
    private static final String TAG = "TransferServer";
    private static final String SERVICE = "transferserver";
    private static final String DESCRIPTOR = "com.android.internal.app.ITransferServer";
    /** ITransferServer.getMuduleEventInfo(int moduleCode): float[] */
    private static final int TRANSACTION_GET_MUDULE_EVENT_INFO = 21;

    private static final float[] NO_EVENTS = {0f};

    public static void publish() {
        ServiceManager.addService(SERVICE, new TransferServer());
        Slog.i(TAG, "Published minimal " + SERVICE);
    }

    @Override
    protected boolean onTransact(int code, Parcel data, Parcel reply, int flags)
            throws RemoteException {
        if (code == INTERFACE_TRANSACTION) {
            reply.writeString(DESCRIPTOR);
            return true;
        }
        if (code < FIRST_CALL_TRANSACTION || code > LAST_CALL_TRANSACTION) {
            return super.onTransact(code, data, reply, flags);
        }
        if (reply != null) {
            reply.writeNoException();
            if (code == TRANSACTION_GET_MUDULE_EVENT_INFO) {
                reply.writeFloatArray(NO_EVENTS);
            }
        }
        return true;
    }
}
