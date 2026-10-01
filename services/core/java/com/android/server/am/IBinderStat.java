// Copyright 2026 Picomisu contributors
// SPDX-License-Identifier: Apache-2.0

package com.android.server.am;

import android.content.Context;
import android.os.DebugSmtEx;
import android.os.ParcelFileDescriptor;
import com.android.internal.os.BinderCallsStats;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import org.json.JSONObject;

/**
 * Reconstructed from the PICO OS 5.13.7 factory services.
 *
 * @hide
 */
public interface IBinderStat {
    default void asyncBinderStat(int[] array) {
        DebugSmtEx.printDefaultFunInfo(getClass());
    }

    default void onewayTransactStat(ParcelFileDescriptor fd) {
        DebugSmtEx.printDefaultFunInfo(getClass());
    }

    default void binderTransactStat(ParcelFileDescriptor fd) {
        DebugSmtEx.printDefaultFunInfo(getClass());
    }

    default String onewayTransactStat(int ownerPid, int callerPid) {
        DebugSmtEx.printDefaultFunInfo(getClass());
        return "";
    }

    default String binderTransactStat(int flags, int ownerPid, int callerPid) {
        DebugSmtEx.printDefaultFunInfo(getClass());
        return "";
    }

    default void saveCacheData() {
        DebugSmtEx.printDefaultFunInfo(getClass());
    }

    default void dumpBinderStat() {
        DebugSmtEx.printDefaultFunInfo(getClass());
    }

    default void settingContext(Context context) {
        DebugSmtEx.printDefaultFunInfo(getClass());
    }

    default void settingBinderStatsSampleInterval(int val) {
        DebugSmtEx.printDefaultFunInfo(getClass());
    }

    default void setAppBinderStatSampling(String[] pkgs, int sampling) {
        DebugSmtEx.printDefaultFunInfo(getClass());
    }

    default int initialBinderStat(Context context) {
        DebugSmtEx.printDefaultFunInfo(getClass());
        return 1000;
    }

    default boolean setDumpParams(String[] args, BinderCallsStats binderStat, PrintWriter pw) {
        DebugSmtEx.printDefaultFunInfo(getClass());
        return false;
    }

    default void dumpBinderStatSaveFile(FileDescriptor fd, PrintWriter pw) {
        DebugSmtEx.printDefaultFunInfo(getClass());
    }

    default void updateBinderStatConfig(JSONObject object) {
        DebugSmtEx.printDefaultFunInfo(getClass());
    }
}
