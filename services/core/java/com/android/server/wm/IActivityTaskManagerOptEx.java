// Copyright 2026 Picomisu contributors
// SPDX-License-Identifier: Apache-2.0
package com.android.server.wm;

/**
 * Activity task manager optimization of the Smartisan sys services JAR (factory PICO OS 5.13.7
 * com.android.server.wm.IActivityTaskManagerOptEx, obtained through
 * {@code SysOptBridge.getFactory().getAtmOptEx()}). The defaults do nothing.
 *
 * @hide
 */
public interface IActivityTaskManagerOptEx {
    default void init(ActivityTaskManagerService atmService) {
    }

    default void onProcessFrozen(WindowProcessController proc, int stat) {
    }
}
