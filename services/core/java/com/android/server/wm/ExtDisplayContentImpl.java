// Copyright 2026 Picomisu contributors
// SPDX-License-Identifier: Apache-2.0
package com.android.server.wm;

import android.os.ServiceManager;

import com.android.server.inputmethod.InputMethodManagerService;

/**
 * PICO display content extension (factory PICO OS 5.13.7
 * com.android.server.wm.ExtDisplayContentImpl).
 */
public class ExtDisplayContentImpl implements IExtDisplayContent {
    private DisplayContent mBase;

    public ExtDisplayContentImpl(DisplayContent base) {
        mBase = base;
    }

    /**
     * End of DisplayContent.removeImmediately: InputMethodManagerService drops the input method
     * client of the removed display.
     */
    @Override
    public void removeImmediately() {
        InputMethodManagerService imms =
                (InputMethodManagerService) ServiceManager.getService("input_method");
        if (imms != null) {
            imms.getExt().onDisplayContentDestroy(mBase.getDisplayId());
        }
    }
}
