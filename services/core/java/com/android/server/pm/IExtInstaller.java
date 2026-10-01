// Copyright 2026 Picomisu contributors
// SPDX-License-Identifier: Apache-2.0
package com.android.server.pm;

import com.pico.util.IExtBase;

/**
 * PICO installer extension (factory PICO OS 5.13.7 com.android.server.pm.IExtInstaller):
 * listeners notified each time {@link Installer#connect} reaches installd.
 */
public interface IExtInstaller extends IExtBase {
    void dispatchConnectSuccess();

    void registerIInstalldConnectSuccessListener(IInstalldConnectSuccessListener listener);
}
