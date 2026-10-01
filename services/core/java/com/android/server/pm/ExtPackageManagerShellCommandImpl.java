// Copyright 2026 Picomisu contributors
// SPDX-License-Identifier: Apache-2.0
package com.android.server.pm;

import android.content.Intent;

/**
 * PICO package manager shell command extension (factory PICO OS 5.13.7
 * com.android.server.pm.ExtPackageManagerShellCommandImpl): {@code pm set-home-activity}
 * starts the com.pvr.vrdisplay service with action pvr.intent.action.vrdisplay, the new home
 * (package or component, as given) in "pkgname" and action_type 10001.
 */
public class ExtPackageManagerShellCommandImpl implements IExtPackageManagerShellCommand {
    private PackageManagerShellCommand mBase;

    public ExtPackageManagerShellCommandImpl(PackageManagerShellCommand base) {
        mBase = base;
    }

    @Override
    public void notifyHomeChanges(String name) {
        Intent intent = new Intent("pvr.intent.action.vrdisplay");
        intent.setPackage("com.pvr.vrdisplay");
        intent.putExtra("pkgname", name);
        intent.putExtra("action_type", 10001);
        PackageManagerService pms = (PackageManagerService) mBase.mInterface;
        if (pms.mContext != null) {
            pms.mContext.startService(intent);
        }
    }
}
