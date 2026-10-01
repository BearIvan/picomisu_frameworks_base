// Copyright 2026 Picomisu contributors
// SPDX-License-Identifier: Apache-2.0

package com.android.internal.app;

import android.content.Context;
import android.os.PowerManager;

/**
 * PICO extension of {@link ResolverActivity} (PICO OS 5.13.7 factory framework).
 *
 * @hide
 */
public class ExtResolverActivityImpl implements IExtResolverActivity {
    private static final String TAG = "Activity";
    private ResolverActivity mBase;

    public ExtResolverActivityImpl(ResolverActivity base) {
        mBase = base;
    }

    @Override
    public boolean disableStart(Context context) {
        PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        return !pm.isScreenOn();
    }
}
