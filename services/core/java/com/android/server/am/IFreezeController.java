// Copyright 2026 Picomisu contributors
// SPDX-License-Identifier: Apache-2.0

package com.android.server.am;

import android.content.Intent;
import android.os.DebugSmtEx;
import android.util.Slog;
import java.util.List;

/**
 * Reconstructed from the PICO OS 5.13.7 factory services.
 *
 * @hide
 */
public interface IFreezeController {
    default void freezeAndReclaimLocked(List<ProcessRecord> apps, IApplicationFreezer.FreezeReason reason) {
        DebugSmtEx.printDefaultFunInfo(getClass());
    }

    default void unfreezeAppsIfNeededLocked(List<ProcessRecord> apps, IApplicationFreezer.UnfreezeReason reason, ProcessRecord caller, Object startingInfo) {
        DebugSmtEx.printDefaultFunInfo(getClass());
    }

    default void onMemLowUpdate(int memPressureState) {
        Slog.e("SYS_DEFAULT_LOG", getClass().getName() + "|" + Thread.currentThread().getStackTrace()[1].getMethodName() + "|SYS_DEFAULT_FUN_CONTENT");
    }

    default void startProcessEvent(ProcessRecord app, int pid) {
        DebugSmtEx.printDefaultFunInfo(getClass());
    }

    default void addProc(int uid, int pid, ProcessRecord app) {
        DebugSmtEx.printDefaultFunInfo(getClass());
    }

    default void removeProc(int uid, int pid) {
        DebugSmtEx.printDefaultFunInfo(getClass());
    }

    default void updateImportantUids(ConnectionRecord cr, ProcessRecord app) {
        DebugSmtEx.printDefaultFunInfo(getClass());
    }

    default void updateImportantUids(ProcessRecord client, ProcessRecord app) {
        DebugSmtEx.printDefaultFunInfo(getClass());
    }

    default void updateLastImportantUidsIfNeeded() {
        DebugSmtEx.printDefaultFunInfo(getClass());
    }

    default void clearImportantUids() {
        DebugSmtEx.printDefaultFunInfo(getClass());
    }

    default void clientConnectionRemoveEvent(int providerUid, int clientUid) {
        DebugSmtEx.printDefaultFunInfo(getClass());
    }

    default void bumpServiceEvent(int uid, int pid, boolean done, String reason) {
        DebugSmtEx.printDefaultFunInfo(getClass());
    }

    default void serviceTimeoutEvent(int uid, int pid, String reason) {
        DebugSmtEx.printDefaultFunInfo(getClass());
    }

    default void receiveBroadcastEvent(int uid, int pid, boolean finish, boolean hasTimeout, Intent intent) {
        DebugSmtEx.printDefaultFunInfo(getClass());
    }

    default void broadcastTimeoutEvent(int uid, int pid, Intent intent) {
        DebugSmtEx.printDefaultFunInfo(getClass());
    }

    default void importantProviderChange(int uid, boolean inc, ProcessRecord client) {
        DebugSmtEx.printDefaultFunInfo(getClass());
    }

    default boolean isPidFrozen(int pid) {
        DebugSmtEx.printDefaultFunInfo(getClass());
        return false;
    }
}
