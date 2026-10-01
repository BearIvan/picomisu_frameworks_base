// Copyright 2026 Picomisu contributors
// SPDX-License-Identifier: Apache-2.0

package com.android.server.am;

import android.os.DebugSmtEx;

/**
 * Reconstructed from the PICO OS 5.13.7 factory services.
 *
 * @hide
 */
public interface IUIDDownloadInfo {
    default long getStartTime() {
        DebugSmtEx.printDefaultFunInfo(getClass());
        return 0L;
    }

    default void setStartTime(long startTime) {
        DebugSmtEx.printDefaultFunInfo(getClass());
    }

    default long getBytes() {
        DebugSmtEx.printDefaultFunInfo(getClass());
        return 0L;
    }

    default void setBytes(long bytes) {
        DebugSmtEx.printDefaultFunInfo(getClass());
    }

    default boolean isDownloadorUPload() {
        DebugSmtEx.printDefaultFunInfo(getClass());
        return false;
    }

    default void setDownloadorUPload(boolean downloadorUPload) {
        DebugSmtEx.printDefaultFunInfo(getClass());
    }

    default boolean getDownloadorUPload() {
        DebugSmtEx.printDefaultFunInfo(getClass());
        return false;
    }
}
