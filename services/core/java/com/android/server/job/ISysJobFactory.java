// Copyright 2026 Picomisu contributors
// SPDX-License-Identifier: Apache-2.0

package com.android.server.job;

import com.android.server.job.controllers.IBatteryControllerOptEx;

/**
 * Factory of the Smartisan job scheduler optimizations implemented by the optional sys services
 * JAR ({@link SysOptJobBridge}). Reconstructed from the PICO OS 5.13.7 factory services, with
 * its factory default implementations.
 *
 * @hide
 */
public interface ISysJobFactory {
    default IJobSchedulerServiceOptEx getJSSOptEx() {
        return new IJobSchedulerServiceOptEx() {};
    }

    default IBatteryControllerOptEx getBatteryControllerOptEx() {
        return new IBatteryControllerOptEx() {};
    }
}
