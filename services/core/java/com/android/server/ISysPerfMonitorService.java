// Copyright 2026 Picomisu contributors
// SPDX-License-Identifier: Apache-2.0

package com.android.server;

/**
 * Smartisan system performance monitor service implemented by the optional sysmonitor services
 * JAR ({@link ISysMonitorSvcFactory#getSysPerfMonitorService()}). Reconstructed from the
 * PICO OS 5.13.7 factory services; only the methods reached by the ported factory code are
 * present, with their factory default (empty) implementations.
 *
 * @hide
 */
public interface ISysPerfMonitorService {
    default void removePid(int pid) {
    }
}
