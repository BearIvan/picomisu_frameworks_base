// Copyright 2026 Picomisu contributors
// SPDX-License-Identifier: Apache-2.0
package com.android.server.wm;

import com.pico.util.IExtBase;

/**
 * PICO task tap listener extension (factory PICO OS 5.13.7
 * com.android.server.wm.IExtTaskTapPointerEventListener).
 * @hide
 */
public interface IExtTaskTapPointerEventListener extends IExtBase {
    boolean disableHover();
}
