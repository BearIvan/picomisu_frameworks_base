// Copyright 2026 Picomisu contributors
// SPDX-License-Identifier: Apache-2.0

package android.app;

import com.android.internal.os.IBatteryStatsHelperOptEx;
import com.android.internal.os.IBatteryStatsImplOptEx;

/**
 * Factory of the Smartisan sysmonitor framework extensions implemented by the optional
 * sysmonitor framework JAR ({@code android.app.SysMonitorFwFactoryImpl}, loaded by
 * {@link SysMonitorFwBridge}). Reconstructed from the PICO OS 5.13.7 factory framework, with
 * the factory default implementations.
 *
 * @hide
 */
public interface ISysMonitorFwFactory {
    default IAnrLogger getAnrLogger() {
        return new IAnrLogger() {};
    }

    default IBatteryStatsImplOptEx getBatteryStatsImpl() {
        return new IBatteryStatsImplOptEx() {};
    }

    default IBatteryStatsHelperOptEx getBSHelperOptEx() {
        return new IBatteryStatsHelperOptEx() {};
    }

    default IBootReceiverOptEx getBootReceiverOptEx() {
        return new IBootReceiverOptEx() {};
    }

    default ILabMonitorCase getLabMonitorCase() {
        return new ILabMonitorCase() {};
    }
}
