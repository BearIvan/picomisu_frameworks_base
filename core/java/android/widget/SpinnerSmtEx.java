// Copyright 2026 Picomisu contributors
// SPDX-License-Identifier: Apache-2.0

package android.widget;

import android.view.ViewSmtBase;

/**
 * Smartisan extension of a {@link Spinner}
 * (factory PICO OS 5.13.7 {@code android.widget.SpinnerSmtEx}).
 *
 * @hide
 */
public class SpinnerSmtEx extends ViewSmtBase {
    private Spinner mSpinner;

    public SpinnerSmtEx(Spinner spinner) {
        super(spinner);
        mSpinner = spinner;
    }
}
