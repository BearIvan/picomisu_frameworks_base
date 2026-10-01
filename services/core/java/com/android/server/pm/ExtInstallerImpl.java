// Copyright 2026 Picomisu contributors
// SPDX-License-Identifier: Apache-2.0
package com.android.server.pm;

import com.android.server.FgThread;

import java.util.ArrayList;

/**
 * PICO installer extension (factory PICO OS 5.13.7 com.android.server.pm.ExtInstallerImpl).
 * Listeners are kept and called on the FgThread; they are called newest first after every
 * successful connection to installd (ExtPackageManagerServiceImpl uses this to retry creating
 * app DE data that failed before installd was available).
 */
public class ExtInstallerImpl implements IExtInstaller {
    private Installer mBase;
    private ArrayList<IInstalldConnectSuccessListener> mInstalldConnectSuccessListeners =
            new ArrayList<>();

    public ExtInstallerImpl(Installer installer) {
        mBase = installer;
    }

    @Override
    public void dispatchConnectSuccess() {
        FgThread.getHandler().post(() -> {
            for (int i = mInstalldConnectSuccessListeners.size() - 1; i >= 0; i--) {
                try {
                    mInstalldConnectSuccessListeners.get(i).connectSuccess();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    @Override
    public void registerIInstalldConnectSuccessListener(
            final IInstalldConnectSuccessListener listener) {
        FgThread.getHandler().post(() -> {
            if (!mInstalldConnectSuccessListeners.contains(listener)) {
                mInstalldConnectSuccessListeners.add(listener);
            }
        });
    }
}
