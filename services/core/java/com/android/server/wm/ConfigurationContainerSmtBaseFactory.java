// Copyright 2026 Picomisu contributors
// SPDX-License-Identifier: Apache-2.0

package com.android.server.wm;

/**
 * Creates the Smartisan extension state of a {@link ConfigurationContainer}. Reconstructed from
 * the PICO OS 5.13.7 factory services; only the container type whose extension is reached by
 * the ported factory code ({@link RootActivityContainer}) gets its specific extension, every
 * other container gets the base {@link ConfigurationContainerSmtBase}.
 *
 * @hide
 */
public class ConfigurationContainerSmtBaseFactory {
    public static ConfigurationContainerSmtBase createSmtEx(ConfigurationContainer container) {
        if (container instanceof RootActivityContainer) {
            return new RootActivityContainerSmtBase((RootActivityContainer) container);
        }
        return new ConfigurationContainerSmtBase(container);
    }
}
