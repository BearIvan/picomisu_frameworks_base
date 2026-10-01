// Copyright 2026 Picomisu contributors
// SPDX-License-Identifier: Apache-2.0

package com.android.server.am;

import android.os.DebugSmtEx;
import java.util.LinkedList;

/**
 * Reconstructed from the PICO OS 5.13.7 factory services.
 *
 * @hide
 */
public interface ICollect3rdInfo {
    default void setCurResumedUid(int curResumedUid) {
        DebugSmtEx.printDefaultFunInfo(getClass());
    }

    default void recordAttach3rdInfo(ProcessRecord app, boolean hasActivity, long elapsedRealtime, boolean is3rdGroup) {
        DebugSmtEx.printDefaultFunInfo(getClass());
    }

    default void recordOom3rdInfo(ProcessRecord app, ProcessRecord TOP_APP, long nowElapsed, int adj, int schedGroup, int procState, boolean connectedWithTop, boolean connectedWithSystemServer, boolean is3rdGroup) {
        DebugSmtEx.printDefaultFunInfo(getClass());
    }

    default void flush3rdEvent() {
        DebugSmtEx.printDefaultFunInfo(getClass());
    }

    default LinkedList<String> get3rdUploadInfo() {
        DebugSmtEx.printDefaultFunInfo(getClass());
        return new LinkedList<>();
    }

    default void switch3rdInfo() {
        DebugSmtEx.printDefaultFunInfo(getClass());
    }

    default void writeOOMAnd3rdCountReason() {
        DebugSmtEx.printDefaultFunInfo(getClass());
    }
}
