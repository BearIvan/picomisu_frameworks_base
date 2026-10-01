// Copyright 2026 Picomisu contributors
// SPDX-License-Identifier: Apache-2.0
package com.android.server.policy;

import android.pico.utils.Features;

import com.pico.util.IExtBase;

/**
 * PICO WindowManagerPolicy extension (factory PICO OS 5.13.7
 * com.android.server.policy.IExtWindowManagerPolicy; interface only, no implementation class).
 */
public interface IExtWindowManagerPolicy extends IExtBase {
    /**
     * Layer of the PICO window types, or -1 to use the AOSP table: window type 2998 is laid out
     * as an application window (layer 2).
     */
    static int getWindowLayerFromTypeLw(int type, boolean canAddInternalSystemWindow) {
        if (Features.isPvr2DEnabled() && type == 2998) {
            return 2;
        }
        return -1;
    }
}
