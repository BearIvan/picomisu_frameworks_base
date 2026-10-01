// Copyright 2026 Picomisu contributors
// SPDX-License-Identifier: Apache-2.0

package android.app;

import com.android.internal.app.IBatteryStats;

/**
 * Reconstructed from the PICO OS 5.13.7 factory framework.
 *
 * @hide
 */
public abstract class ActivityManagerInternalSmtBase {
    public abstract IBatteryStats getBatteryStatsService();
}
