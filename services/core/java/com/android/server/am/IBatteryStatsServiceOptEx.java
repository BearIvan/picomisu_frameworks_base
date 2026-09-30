// Copyright 2026 Picomisu contributors
// SPDX-License-Identifier: Apache-2.0

package com.android.server.am;

/**
 * Smartisan battery stats service optimization implemented by the optional sys services JAR.
 * Reconstructed from the PICO OS 5.13.7 factory services; none of its methods is reached by
 * the ported factory code. Note that the factory default implementation returned by
 * {@link com.android.server.ISysSvsFactory#getBatteryStatsServiceOptEx} does not implement
 * {@link com.android.internal.app.IBatteryStatsOptEx}.
 *
 * @hide
 */
public interface IBatteryStatsServiceOptEx {
}
