// Copyright 2026 Picomisu contributors
// SPDX-License-Identifier: Apache-2.0
package com.android.server.power;

import android.content.Context;
import android.os.IBinder;

import com.android.server.lights.LightsManager;
import com.pico.util.IExtBase;

/**
 * PICO power manager service extension: switch of the proximity-sensor controlled screen
 * (never auto sleep while an allowed application keeps it off).
 * @hide
 */
public interface IExtPowerManagerService extends IExtBase {
    void setSensorControlScreenFeatureState(boolean opened, IBinder appToken,
            String packageName);
    boolean systemReady(Context context, LightsManager lightsManager);
}
