// Copyright 2026 Picomisu contributors
// SPDX-License-Identifier: Apache-2.0
package android.view;

import com.pico.util.IExtBase;

/**
 * PICO view-root extension: VR skip-draw policy, 2D-display application resources and the
 * native shell ("NS") client of input-method windows.
 * @hide
 */
public interface IExtViewRootImpl extends IExtBase {
    void adjustApplicationContextResources();
    boolean isSkipDrawVrActivity();
    void onDoDie();
    void onSetView(View view, WindowManager.LayoutParams attrs);
}
