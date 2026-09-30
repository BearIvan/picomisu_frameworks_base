// Copyright 2026 Picomisu contributors
// SPDX-License-Identifier: Apache-2.0

package com.android.internal.os;

/**
 * Smartisan {@link BatterySipper} helpers. Reconstructed from the PICO OS 5.13.7 factory
 * framework.
 *
 * @hide
 */
public class BatterySipperSmt {
    public static String getPkg(BatterySipper bs) {
        if (bs instanceof BatterySipperSmtEx) {
            return ((BatterySipperSmtEx) bs).mPkg;
        }
        return "";
    }
}
