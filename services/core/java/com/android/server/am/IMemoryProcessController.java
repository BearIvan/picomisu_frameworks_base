// Copyright 2026 Picomisu contributors
// SPDX-License-Identifier: Apache-2.0

package com.android.server.am;

import android.content.ComponentName;
import android.os.DebugSmtEx;
import java.util.List;

/**
 * Reconstructed from the PICO OS 5.13.7 factory services.
 *
 * @hide
 */
public interface IMemoryProcessController {
    default void compactProcess(ProcessRecord app) {
        DebugSmtEx.printDefaultFunInfo(getClass());
    }

    default boolean compact(ProcessRecord app) {
        DebugSmtEx.printDefaultFunInfo(getClass());
        return true;
    }

    default void compactProcess(int pid) {
        DebugSmtEx.printDefaultFunInfo(getClass());
    }

    default void setEnable(boolean enable) {
        DebugSmtEx.printDefaultFunInfo(getClass());
    }

    default boolean getEnable() {
        DebugSmtEx.printDefaultFunInfo(getClass());
        return false;
    }

    default void onMemLowUpdate(int newPressureState) {
        DebugSmtEx.printDefaultFunInfo(getClass());
    }

    default void updateVpnPackage(String vpnPackage) {
        DebugSmtEx.printDefaultFunInfo(getClass());
    }

    default void onActivityStopped(int pid) {
        DebugSmtEx.printDefaultFunInfo(getClass());
    }

    default void keepAliveBackground(ComponentName className, int pid, int flags, int level) {
        DebugSmtEx.printDefaultFunInfo(getClass());
    }

    default void clearKeepAliveProcesses() {
        DebugSmtEx.printDefaultFunInfo(getClass());
    }

    default void updatePrefetchApp(List<String> packageNames) {
        DebugSmtEx.printDefaultFunInfo(getClass());
    }

    default boolean getMemPushUfsEnable() {
        DebugSmtEx.printDefaultFunInfo(getClass());
        return false;
    }
}
