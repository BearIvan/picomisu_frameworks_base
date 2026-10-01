// Copyright 2026 Picomisu contributors
// SPDX-License-Identifier: Apache-2.0

package com.android.server.am;

import android.os.DebugSmtEx;
import com.android.internal.app.ProcessMap;

/**
 * Reconstructed from the PICO OS 5.13.7 factory services.
 *
 * @hide
 */
public interface IApplicationFreezer {
    public static final int BLOCK_NONE = 0;
    public static final int BLOCK_PUBLISH_PROVIDER = 1;
    public static final int FREEZE_ERRNO_BLOCKED = -1;
    public static final int FREEZE_ERRNO_FOCUS = -3;
    public static final int FREEZE_ERRNO_PENDING_UID = -2;
    public static final int FREEZE_ERRNO_SUCCESS = 0;

    public enum FreezeStatus {
        NOT_FROZEN,
        FREEZING,
        FROZEN,
        SHOULD_NOT_UNFREEZE,
        UNFROZEN
    }

    public enum Mode {
        DEFAULT,
        LITE,
        LIGHTNING,
        INVALID
    }

    @FunctionalInterface
    public interface ProcessFilter {
        int canFreeze(ProcessRecord processRecord);
    }

    @FunctionalInterface
    public interface TraverseCallback {
        ProcessRecord onProcess(ProcessRecord processRecord);
    }

    @FunctionalInterface
    public interface UidFrozenStateCallback {
        void onUidUnfreeze(int i, boolean z);
    }

    public enum UnfreezeReason {
        NONE(-1),
        NEED_NO_CONDITION(0),
        NEED_START_PROCESS(1),
        FREEZE_WINDOW(2),
        NEED_CONTENT_PROVIDER(10),
        NEED_SERVICE(20),
        NEED_BACKUP_AGENT(30),
        NEED_START_ACTIVITY(40),
        NEED_RESUME_ACTIVITY(50),
        NEED_UPDATE_VISIBILITY(55),
        NEED_DESTROY_ACTIVITY(50),
        NEED_BROADCAST(60),
        NEED_FOCUSED(70),
        NEED_RESUME_ALIVE(80),
        NEED_KILLED(100),
        NEED_NETWORK(150),
        NEED_HEARTBEAT(160),
        AUTO_FREEZE(170);

        private int value;

        UnfreezeReason(int v) {
            this.value = v;
        }

        public int getValue() {
            return this.value;
        }
    }

    public enum FreezeReason {
        NONE(-1),
        NO_CONDITION(0),
        BUSY_BACKGROUND(1),
        FREEZE_WINDOW(2),
        TNT_MEM_LOW(3),
        PREFETCH(10),
        KEEP_ALIVE(20),
        FREEZE_PREFETCH(30),
        POWER(100),
        MEM_LOW(200),
        SMART_SCENSE(300),
        SMART_SCENSE_STATE_IDLE(301),
        SMART_SCENSE_STATE_DOZE(302),
        AUTO_FREEZE(400);

        private int value;

        FreezeReason(int v) {
            this.value = v;
        }

        public int getValue() {
            return this.value;
        }
    }

    default ProcessRecord unfreezeAppIfNeededLocked(ProcessRecord app, String processName, int uid, UnfreezeReason reason, ProcessRecord caller, Object startingInfo) {
        DebugSmtEx.printDefaultFunInfo(getClass());
        return null;
    }

    default boolean unfreezeAppIfNeededLocked(ProcessRecord app, UnfreezeReason reason, ProcessRecord caller, Object startingInfo) {
        DebugSmtEx.printDefaultFunInfo(getClass());
        return false;
    }

    default ProcessRecord traverse(TraverseCallback cb) {
        DebugSmtEx.printDefaultFunInfo(getClass());
        return null;
    }

    default boolean freezeProcessLocked(ProcessRecord oneApp, boolean allAppInUid, FreezeReason reason) {
        DebugSmtEx.printDefaultFunInfo(getClass());
        return false;
    }

    default boolean unfreezeProcessLocked(ProcessRecord oneApp, boolean allAppInUid, UnfreezeReason reason) {
        DebugSmtEx.printDefaultFunInfo(getClass());
        return false;
    }

    default void setFocusedAppLocked(ProcessRecord app) {
        DebugSmtEx.printDefaultFunInfo(getClass());
    }

    default ProcessRecord get(String processName, int uid) {
        DebugSmtEx.printDefaultFunInfo(getClass());
        return null;
    }

    default ProcessRecord getFrozenProc(int pid) {
        DebugSmtEx.printDefaultFunInfo(getClass());
        return null;
    }

    default ProcessRecord takeoutByProcessLocked(ProcessRecord app) {
        DebugSmtEx.printDefaultFunInfo(getClass());
        return null;
    }

    default ProcessRecord takeoutByNameLocked(String name, int uid) {
        DebugSmtEx.printDefaultFunInfo(getClass());
        return null;
    }

    default ProcessMap<ProcessRecord> getFrozenProcesses() {
        DebugSmtEx.printDefaultFunInfo(getClass());
        return null;
    }

    default boolean freezeUidLocked(int uid, FreezeReason reason) {
        DebugSmtEx.printDefaultFunInfo(getClass());
        return false;
    }

    default boolean freezeUidLocked(int uid, FreezeReason reason, Mode mode) {
        DebugSmtEx.printDefaultFunInfo(getClass());
        return false;
    }

    default boolean registerProcessFilter(ProcessFilter filter, boolean register) {
        DebugSmtEx.printDefaultFunInfo(getClass());
        return false;
    }

    default boolean freezeProcessLiteLocked(int[] pid, FreezeReason reason) {
        DebugSmtEx.printDefaultFunInfo(getClass());
        return false;
    }

    default boolean unfreezeProcessLiteLocked(int[] pid, UnfreezeReason reason) {
        DebugSmtEx.printDefaultFunInfo(getClass());
        return false;
    }

    default boolean freezeProcessLocked(ProcessRecord oneApp, boolean allAppInUid, FreezeReason reason, Mode mode) {
        DebugSmtEx.printDefaultFunInfo(getClass());
        return false;
    }
}
