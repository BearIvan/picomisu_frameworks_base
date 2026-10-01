// Copyright 2026 Picomisu contributors
// SPDX-License-Identifier: Apache-2.0

package com.android.server.am;

import android.content.Context;
import android.os.DebugSmtEx;

/**
 * Reconstructed from the PICO OS 5.13.7 factory services.
 *
 * @hide
 */
public interface IBatteryStatsServiceOptEx {
    default void init(Context context, BatteryStatsService service) {
        DebugSmtEx.printDefaultFunInfo(getClass());
    }
}
