// Copyright 2026 Picomisu contributors
// SPDX-License-Identifier: Apache-2.0

package android.content.pm;

import android.content.Context;

/**
 * Smartisan monitor client part of {@link PackageManager}
 * (factory PICO OS 5.13.7 {@code android.content.pm.PackageManagerMonitorEx}; nothing in the
 * factory jars creates it).
 *
 * @hide
 */
public class PackageManagerMonitorEx {
    private PackageManager mPackageManager;
    private final Context mContext;
    private final IPackageManager mIPackageManager;
    private IPackageManagerMonitorEx mIPackageManagerMonitorEx;

    public PackageManagerMonitorEx(PackageManager packageManager, Context context,
            IPackageManager iPackageManager) {
        mPackageManager = packageManager;
        mContext = context;
        mIPackageManager = iPackageManager;
    }

    private IPackageManagerMonitorEx getServiceMonitorEx() {
        return mIPackageManagerMonitorEx;
    }
}
