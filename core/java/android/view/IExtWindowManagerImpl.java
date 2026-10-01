// Copyright 2026 Picomisu contributors
// SPDX-License-Identifier: Apache-2.0
package android.view;

import com.pico.util.IExtBase;

/**
 * PICO window manager extension (factory PICO OS 5.13.7 android.view.IExtWindowManagerImpl).
 * @hide
 */
public interface IExtWindowManagerImpl extends IExtBase {
    boolean disableAddView(View view, ViewGroup.LayoutParams params);
}
