// Copyright 2026 Picomisu contributors
// SPDX-License-Identifier: Apache-2.0
package android.view;

import com.pico.util.IExtBase;

/**
 * PICO view-root extension. Only the VR skip-draw policy is ported; the factory
 * NS client, input and window hooks are not.
 * @hide
 */
public interface IExtViewRootImpl extends IExtBase {
    boolean isSkipDrawVrActivity();
}
