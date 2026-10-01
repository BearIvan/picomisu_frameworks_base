// Copyright 2026 Picomisu contributors
// SPDX-License-Identifier: Apache-2.0

package com.android.internal.app;

import android.content.Context;

import com.pico.util.IExtBase;

/**
 * PICO extension of {@link ResolverActivity} (PICO OS 5.13.7 factory framework).
 *
 * @hide
 */
public interface IExtResolverActivity extends IExtBase {
    /** Whether starting the selected target is disabled (the factory: while the screen is off). */
    boolean disableStart(Context context);
}
