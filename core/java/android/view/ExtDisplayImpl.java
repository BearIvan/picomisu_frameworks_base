// Copyright 2026 Picomisu contributors
// SPDX-License-Identifier: Apache-2.0
package android.view;

/**
 * PICO display extension.
 * @hide
 */
public class ExtDisplayImpl implements IExtDisplay {
    private static final String TAG = "DisplayExt";
    private Display mBase;

    public ExtDisplayImpl(Display base) {
        mBase = base;
    }

    @Override
    public boolean isNoFocusableDisplay() {
        return (mBase.getFlags() & FLAG_2D_APP_VIRTUAL_DISPLAY_UNFOCUSABLE) != 0;
    }

    @Override
    public boolean isVr2dDisplay() {
        return (mBase.getFlags() & FLAG_2D_APP_VIRTUAL_DISPLAY) != 0;
    }

    @Override
    public boolean isVrLoadingDisplay() {
        return (mBase.getFlags() & FLAG_VR_LOADING_VIRTUAL_DISPLAY) != 0;
    }
}
