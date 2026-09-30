// Copyright 2026 Picomisu contributors
// SPDX-License-Identifier: Apache-2.0

package com.android.server;

import com.android.server.am.IAnrMonitor;
import com.android.server.am.IMemoryStrategy;

/**
 * Factory of the Smartisan system monitor services implemented by the optional sysmonitor
 * services JAR ({@link com.android.server.am.SysMonitorSvcBridge}). Reconstructed from the
 * PICO OS 5.13.7 factory services; only the getters reached by the ported factory code are
 * present, with their factory default implementations.
 *
 * @hide
 */
public interface ISysMonitorSvcFactory {
    default IAnrMonitor getAnrMonitor() {
        return new IAnrMonitor() {};
    }

    default IMemoryStrategy getMemoryStrategy() {
        return new IMemoryStrategy() {};
    }

    default ITransferController getTransferController() {
        return new ITransferController() {};
    }
}
