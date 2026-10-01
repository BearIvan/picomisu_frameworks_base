// Copyright 2026 Picomisu contributors
// SPDX-License-Identifier: Apache-2.0
package com.android.server.am;

import android.pico.utils.Features;

/**
 * PICO app error extension (factory PICO OS 5.13.7 com.android.server.am.ExtAppErrorsImpl):
 * with Features.FEAT_DISABLE_ANR_CRASH_DIALOG (persist.pvr.disableCrashAnr, default 1) no crash
 * or ANR dialog is shown.
 * @hide
 */
public class ExtAppErrorsImpl implements IExtAppErrors {
    private AppErrors mBase;

    public ExtAppErrorsImpl(AppErrors base) {
        mBase = base;
    }

    @Override
    public boolean getCrashSilenced(boolean oldCrashSilenced) {
        if (!Features.FEAT_DISABLE_ANR_CRASH_DIALOG) {
            return oldCrashSilenced;
        }
        return true;
    }

    @Override
    public boolean canShowAnrDialog() {
        return !Features.FEAT_DISABLE_ANR_CRASH_DIALOG;
    }
}
