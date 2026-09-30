// Copyright 2026 Picomisu contributors
// SPDX-License-Identifier: Apache-2.0

package com.android.internal.os;

import android.os.BatteryStats;

/**
 * Smartisan {@link BatterySipper} that carries a package name. Reconstructed from the PICO OS
 * 5.13.7 factory framework.
 *
 * @hide
 */
public class BatterySipperSmtEx extends BatterySipper {
    public String mPkg;

    public BatterySipperSmtEx(DrainType drainType, BatteryStats.Uid uid, double value,
            String pkg) {
        super(drainType, uid, value);
        mPkg = pkg;
    }
}
