// Copyright 2026 Picomisu contributors
// SPDX-License-Identifier: Apache-2.0

package com.android.server.pm;

import android.util.SparseArray;
import android.util.SparseIntArray;

import java.util.HashSet;

/**
 * Smartisan extension state of the package {@link Settings}. Reconstructed from the PICO OS
 * 5.13.7 factory services; only the members reached by
 * {@code IPackageManagerSmtEx.isTaskPersist} are present.
 *
 * @hide
 */
public class SettingsSmtBase {
    protected static final String TAG = "SettingsSmtEx";

    /** Uids whose tasks persist (queried by PackageManagerServiceMonitorEx.isTaskPersist). */
    static final SparseIntArray mAllTaskPersistUids = new SparseIntArray();

    /** Per user: packages whose tasks persist (Smartisan task-persist configuration). */
    final SparseArray<HashSet<String>> mAllTaskPersistPackages = new SparseArray<>();
    protected Settings mSettings;

    public SettingsSmtBase(Settings settings) {
        mSettings = settings;
    }

    boolean isTaskPersist(String packagename, int userId) {
        HashSet<String> taskperistPackages = mAllTaskPersistPackages.get(userId);
        if (taskperistPackages != null) {
            return taskperistPackages.contains(packagename);
        }
        return false;
    }
}
