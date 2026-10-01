// Copyright 2026 Picomisu contributors
// SPDX-License-Identifier: Apache-2.0

package com.android.server;

import android.os.DebugSmtEx;

/**
 * Reconstructed from the PICO OS 5.13.7 factory services.
 *
 * @hide
 */
public interface ISmtResourceControl {
    public static final int CPUSET_LEVEL_CLUSTER_BIG = 3;
    public static final int CPUSET_LEVEL_CLUSTER_SUPER = 4;
    public static final int CPUSET_LEVEL_COMPOSITOR = 5;
    public static final int CPUSET_LEVEL_FG = 2;
    public static final int CPUSET_LEVEL_MAX = 6;
    public static final int CPUSET_LEVEL_NORMAL = 0;
    public static final int CPUSET_LEVEL_TOP = 1;
    public static final long LAUNCH_CPUSET_EFFECTIVE_TIME = 2000;
    public static final int LAUNCH_CPUSET_LEVEL_FG = 2;
    public static final int LAUNCH_CPUSET_LEVEL_NORMAL = 0;
    public static final int LAUNCH_CPUSET_LEVEL_TOP = 1;
    public static final long RUNNING_CPUSET_EFFECTIVE_TIME = 3600000;
    public static final int RUNNING_CPUSET_LEVEL_CLUSTER_BIG = 3;
    public static final int RUNNING_CPUSET_LEVEL_CLUSTER_SUPER = 4;
    public static final int RUNNING_CPUSET_LEVEL_COMPOSITOR = 5;
    public static final int RUNNING_CPUSET_LEVEL_FG = 2;
    public static final int RUNNING_CPUSET_LEVEL_NORMAL = 0;
    public static final int RUNNING_CPUSET_LEVEL_TOP = 1;
    public static final int SCENES_LAUNCH = 1;
    public static final int SCENES_RUNNING = 2;

    default void setUidLaunchCpuset(int pid, int uid, int cpusetLevel) {
        DebugSmtEx.printDefaultFunInfo(getClass());
    }

    default void selectLaunchCpusetStatus(int pid, int uid, boolean launchStart) {
        DebugSmtEx.printDefaultFunInfo(getClass());
    }

    default void setProcessRunningCpuset(int pid, int cpusetLevel, long timeOut, boolean force) {
        DebugSmtEx.printDefaultFunInfo(getClass());
    }
}
