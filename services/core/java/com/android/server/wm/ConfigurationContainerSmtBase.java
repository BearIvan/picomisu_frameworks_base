// Copyright 2026 Picomisu contributors
// SPDX-License-Identifier: Apache-2.0

package com.android.server.wm;

/**
 * Smartisan extension state of a {@link ConfigurationContainer} (its {@code mSmtEx}, created
 * by {@link ConfigurationContainerSmtBaseFactory}). Reconstructed from the PICO OS 5.13.7
 * factory services.
 *
 * @hide
 */
public class ConfigurationContainerSmtBase {
    protected ConfigurationContainer mConfigurationContainer;

    public ConfigurationContainerSmtBase(ConfigurationContainer configurationContainer) {
        mConfigurationContainer = configurationContainer;
    }
}
