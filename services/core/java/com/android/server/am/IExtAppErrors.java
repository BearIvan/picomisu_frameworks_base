// Copyright 2026 Picomisu contributors
// SPDX-License-Identifier: Apache-2.0
package com.android.server.am;

import com.pico.util.IExtBase;

/**
 * PICO app error extension (factory PICO OS 5.13.7 com.android.server.am.IExtAppErrors).
 * @hide
 */
public interface IExtAppErrors extends IExtBase {
    boolean canShowAnrDialog();

    boolean getCrashSilenced(boolean oldCrashSilenced);
}
