// Copyright 2026 Picomisu contributors
// SPDX-License-Identifier: Apache-2.0
package com.android.server.power;

import android.content.Context;
import android.os.IBinder;

import com.android.server.lights.LightsManager;
import com.pico.util.IExtBase;

/**
 * PICO power manager service extension (factory PICO OS 5.13.7
 * com.android.server.power.IExtPowerManagerService), implemented by
 * {@link ExtPowerManagerServiceImpl}.
 * @hide
 */
public interface IExtPowerManagerService extends IExtBase {
    /** readConfigurationLocked: replaces config_allowTheaterModeWakeFromUnplug. */
    boolean allowTheaterModeWakeFromUnplugConfig(
            boolean wakeUpWhenPluggedOrUnpluggedInTheaterModeConfig);

    /** getDesiredScreenPolicyLocked: true = never POLICY_DIM. */
    boolean disableDimPowerState();

    /** Start of updatePowerStateLocked: applies a pending power LED change. */
    void notifyLedStatus();

    /** isWakeLockLevelSupportedInternal: true = PROXIMITY_SCREEN_OFF_WAKE_LOCK unsupported. */
    boolean proximityScreenOffWakeLock();

    /** goToSleepInternal: false = the power manager service skips its own sleep path. */
    boolean proxyGoToSleepInternal(long eventTime, int reason, int flags, int uid,
            PowerManagerService.NativeWrapper nativeWrapper);

    /** wakeUpInternal: true = the wake-up was handled here. */
    boolean proxyWakeUpInternal(long eventTime, int reason, String details, int uid,
            String opPackageName, int opUid);

    void setSensorControlScreenFeatureState(boolean opened, IBinder appToken,
            String packageName);

    /** True = the extension registered the battery receiver in place of the service's own. */
    boolean systemReady(Context context, LightsManager lightsManager);

    /** readConfigurationLocked: replaces config_unplugTurnsOnScreen. */
    boolean unplugTurnsOnScreenConfig(boolean wakeUpWhenPluggedOrUnpluggedConfig);
}
