// Copyright 2026 Picomisu contributors
// SPDX-License-Identifier: Apache-2.0

package com.android.server.am;

import android.os.DebugSmtEx;

/**
 * Reconstructed from the PICO OS 5.13.7 factory services.
 *
 * @hide
 */
public interface IBootEventStat {
    default void writeEvent(String event, long value) {
        DebugSmtEx.printDefaultFunInfo(getClass());
    }

    default void setBootType(int bootType) {
        DebugSmtEx.printDefaultFunInfo(getClass());
    }

    default void setDataPackagesCount(int num) {
        DebugSmtEx.printDefaultFunInfo(getClass());
    }

    default void setSystemPackagesCount(int num) {
        DebugSmtEx.printDefaultFunInfo(getClass());
    }

    default void saveFile() {
        DebugSmtEx.printDefaultFunInfo(getClass());
    }

    default void release() {
        DebugSmtEx.printDefaultFunInfo(getClass());
    }
}
