// Copyright 2026 Picomisu contributors
// SPDX-License-Identifier: Apache-2.0

package com.android.server.wm;

/**
 * Creates the Smartisan extension state of a {@link ConfigurationContainer}. Reconstructed from
 * the PICO OS 5.13.7 factory services, in the factory order.
 *
 * @hide
 */
public class ConfigurationContainerSmtBaseFactory {
    public static ConfigurationContainerSmtBase createSmtEx(ConfigurationContainer container) {
        if (container instanceof DisplayContent) {
            return new DisplayContentSmtBase((DisplayContent) container);
        }
        if (container instanceof WindowState) {
            return new WindowStateSmtBase((WindowState) container);
        }
        if (container instanceof AppWindowToken) {
            return new AppWindowTokenSmtBase((AppWindowToken) container);
        }
        if (container instanceof RootWindowContainer) {
            return new RootWindowContainerSmtBase((RootWindowContainer) container);
        }
        if (container instanceof WindowProcessController) {
            return new WindowProcessControllerSmtBase((WindowProcessController) container);
        }
        if (container instanceof WindowContainer) {
            return new WindowContainerSmtBase((WindowContainer) container);
        }
        if (container instanceof ActivityRecord) {
            return new ActivityRecordSmtBase((ActivityRecord) container);
        }
        if (container instanceof ActivityStack) {
            return new ActivityStackSmtBase((ActivityStack) container);
        }
        if (container instanceof RootActivityContainer) {
            return new RootActivityContainerSmtBase((RootActivityContainer) container);
        }
        if (container instanceof TaskRecord) {
            return new TaskRecordSmtBase((TaskRecord) container);
        }
        return new ConfigurationContainerSmtBase(container);
    }
}
