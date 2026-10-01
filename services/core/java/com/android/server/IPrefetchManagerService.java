// Copyright 2026 Picomisu contributors
// SPDX-License-Identifier: Apache-2.0

package com.android.server;

import android.app.IPrefetchObserver;
import android.content.pm.ApplicationInfo;
import android.os.DebugSmtEx;
import com.android.server.am.ProcessRecord;
import java.util.List;

/**
 * Reconstructed from the PICO OS 5.13.7 factory services.
 *
 * @hide
 */
public interface IPrefetchManagerService {
    default void startPrefetchApp(String packageName) {
        DebugSmtEx.printDefaultFunInfo(getClass());
    }

    default boolean isPrefetchProc(String packageName) {
        DebugSmtEx.printDefaultFunInfo(getClass());
        return false;
    }

    default void putPrefetch(ProcessRecord proc) {
        DebugSmtEx.printDefaultFunInfo(getClass());
    }

    default void removePrefetch(ProcessRecord proc) {
        DebugSmtEx.printDefaultFunInfo(getClass());
    }

    default void registerPrefetchObserver(IPrefetchObserver observer) {
        DebugSmtEx.printDefaultFunInfo(getClass());
    }

    default void unregisterPrefetchObserver(IPrefetchObserver observer) {
        DebugSmtEx.printDefaultFunInfo(getClass());
    }

    default void onRealStart(ProcessRecord proc) {
        DebugSmtEx.printDefaultFunInfo(getClass());
    }

    default void updatePrefetchApp(List<String> packageNames, int flag) {
        DebugSmtEx.printDefaultFunInfo(getClass());
    }

    default boolean isAllowStartPretch(String packageName) {
        DebugSmtEx.printDefaultFunInfo(getClass());
        return false;
    }

    default void updatePrefetchVersion(String packageName, String packageVersionName, int packageVersionCode) {
        DebugSmtEx.printDefaultFunInfo(getClass());
    }

    default boolean getPrefetchEnable() {
        DebugSmtEx.printDefaultFunInfo(getClass());
        return false;
    }

    default void canclePrefetch(ApplicationInfo info) {
        DebugSmtEx.printDefaultFunInfo(getClass());
    }
}
