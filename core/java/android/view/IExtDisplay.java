// Copyright 2026 Picomisu contributors
// SPDX-License-Identifier: Apache-2.0
package android.view;

import com.pico.util.IExtBase;

/**
 * PICO display extension: queries of the PICO virtual-display flags.
 * @hide
 */
public interface IExtDisplay extends IExtBase {
    /** Virtual display hosting a 2D application in VR. */
    int FLAG_2D_APP_VIRTUAL_DISPLAY = 1 << 15;
    /** 2D application virtual display that must not take focus. */
    int FLAG_2D_APP_VIRTUAL_DISPLAY_UNFOCUSABLE = 1 << 16;
    /** Virtual display showing the VR loading UI. */
    int FLAG_VR_LOADING_VIRTUAL_DISPLAY = 1 << 14;

    boolean isNoFocusableDisplay();
    boolean isVr2dDisplay();
    boolean isVrLoadingDisplay();
}
