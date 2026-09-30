// Copyright 2026 Picomisu contributors
// SPDX-License-Identifier: Apache-2.0

package android.os;

import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;

/**
 * Local (system server) interface of the Smartisan power advisor, published by
 * {@code com.android.server.power.SmartisanPowerAdvisor} in the optional sys services JAR, or by
 * the default {@code ISysSvsFactory.startSmartisanPowerAdvisor}. Reconstructed from the PICO OS
 * 5.13.7 factory framework.
 *
 * @hide
 */
public abstract class PowerAdvisorInternal {
    public static final String ACTION_SLEEP_MODE_CHANGED =
            "com.smartisanos.action.SLEEP_MODE_CHANGED";

    public abstract int shouldAppKillSkipInDeviceIdle(ApplicationInfo applicationInfo);

    public abstract boolean shouldAppKillSkipInDeviceIdleDrainFast(
            ApplicationInfo applicationInfo);

    public abstract void switchPerfileWhenScreenUpdate(boolean screenOn);

    public abstract void entryPCMode(boolean PCMode);

    public abstract void reportEvent(int eventType, String packageName, int userId);

    public abstract long getAdjustUsedElapsedTime(int userId, String packageName);

    public abstract boolean inPowerCheckBlacklist(String packageName, int uid);

    public abstract void notifyRecentPSPShow(boolean show, boolean switchPerf,
            boolean fromRecent);

    public abstract void notifyPowerAppSwitch(ActivityInfo activityInfo);

    public abstract void notifyLimitedChanged();

    public abstract void updatePowerAdvisorFeatureEnable(String tag, int state);

    public abstract boolean inSleepMode();

    public abstract boolean inDozeMode();
}
