// Copyright 2026 Picomisu contributors
// SPDX-License-Identifier: Apache-2.0

package com.android.server.audio;

import android.os.DebugSmtEx;

/**
 * Reconstructed from the PICO OS 5.13.7 factory services.
 *
 * @hide
 */
public interface ISmtMediaMonitorService {
    default void updateInfo(String info, boolean upload) {
        DebugSmtEx.printDefaultFunInfo(getClass());
    }
}
