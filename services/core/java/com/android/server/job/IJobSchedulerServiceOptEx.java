// Copyright 2026 Picomisu contributors
// SPDX-License-Identifier: Apache-2.0

package com.android.server.job;

import android.os.DebugSmtEx;

/**
 * Smartisan job scheduler optimization implemented by the optional sys services JAR
 * ({@link ISysJobFactory#getJSSOptEx()}). Reconstructed from the PICO OS 5.13.7 factory
 * services, with its factory default implementations.
 *
 * @hide
 */
public interface IJobSchedulerServiceOptEx {
    default void init(JobSchedulerService service) {
        DebugSmtEx.printDefaultFunInfo(getClass());
    }
}
