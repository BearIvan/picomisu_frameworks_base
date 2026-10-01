// Copyright 2026 Picomisu contributors
// SPDX-License-Identifier: Apache-2.0

package android.view;

import com.pico.util.IExtBase;

/**
 * PICO window types (PICO OS 5.13.7 factory framework).
 *
 * @hide
 */
public interface IExtWindowManager extends IExtBase {
    /** Native shell floating window. */
    int TYPE_NS_FLOATING_WINDOW = 2998;
}
