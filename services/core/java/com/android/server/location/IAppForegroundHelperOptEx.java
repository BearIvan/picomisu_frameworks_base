// Copyright 2026 Picomisu contributors
// SPDX-License-Identifier: Apache-2.0

package com.android.server.location;

import android.os.DebugSmtEx;

/**
 * Reconstructed from the PICO OS 5.13.7 factory services.
 *
 * @hide
 */
public interface IAppForegroundHelperOptEx {
    default void onSystemReady() {
        DebugSmtEx.printDefaultFunInfo(getClass());
    }
}
