// Copyright 2026 Picomisu contributors
// SPDX-License-Identifier: Apache-2.0

package com.android.server;

import android.os.DebugSmtEx;
import java.util.ArrayList;
import java.util.List;

/**
 * Reconstructed from the PICO OS 5.13.7 factory services.
 *
 * @hide
 */
public interface ISysMonitorExtraLogUtil {
    default String currentPsiIoState() {
        DebugSmtEx.printDefaultFunInfo(getClass());
        return "";
    }

    default String addBinderPeer(int tid, ArrayList<Integer> firstPids, ArrayList<Integer> nativePids) {
        DebugSmtEx.printDefaultFunInfo(getClass());
        return "";
    }

    default String checkBinderPeerForWtd(ArrayList<Integer> firstPids, ArrayList<Integer> nativePids, List<Watchdog.HandlerChecker> blockedCheckers, Watchdog watchdog) {
        DebugSmtEx.printDefaultFunInfo(getClass());
        return "";
    }

    default String addBinderPeerSpecifically(ArrayList<String> XRInterested, ArrayList<Integer> firstPids, ArrayList<Integer> nativePids) {
        DebugSmtEx.printDefaultFunInfo(getClass());
        return "";
    }
}
