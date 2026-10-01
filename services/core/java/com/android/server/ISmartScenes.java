// Copyright 2026 Picomisu contributors
// SPDX-License-Identifier: Apache-2.0

package com.android.server;

import android.os.DebugSmtEx;

/**
 * Smartisan smart scenes (foreground/recording/playback state tracking) implemented by the
 * optional sys services JAR. Reconstructed from the PICO OS 5.13.7 factory services; only the
 * methods reached by the ported factory code are present, with their factory default
 * implementations.
 *
 * @hide
 */
public interface ISmartScenes {
    default void updateRecordingUid(int uid, boolean active) {
        DebugSmtEx.printDefaultFunInfo(getClass());
    }
}
