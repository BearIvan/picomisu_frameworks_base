// Copyright 2026 Picomisu contributors
// SPDX-License-Identifier: Apache-2.0
package com.android.server.pm;

import com.pico.util.IExtBase;

/**
 * PICO package manager shell command extension (factory PICO OS 5.13.7
 * com.android.server.pm.IExtPackageManagerShellCommand).
 */
public interface IExtPackageManagerShellCommand extends IExtBase {
    void notifyHomeChanges(String name);
}
