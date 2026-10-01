// Copyright 2026 Picomisu contributors
// SPDX-License-Identifier: Apache-2.0
package android.view;

import com.pico.util.IExtBase;

/**
 * PICO SurfaceView extension (factory PICO OS 5.13.7 android.view.IExtSurfaceView).
 * @hide
 */
public interface IExtSurfaceView extends IExtBase {
    Surface getSurface();
}
