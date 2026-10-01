// Copyright 2026 Picomisu contributors
// SPDX-License-Identifier: Apache-2.0
package android.widget;

import com.pico.util.IExtBase;

/**
 * PICO toast extension (factory PICO OS 5.13.7 android.widget.IExtToast).
 * @hide
 */
public interface IExtToast extends IExtBase {
    boolean disableShow(int displayId);
}
