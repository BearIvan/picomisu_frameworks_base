// Copyright 2026 Picomisu contributors
// SPDX-License-Identifier: Apache-2.0
package android.app;

import android.view.ViewRootImpl;
import com.pico.util.IExtBase;

/**
 * PICO activity-thread extension. Only the VR force-render query is ported;
 * the other factory methods (lifecycle, display and launch hooks) are not.
 * @hide
 */
public interface IExtActivityThread extends IExtBase {
    boolean isActivityForceRender(ViewRootImpl viewRoot);
}
