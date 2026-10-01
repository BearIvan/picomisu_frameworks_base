// Copyright 2026 Picomisu contributors
// SPDX-License-Identifier: Apache-2.0

package com.android.server;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.os.DebugSmtEx;
import android.os.Handler;
import java.util.HashSet;

/**
 * Reconstructed from the PICO OS 5.13.7 factory services.
 *
 * @hide
 */
public interface ISmartScenes {
    public static final int BLE_STATE = 2048;
    public static final int DOPPELGANGER_PACKAGE = 4;
    public static final int DOWNLOADING_STATE = 8;
    public static final int EXEMPT_PACKAGE = 65536;
    public static final int FOCUS_STATE = 4;

    @Deprecated
    public static final int IDLE_STATE = 1024;
    public static final int INPUT_METHOD_PACKAGE = 2;
    public static final int KEYGUARD_PACKAGE = 16;
    public static final int LOCATING_STATE = 16;
    public static final int NAVIGATING_STATE = 32;
    public static final int PING_STATE_FAILED = 1;
    public static final int PING_STATE_IDLE = 0;
    public static final int PING_STATE_PINGING = 2;
    public static final int PING_STATE_SUCCESS = 3;
    public static final int PIP_STATE = 512;
    public static final int RECORDING_STATE = 64;
    public static final int SOUNDING_STATE = 128;
    public static final int TOP_STATE = 2;
    public static final int UID_STATE_MASK = -1;
    public static final int UNKNOWN_STATE = 0;
    public static final int VISIBLE_STATE = 256;
    public static final int VPN_PACKAGE = 8;
    public static final int WALK_STATE_FAST = 2;
    public static final int WALK_STATE_IDLE = 0;
    public static final int WALK_STATE_RUN = 3;
    public static final int WALK_STATE_SLOW = 1;
    public static final int WALLPAPER_PACKAGE = 32;

    public interface IUidState {
        public static final int ERROR = -1;
        public static final int FOUND_RESULT = 1;
        public static final int NO_RESULT = 0;
        public static final int STATE_DOZE = 5;
        public static final int STATE_IDLE = 4;
        public static final int STATE_INTERACTIVE = 1;
        public static final int STATE_NONE = 0;
        public static final int STATE_PRE_IDLE = 3;
        public static final int STATE_USEFUL = 2;
        public static final int UPDATE_BY_GONE = 5;
        public static final int UPDATE_BY_HEART_BEAT = 4;
        public static final int UPDATE_BY_SMARTISAN_BRAIN = 6;
        public static final int UPDATE_BY_STATE_CHANGED = 1;
        public static final int UPDATE_BY_TIME = 2;
        public static final int UPDATE_BY_UNFREEZE = 3;
    }

    default void systemReady(Context context) {
        DebugSmtEx.printDefaultFunInfo(getClass());
    }

    default void onUidActive(int uid) {
        DebugSmtEx.printDefaultFunInfo(getClass());
    }

    default void onUidGone(int uid) {
        DebugSmtEx.printDefaultFunInfo(getClass());
    }

    default void updateUidProcState(int uid, int processState) {
        DebugSmtEx.printDefaultFunInfo(getClass());
    }

    default void onUidResume(int uid) {
        DebugSmtEx.printDefaultFunInfo(getClass());
    }

    default void noteStartGps(int uid) {
        DebugSmtEx.printDefaultFunInfo(getClass());
    }

    default void noteStopGps(int uid) {
        DebugSmtEx.printDefaultFunInfo(getClass());
    }

    default void updateFocusedUid(int uid, boolean pcMode) {
        DebugSmtEx.printDefaultFunInfo(getClass());
    }

    default void updateSoundingUids(HashSet<Integer> soundUids) {
        DebugSmtEx.printDefaultFunInfo(getClass());
    }

    default void updateRecordingUid(int uid, boolean active) {
        DebugSmtEx.printDefaultFunInfo(getClass());
    }

    default void updateKeyguardPackage(String packageName) {
        DebugSmtEx.printDefaultFunInfo(getClass());
    }

    default void updateWallPaperPackage(String packageName) {
        DebugSmtEx.printDefaultFunInfo(getClass());
    }

    default void updateVpnPackage(String packageName) {
        DebugSmtEx.printDefaultFunInfo(getClass());
    }

    default void updateBlePackage(String packageName, boolean added) {
        DebugSmtEx.printDefaultFunInfo(getClass());
    }

    default void updateProcessNameState(int uid, String processName, boolean added, boolean pcMode) {
        DebugSmtEx.printDefaultFunInfo(getClass());
    }

    default void updateProcessState(String processName, ApplicationInfo info, boolean added, boolean pcMode) {
        DebugSmtEx.printDefaultFunInfo(getClass());
    }

    default void updateInputMethod(String packageName) {
        DebugSmtEx.printDefaultFunInfo(getClass());
    }

    default void updateFocusWindow(ApplicationInfo info) {
        DebugSmtEx.printDefaultFunInfo(getClass());
    }

    default void updateDisplayRotation(int rotation) {
        DebugSmtEx.printDefaultFunInfo(getClass());
    }

    default int getUidCurrentState(int uid, String tag) {
        DebugSmtEx.printDefaultFunInfo(getClass());
        return 0;
    }

    default boolean uidNotInUse(int uid, String tag) {
        DebugSmtEx.printDefaultFunInfo(getClass());
        return false;
    }

    default int getPackageType(int uid, String packageName, String tag) {
        DebugSmtEx.printDefaultFunInfo(getClass());
        return 0;
    }

    default boolean hasPcProcess(int uid, String tag) {
        DebugSmtEx.printDefaultFunInfo(getClass());
        return false;
    }

    default int getPingGoogleState() {
        DebugSmtEx.printDefaultFunInfo(getClass());
        return 0;
    }

    default int getCurrentUidUsageState(int uid) {
        DebugSmtEx.printDefaultFunInfo(getClass());
        return 0;
    }

    public interface UidUsageStateChangeListener {
        default void onUidUsageStateChanged(int uid, int state) {
            DebugSmtEx.printDefaultFunInfo(getClass());
        }
    }

    default void registerUidUsageStateChangeListener(UidUsageStateChangeListener listener, Handler handler) {
        DebugSmtEx.printDefaultFunInfo(getClass());
    }

    default void unRegisterUidUsageStateChangeListener(UidUsageStateChangeListener listener) {
        DebugSmtEx.printDefaultFunInfo(getClass());
    }

    default void updateWindowCheckState(int pid, Object token) {
    }

    default void clearWindowCheck(Object token) {
    }

    default int getWindowCheckStatue() {
        return 0;
    }
}
