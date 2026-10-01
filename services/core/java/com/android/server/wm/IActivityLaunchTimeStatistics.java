// Copyright 2026 Picomisu contributors
// SPDX-License-Identifier: Apache-2.0

package com.android.server.wm;

/**
 * Reconstructed from the PICO OS 5.13.7 factory services.
 *
 * @hide
 */
public interface IActivityLaunchTimeStatistics {
    public static final int LAUNCH_TYPE_COLD_ACTIVITY = 20;
    public static final int LAUNCH_TYPE_COLD_ACTIVITY_FREEZE = 30;
    public static final int LAUNCH_TYPE_COLD_ACTIVITY_PREFETCH_FREEZE = 31;
    public static final int LAUNCH_TYPE_COLD_PROCESS = 10;
    public static final int LAUNCH_TYPE_HOT = 40;
    public static final int LAUNCH_TYPE_HOT_FREEZE = 50;
    public static final int LAUNCH_TYPE_NONE = 0;
    public static final int LAUNCH_TYPE_VR_PREFETCH_HOT = 41;

    default void clearLaunchStepIfPausing(String reason) {
    }

    default void reportLaunchTime(long endTime) {
    }

    default void setLaunchStartTime(long start) {
    }

    default long getLaunchStartTime() {
        return 0L;
    }

    default int getLaunchType() {
        return 0;
    }

    default void setLaunchType(int type) {
    }

    default boolean isColdLaunch() {
        return false;
    }
}
