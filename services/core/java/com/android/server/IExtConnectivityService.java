// Copyright 2026 Picomisu contributors
// SPDX-License-Identifier: Apache-2.0
package com.android.server;

import android.content.Context;
import android.os.Handler;

import com.android.server.connectivity.MockableSystemProperties;
import com.pico.util.IExtBase;

/**
 * PICO ConnectivityService extension (factory PICO OS 5.13.7
 * com.android.server.IExtConnectivityService).
 */
public interface IExtConnectivityService extends IExtBase {
    void init(Context context, Handler handler);

    void setupUniqueDeviceName(Context context, MockableSystemProperties systemProperties);
}
