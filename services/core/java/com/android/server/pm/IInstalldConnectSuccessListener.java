// Copyright 2026 Picomisu contributors
// SPDX-License-Identifier: Apache-2.0
package com.android.server.pm;

/**
 * Callback for a (re)connection of the system server to installd (factory PICO OS 5.13.7
 * com.android.server.pm.IInstalldConnectSuccessListener), dispatched by {@link IExtInstaller}.
 */
public interface IInstalldConnectSuccessListener {
    void connectSuccess();
}
