// Copyright 2026 Picomisu contributors
// SPDX-License-Identifier: Apache-2.0
package com.android.server.locksettings;

import android.os.Build;
import android.pico.utils.Features;

/**
 * PICO lock settings shell command extension (factory PICO OS 5.13.7
 * com.android.server.locksettings.ExtLockSettingsShellCommandImpl): on user builds with the
 * keyguard enabled, the shell cannot set a lock credential.
 */
public class ExtLockSettingsShellCommandImpl implements IExtLockSettingsShellCommand {
    private LockSettingsShellCommand mBase;

    public ExtLockSettingsShellCommandImpl(LockSettingsShellCommand base) {
        mBase = base;
    }

    @Override
    public boolean disableAdbSetPassword() {
        return !Build.IS_DEBUGGABLE && Features.isKeyguardEnabled();
    }
}
