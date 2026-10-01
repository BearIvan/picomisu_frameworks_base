// Copyright 2026 Picomisu contributors
// SPDX-License-Identifier: Apache-2.0

package com.android.server;

import com.android.server.am.ActivityManagerService;
import com.android.server.am.IAnrMonitor;
import com.android.server.am.IBatteryStatsServiceOptEx;
import com.android.server.am.IDumpUtils;
import com.android.server.am.IHandleMemoryLeak;
import com.android.server.am.IMemoryStrategy;
import com.android.server.am.IProcessStatsServiceOptEx;
import com.android.server.am.ISchedLogdPriority;
import com.android.server.am.IUploadUtils;
import com.android.server.power.IPowerManagerMonitorEx;
import com.android.server.power.ISmartPowerData;
import com.android.server.wm.ActivityRecord;
import com.android.server.wm.IActivityLaunchTimeStatistics;

/**
 * Reconstructed from the PICO OS 5.13.7 factory services.
 *
 * @hide
 */
public interface ISysMonitorSvcFactory {
    default IAnrMonitor getAnrMonitor() {
        return new IAnrMonitor() {
        };
    }

    default IMemoryStrategy getMemoryStrategy() {
        return new IMemoryStrategy() {
        };
    }

    default ISysMonitorService getSysMonitorService() {
        return new ISysMonitorService() {
        };
    }

    default ISmartMonitorController getSmartMonitorController() {
        return new ISmartMonitorController() {
        };
    }

    default ISmartService getSmartService() {
        return new ISmartService() {
        };
    }

    default IAtraceStatusMonitor getAtraceStatusMonitor() {
        return new IAtraceStatusMonitor() {
        };
    }

    default ITransferController getTransferController() {
        return new ITransferController() {
        };
    }

    default ISysMonitorExtraLogUtil getSysMonitorExtraLogUtil() {
        return new ISysMonitorExtraLogUtil() {
        };
    }

    default ISmartAnaly getSmtAnalysis() {
        return new ISmartAnaly() {
        };
    }

    default ISysPerfMonitorService getSysPerfMonitorService() {
        return new ISysPerfMonitorService() {
        };
    }

    default IActivityLaunchTimeStatistics getActivityLaunchTimeStatistics(ActivityRecord record) {
        return new IActivityLaunchTimeStatistics() {
        };
    }

    default IProcessStatsServiceOptEx getProcessStatsServiceOptEx() {
        return new IProcessStatsServiceOptEx() {
        };
    }

    default ISmartPowerData getSmartPowerDataInstance() {
        return new ISmartPowerData() {
        };
    }

    default IPowerManagerMonitorEx getPowerManagerMonitor() {
        return new IPowerManagerMonitorEx() {
        };
    }

    default IBatteryStatsServiceOptEx getBatteryStatsServiceOptEx() {
        return new IBatteryStatsServiceOptEx() {
        };
    }

    default IBatteryServiceOptEx getBatteryServiceOptEx() {
        return new IBatteryServiceOptEx() {
        };
    }

    default IActivityManagerOptEx getActivityManager(ActivityManagerService service) {
        return new IActivityManagerOptEx() {
        };
    }

    default IHandleMemoryLeak getHandleMemoryLeak() {
        return new IHandleMemoryLeak() {
        };
    }

    default IDumpUtils getDumpUtils() {
        return new IDumpUtils() {
        };
    }

    default ISchedLogdPriority getSchedLogdPriority() {
        return new ISchedLogdPriority() {
        };
    }

    default IUploadUtils getUploadUtils() {
        return new IUploadUtils() {
        };
    }
}
