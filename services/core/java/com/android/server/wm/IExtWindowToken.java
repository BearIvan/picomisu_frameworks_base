// Copyright 2026 Picomisu contributors
// SPDX-License-Identifier: Apache-2.0
package com.android.server.wm;

import com.pico.util.IExtBase;

/**
 * PICO window token extension (factory PICO OS 5.13.7 com.android.server.wm.IExtWindowToken).
 * @hide
 */
public interface IExtWindowToken extends IExtBase {
    boolean removeAllWindowsIfPossible();
}
