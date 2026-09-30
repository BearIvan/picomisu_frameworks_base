// Copyright 2026 Picomisu contributors
// SPDX-License-Identifier: Apache-2.0

package android.app;

import android.content.IIntentReceiver;
import android.os.DebugSmtEx;
import android.os.IBinder;

/**
 * Smartisan ANR logger of an application process, implemented by the optional sysmonitor
 * framework JAR ({@link SysMonitorFwBridge}). Reconstructed from the PICO OS 5.13.7 factory
 * framework; the default methods are the factory behaviour when that JAR is absent.
 *
 * @hide
 */
public interface IAnrLogger {
    default void monitorLooper() {
        DebugSmtEx.printDefaultFunInfo(getClass());
    }

    default void monitorLooper(int thresholdMsg, int thresholdList) {
        DebugSmtEx.printDefaultFunInfo(getClass());
    }

    default void notesServiceTrack(String name, IBinder token, int track) {
        DebugSmtEx.printDefaultFunInfo(getClass());
    }

    default void notesBDtrack(String name, int flag, int track) {
        DebugSmtEx.printDefaultFunInfo(getClass());
    }

    default void notesInputTrack(int serSeq, int proSeq, int track) {
        DebugSmtEx.printDefaultFunInfo(getClass());
    }

    default void collectBroadcastInfo(IIntentReceiver receiver, String name, String identifier,
            int collectTimes, long beginTime) {
        DebugSmtEx.printDefaultFunInfo(getClass());
    }

    default void collectServiceInfo(String name, int collectTimes, long beginTime,
            long timeout) {
        DebugSmtEx.printDefaultFunInfo(getClass());
    }

    default void collectInputInfo(int seq, int collectTimes, long beginTime) {
        DebugSmtEx.printDefaultFunInfo(getClass());
    }

    default void findCurrentMainMsgs(long current, long trackingTime, int uid,
            long endWallTime) {
        DebugSmtEx.printDefaultFunInfo(getClass());
    }
}
