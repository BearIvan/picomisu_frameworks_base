// Copyright 2026 Picomisu contributors
// SPDX-License-Identifier: Apache-2.0
package android.app;

/**
 * PICO ResourcesManager extension (factory PICO OS 5.13.7 android.app.ExtResourcesManagerImpl).
 * The factory implementation only keeps its base object.
 * @hide
 */
public class ExtResourcesManagerImpl implements IExtResourcesManager {
    private ResourcesManager mBase;

    public ExtResourcesManagerImpl(ResourcesManager base) {
        mBase = base;
    }
}
