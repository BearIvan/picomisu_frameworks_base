// Copyright 2026 Picomisu contributors
// SPDX-License-Identifier: Apache-2.0

package android.app;

import android.content.Context;
import android.os.DebugSmtEx;

import java.util.HashMap;

/**
 * Smartisan sysmonitor extension of {@code com.android.server.BootReceiver}, implemented by the
 * optional sysmonitor framework JAR ({@link SysMonitorFwBridge}). Reconstructed from the
 * PICO OS 5.13.7 factory framework; the default methods are the factory behaviour when that
 * JAR is absent.
 *
 * @hide
 */
public interface IBootReceiverOptEx {
    default void logBootEvents(Context ctx, HashMap<String, Long> timestamps, String headers) {
        DebugSmtEx.printDefaultFunInfo(getClass());
    }

    default void addLogToRestart(String headers, int max, String ellipsis) {
        DebugSmtEx.printDefaultFunInfo(getClass());
    }

    default boolean addMoreInfoToNativeCarsh(String tag, String headers, String fileContents,
            String filename, int maxSize, String footers) {
        DebugSmtEx.printDefaultFunInfo(getClass());
        return false;
    }
}
