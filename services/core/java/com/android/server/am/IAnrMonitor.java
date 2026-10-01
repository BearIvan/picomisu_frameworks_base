// Copyright 2026 Picomisu contributors
// SPDX-License-Identifier: Apache-2.0

package com.android.server.am;

import android.app.ISysClient;
import android.os.DebugSmtEx;

/**
 * Reconstructed from the PICO OS 5.13.7 factory services.
 *
 * @hide
 */
public interface IAnrMonitor {
    default void monitorBroadcast(BroadcastRecord record) {
        DebugSmtEx.printDefaultFunInfo(getClass());
    }

    default void monitorService(ServiceRecord record) {
        DebugSmtEx.printDefaultFunInfo(getClass());
    }

    default void monitorInput(int pid, String reason) {
        DebugSmtEx.printDefaultFunInfo(getClass());
    }

    default void cancelService(ServiceRecord record) {
        DebugSmtEx.printDefaultFunInfo(getClass());
    }

    default void cancelBroadcast(BroadcastRecord record) {
        DebugSmtEx.printDefaultFunInfo(getClass());
    }

    default void notesBDTrack(String name, int track) {
        DebugSmtEx.printDefaultFunInfo(getClass());
    }

    default void notesServiceTrack(ServiceRecord r, int track) {
        DebugSmtEx.printDefaultFunInfo(getClass());
    }

    default String getMonitorInfo(String annotation, int pid, String processName) {
        DebugSmtEx.printDefaultFunInfo(getClass());
        return "";
    }

    default void anrOccured(String annotation, int pid, String processName) {
        DebugSmtEx.printDefaultFunInfo(getClass());
    }

    default void skipAnr(String annotation, int pid) {
        DebugSmtEx.printDefaultFunInfo(getClass());
    }

    default void removeClient(int pid) {
        DebugSmtEx.printDefaultFunInfo(getClass());
    }

    default void addClient(int pid, ISysClient client) {
        DebugSmtEx.printDefaultFunInfo(getClass());
    }

    default ISysClient getClient(int pid) {
        DebugSmtEx.printDefaultFunInfo(getClass());
        return null;
    }

    default void getCpuTopInfo() {
        DebugSmtEx.printDefaultFunInfo(getClass());
    }
}
