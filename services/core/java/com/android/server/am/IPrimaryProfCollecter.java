// Copyright 2026 Picomisu contributors
// SPDX-License-Identifier: Apache-2.0

package com.android.server.am;

import android.os.DebugSmtEx;
import android.util.ArraySet;

/**
 * Reconstructed from the PICO OS 5.13.7 factory services.
 *
 * @hide
 */
public interface IPrimaryProfCollecter {
    default void uploadProfIfNeeded(ArraySet<String> pkgs) {
        DebugSmtEx.printDefaultFunInfo(getClass());
    }
}
