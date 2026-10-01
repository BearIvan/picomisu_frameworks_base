// Copyright 2026 Picomisu contributors
// SPDX-License-Identifier: Apache-2.0

package com.android.server;

import android.content.Context;
import android.os.DebugSmtEx;
import android.os.Parcel;
import android.os.RemoteException;

/**
 * Reconstructed from the PICO OS 5.13.7 factory services.
 *
 * @hide
 */
public interface IBatteryServiceOptEx {
    default void init(Context context, BatteryService batteryService) {
        DebugSmtEx.printDefaultFunInfo(getClass());
    }

    default void initPowerMonitor() {
        DebugSmtEx.printDefaultFunInfo(getClass());
    }

    default void notifyBatteryChange() {
        DebugSmtEx.printDefaultFunInfo(getClass());
    }

    default boolean onTransactOptEx(int code, Parcel data, Parcel reply, int pid, int uid) throws RemoteException {
        DebugSmtEx.printDefaultFunInfo(getClass());
        return false;
    }
}
