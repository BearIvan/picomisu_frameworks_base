// Copyright 2026 Picomisu contributors
// SPDX-License-Identifier: Apache-2.0

package android.widget;

import android.view.ViewSmtBase;

/**
 * Smartisan extension of an {@link ImageButton}
 * (factory PICO OS 5.13.7 {@code android.widget.ImageButtonSmtEx}; the factory never creates it).
 *
 * @hide
 */
public class ImageButtonSmtEx extends ViewSmtBase {
    private ImageButton mImageButton;

    public ImageButtonSmtEx(ImageButton imageButton) {
        super(imageButton);
        mImageButton = imageButton;
    }
}
