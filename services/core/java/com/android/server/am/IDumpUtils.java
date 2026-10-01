// Copyright 2026 Picomisu contributors
// SPDX-License-Identifier: Apache-2.0

package com.android.server.am;

import android.content.Context;
import android.os.DebugSmtEx;
import com.android.server.wm.ActivityRecord;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

/**
 * Reconstructed from the PICO OS 5.13.7 factory services.
 *
 * @hide
 */
public interface IDumpUtils {
    default void doDump(Context context, String errorPrefix, int reasonCode, int actionMode, String services, String pids, String clientPackageName) {
        DebugSmtEx.printDefaultFunInfo(getClass());
    }

    default void dumpHeader(Context context, File file, ActivityRecord activityRecord, ProcessRecord processRecord, int clientPid, String clientPackageName) throws IOException {
        DebugSmtEx.printDefaultFunInfo(getClass());
    }

    default void printClientAndLastResumedProc(ActivityRecord activityRecord, int clientPid, String clientPackageName) {
        DebugSmtEx.printDefaultFunInfo(getClass());
    }

    default void dumpServices(File file, String services) throws IOException {
        DebugSmtEx.printDefaultFunInfo(getClass());
    }

    default void dumpLogcat(File file) throws IOException {
        DebugSmtEx.printDefaultFunInfo(getClass());
    }

    default void dumpPidTrace(File file, ArrayList<Integer> pidList) {
        DebugSmtEx.printDefaultFunInfo(getClass());
    }

    default String getPidCmdline(int pid) {
        return null;
    }

    default String getCurCapacity(Context context) {
        return null;
    }

    default String getCurReason(String error_prefix, int reasonCode) {
        return null;
    }

    default ActivityRecord getLastResumedActivityRecord(Context context) {
        return null;
    }

    default String getAppType(ActivityRecord activityRecord) {
        return null;
    }

    default ProcessRecord getProcessRecord(String procName, int uid) {
        return null;
    }

    default void pruneOldTraces(File dir) {
        DebugSmtEx.printDefaultFunInfo(getClass());
    }

    default void adjustSDKLogLevel(Context context) {
        DebugSmtEx.printDefaultFunInfo(getClass());
    }

    default void appendFile(File writeTo, File copyFrom) {
        DebugSmtEx.printDefaultFunInfo(getClass());
    }

    default void addCPUTrackerInfo(File file) {
        DebugSmtEx.printDefaultFunInfo(getClass());
    }

    default String getAppSDKVersion(Context context, int pid) {
        return null;
    }

    default void reportEvent(int eventCode, int reportCode, float ratio, int reasonCode, String reason, int reportCount, long[] returnTimeArray) {
        DebugSmtEx.printDefaultFunInfo(getClass());
    }

    default void copyToDropbox(String filePath) {
        DebugSmtEx.printDefaultFunInfo(getClass());
    }

    default void finishDump(Context context, String error_prefix, int reasonCode, File file, String packageName, String appType) throws IOException {
        DebugSmtEx.printDefaultFunInfo(getClass());
    }
}
