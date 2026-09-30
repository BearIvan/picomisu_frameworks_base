// Copyright 2026 Picomisu contributors
// SPDX-License-Identifier: Apache-2.0

package com.android.server.am;

/**
 * Smartisan extension state of a {@link UidRecord} (its {@code mSmtEx}). Reconstructed from
 * the PICO OS 5.13.7 factory services; only the members reached by the Smartisan
 * {@code IActivityManagerSmtEx} methods are present.
 *
 * @hide
 */
public class UidRecordSmtBase {
    /** Current Smartisan freeze state of the uid, non-zero while frozen. */
    int curFrozenStat;

    public UidRecordSmtBase() {
    }
}
