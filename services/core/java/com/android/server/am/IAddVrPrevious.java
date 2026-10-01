// Copyright 2026 Picomisu contributors
// SPDX-License-Identifier: Apache-2.0

package com.android.server.am;

import android.os.DebugSmtEx;
import com.android.server.wm.WindowProcessController;

/**
 * Reconstructed from the PICO OS 5.13.7 factory services.
 *
 * @hide
 */
public interface IAddVrPrevious {
    default void init(ActivityManagerService ams) {
        DebugSmtEx.printDefaultFunInfo(getClass());
    }

    default void updatePreviousVrProcess(WindowProcessController wpc, long visibleTime) {
        DebugSmtEx.printDefaultFunInfo(getClass());
    }

    default void dumpsysMemInfo(long cachedKernelMb, int cachedProcs) {
        DebugSmtEx.printDefaultFunInfo(getClass());
    }

    default boolean isEnable() {
        DebugSmtEx.printDefaultFunInfo(getClass());
        return false;
    }
}
