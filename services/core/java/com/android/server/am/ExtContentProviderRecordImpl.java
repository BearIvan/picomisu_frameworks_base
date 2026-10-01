// Copyright 2026 Picomisu contributors
// SPDX-License-Identifier: Apache-2.0
package com.android.server.am;

import android.content.ComponentName;

/**
 * PICO content provider record extension (factory PICO OS 5.13.7
 * com.android.server.am.ExtContentProviderRecordImpl): providers of root and system uid need
 * no release by their clients, except those of the AOSP and PICO settings, the PICO activity
 * center and the PICO store.
 * @hide
 */
public class ExtContentProviderRecordImpl implements IExtContentProviderRecord {
    private ContentProviderRecord mBase;

    public ExtContentProviderRecordImpl(ContentProviderRecord base) {
        mBase = base;
    }

    @Override
    public boolean isNoReleaseNeededClient(int uid, ComponentName name,
            boolean oldNoReleaseNeeded) {
        return (uid == 0 || uid == 1000)
                && (name == null || !("com.android.settings".equals(name.getPackageName())
                        || "com.picovr.settings".equals(name.getPackageName())
                        || "com.picovr.activitycenter".equals(name.getPackageName())
                        || "com.picovr.store".equals(name.getPackageName())));
    }
}
