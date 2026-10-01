// Copyright 2026 Picomisu contributors
// SPDX-License-Identifier: Apache-2.0
package com.android.server.locksettings;

import com.pico.util.IExtBase;

/**
 * PICO lock settings shell command extension (factory PICO OS 5.13.7
 * com.android.server.locksettings.IExtLockSettingsShellCommand).
 */
public interface IExtLockSettingsShellCommand extends IExtBase {
    /** Whether "cmd lock_settings set-pattern/set-password/set-pin" must do nothing. */
    default boolean disableAdbSetPassword() {
        return false;
    }
}
