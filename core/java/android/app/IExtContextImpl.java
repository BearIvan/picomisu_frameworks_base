// Copyright 2026 Picomisu contributors
// SPDX-License-Identifier: Apache-2.0
package android.app;

import android.view.Display;

import com.pico.util.IExtBase;

/**
 * PICO context extension (factory PICO OS 5.13.7 android.app.IExtContextImpl).
 * @hide
 */
public interface IExtContextImpl extends IExtBase {
    Display redirectDisplayIfNeeded(ResourcesManager resourcesManager);
}
