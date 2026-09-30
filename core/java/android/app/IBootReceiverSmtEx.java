// Copyright 2026 Picomisu contributors
// SPDX-License-Identifier: Apache-2.0

package android.app;

import android.content.Context;
import android.os.DebugSmtEx;

import java.io.File;
import java.util.HashMap;

/**
 * Smartisan extension of {@code com.android.server.BootReceiver} (crash and restart logs added
 * to DropBox), implemented by the optional sys framework JAR ({@link SysFwBridge}).
 * Reconstructed from the PICO OS 5.13.7 factory framework; the default methods are the factory
 * behaviour when that JAR is absent.
 *
 * @hide
 */
public interface IBootReceiverSmtEx {
    default void logBootEvents(Context ctx, HashMap<String, Long> timestamps, String headers) {
        DebugSmtEx.printDefaultFunInfo(getClass());
    }

    default void addLogToRestart(String headers, int max, String ellipsis) {
        DebugSmtEx.printDefaultFunInfo(getClass());
    }

    default boolean checkSystemRestart() {
        DebugSmtEx.printDefaultFunInfo(getClass());
        return false;
    }

    default boolean checkIfCrashBelongsCurVer(File file) {
        DebugSmtEx.printDefaultFunInfo(getClass());
        return true;
    }

    default int adjustDropboxFileContentSize(File file) {
        DebugSmtEx.printDefaultFunInfo(getClass());
        return -1;
    }

    default String addLogcatToFooters() {
        DebugSmtEx.printDefaultFunInfo(getClass());
        return "";
    }

    default boolean addMoreInfoToNativeCarsh(String tag, String headers, String fileContents,
            String filename, int maxSize, String footers) {
        DebugSmtEx.printDefaultFunInfo(getClass());
        return false;
    }
}
