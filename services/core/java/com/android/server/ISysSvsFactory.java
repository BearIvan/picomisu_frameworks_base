// Copyright 2026 Picomisu contributors
// SPDX-License-Identifier: Apache-2.0

package com.android.server;

import com.android.server.am.ActivityManagerService;
import com.android.server.am.IBatteryStatsServiceOptEx;
import com.android.server.am.IHandleMemoryLeak;
import com.android.server.am.IMemoryProcessController;
import com.android.server.am.ITaskDeepClean;

/**
 * Factory of the Smartisan system service optimizations implemented by the optional sys
 * services JAR ({@link SysOptBridge}). Reconstructed from the PICO OS 5.13.7 factory services;
 * only the getters reached by the ported factory code are present, with their factory default
 * implementations.
 *
 * @hide
 */
public interface ISysSvsFactory extends ISysMonitorSvcFactory {
    default ITaskDeepClean getTaskDeepClean() {
        return new ITaskDeepClean() {};
    }

    default IActivityManagerOptEx getActivityManager(ActivityManagerService service) {
        return new IActivityManagerOptEx() {};
    }

    default ISmartService getSmartService() {
        return new ISmartService() {};
    }

    default IMemoryProcessController getMemoryProcessController() {
        return new IMemoryProcessController() {};
    }

    default IMemMonitor getMemMonitor() {
        return new IMemMonitor() {};
    }

    default ISmtResourceControl getSmtResourceControl() {
        return new ISmtResourceControl() {};
    }

    default IBatteryStatsServiceOptEx getBatteryStatsServiceOptEx() {
        return new IBatteryStatsServiceOptEx() {};
    }

    default ISysPrefetchService getSysPrefetchService() {
        return new ISysPrefetchService() {};
    }

    default IHandleMemoryLeak getHandleMemoryLeak() {
        return new IHandleMemoryLeak() {};
    }

    default IPrefetchManagerService getPrefetchManager() {
        return new IPrefetchManagerService() {};
    }
}
