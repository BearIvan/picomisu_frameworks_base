// Copyright 2026 Picomisu contributors
// SPDX-License-Identifier: Apache-2.0
package android.view;

import com.pico.util.IExtBase;

/**
 * PICO view extension (factory PICO OS 5.13.7 android.view.IExtView).
 * @hide
 */
public interface IExtView extends IExtBase {
    boolean isOrientation180();

    boolean isTypeVR();
}
