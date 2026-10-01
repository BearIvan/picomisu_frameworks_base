// Copyright 2026 Picomisu contributors
// SPDX-License-Identifier: Apache-2.0
package com.android.server.am;

import android.content.ComponentName;

import com.pico.util.IExtBase;

/**
 * PICO content provider record extension (factory PICO OS 5.13.7
 * com.android.server.am.IExtContentProviderRecord).
 * @hide
 */
public interface IExtContentProviderRecord extends IExtBase {
    boolean isNoReleaseNeededClient(int uid, ComponentName name, boolean oldNoReleaseNeeded);
}
