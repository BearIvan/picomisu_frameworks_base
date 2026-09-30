// Copyright 2026 Picomisu contributors
// SPDX-License-Identifier: Apache-2.0

package com.android.server;

/**
 * Factory of the Smartisan multi-platform service optimizations
 * ({@code com.android.server.MultiPlatFactoryImpl}, loaded by
 * {@link SysOptBridge#getMultiPlatFactory()}). Reconstructed from the PICO OS 5.13.7 factory
 * services; as in the factory, it has no default implementation.
 *
 * @hide
 */
public interface IMultiPlatSvsFactory {
    IBoostFrameworkOptEx getBoostFramework();

    IBoostFrameworkOptEx getBoostFrameworkByPerf();

    IBoostNetwork getBoostNetwork();
}
