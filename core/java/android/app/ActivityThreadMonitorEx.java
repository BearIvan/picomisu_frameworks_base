// Copyright 2026 Picomisu contributors
// SPDX-License-Identifier: Apache-2.0

package android.app;

/**
 * Smartisan sysmonitor extension state of an {@link ActivityThread} (its {@code mMonitorEx}).
 * Reconstructed from the PICO OS 5.13.7 factory framework.
 *
 * @hide
 */
public class ActivityThreadMonitorEx {
    public static final int NOTIFY_SWITCH_STATE = 1032;

    private ActivityThread mActivityThread;

    public ActivityThreadMonitorEx(ActivityThread activityThread) {
        mActivityThread = activityThread;
    }
}
