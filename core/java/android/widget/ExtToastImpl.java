// Copyright 2026 Picomisu contributors
// SPDX-License-Identifier: Apache-2.0
package android.widget;

/**
 * PICO toast extension (factory PICO OS 5.13.7 android.widget.ExtToastImpl): toasts of contexts
 * on the default display (the VR display) are not shown.
 * @hide
 */
public class ExtToastImpl implements IExtToast {
    private Toast mBase;

    public ExtToastImpl(Toast base) {
        mBase = base;
    }

    @Override
    public boolean disableShow(int displayId) {
        return displayId == 0;
    }
}
