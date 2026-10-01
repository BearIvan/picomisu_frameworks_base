// Copyright 2026 Picomisu contributors
// SPDX-License-Identifier: Apache-2.0

package com.android.server.wm;

import android.os.SystemClock;
import android.util.SmtUidUtil;
import com.android.server.ApplicationFreezerHelperSmt;
import com.android.server.ApplicationFreezerInternalSmt;
import com.android.server.SysOptBridge;
import com.android.server.am.SysMonitorSvcBridge;

/**
 * Reconstructed from the PICO OS 5.13.7 factory services.
 *
 * @hide
 */
public class WindowStateSmtBase extends WindowContainerSmtBase implements ApplicationFreezerInternalSmt.IFrozenCallback {
    protected static final boolean DEBUG = true;
    protected static final String TAG = "WindowStateSmt";
    boolean isFrozenCallbackRegisterd;
    boolean isScreenDim;
    long mVisibleStartTime;
    protected WindowState mWindowState;
    public int smtUid;

    public WindowStateSmtBase(WindowState windowState) {
        super(windowState);
        this.isScreenDim = false;
        this.isFrozenCallbackRegisterd = false;
        this.smtUid = 1000;
        this.mVisibleStartTime = -1L;
        this.mWindowState = windowState;
    }

    void registerFrozenCallbackByPidOnce() {
        if (!this.isFrozenCallbackRegisterd) {
            this.isFrozenCallbackRegisterd = true;
            ApplicationFreezerHelperSmt.registerFrozenCallbackByPidOnce(this.mWindowState.mSession.mPid, this.mWindowState.mSession.mUid, this);
        }
    }

    @Override
    public void onAppFreeze(int pid, int uid) {
    }

    @Override
    public void onAppUnfreeze(int pid, int uid) {
    }

    public int getSmtUid(int uid, String packageName) {
        if (uid != 1000) {
            return uid;
        }
        if (this.smtUid == 1000) {
            this.smtUid = SmtUidUtil.getSystemUidForPackage(packageName);
        }
        int result = this.smtUid;
        return result;
    }

    public int getSmtUid() {
        return getSmtUid(this.mWindowState.mOwnerUid, this.mWindowState.mAttrs.packageName);
    }

    public void onWindowVisibleChanged(int currentVisibility) {
        if (SysMonitorSvcBridge.getFactory().getSysPerfMonitorService().isScreenOn()) {
            long current = SystemClock.uptimeMillis();
            if (currentVisibility == 0) {
                this.mVisibleStartTime = current;
                return;
            }
            long j = this.mVisibleStartTime;
            if (j != -1) {
                long duration = current - j;
                SysOptBridge.getFactory().getSmartService().updateWindowVisibleTime(getSmtUid(), this.mWindowState.getWindowTag().toString(), duration, SysOptBridge.getFactory().getSmartService().getCurrentDisplayMode());
            }
            this.mVisibleStartTime = -1L;
        }
    }

    public void onDisplayFpsModeChanged(int displayMode) {
        long current = SystemClock.uptimeMillis();
        long j = this.mVisibleStartTime;
        if (j != -1) {
            long duration = current - j;
            SysOptBridge.getFactory().getSmartService().updateWindowVisibleTime(getSmtUid(), this.mWindowState.getWindowTag().toString(), duration, displayMode);
        }
        if (this.mWindowState.mViewVisibility == 0) {
            this.mVisibleStartTime = current;
        } else {
            this.mVisibleStartTime = -1L;
        }
    }

    public void forceUpdateVisibleTime(boolean screenOn) {
        long current = SystemClock.uptimeMillis();
        long j = this.mVisibleStartTime;
        if (j != -1) {
            long duration = current - j;
            SysOptBridge.getFactory().getSmartService().updateWindowVisibleTime(getSmtUid(), this.mWindowState.getWindowTag().toString(), duration, SysOptBridge.getFactory().getSmartService().getCurrentDisplayMode());
        }
        if (screenOn) {
            if (this.mWindowState.mViewVisibility == 0) {
                this.mVisibleStartTime = current;
                return;
            } else {
                this.mVisibleStartTime = -1L;
                return;
            }
        }
        this.mVisibleStartTime = -1L;
    }
}
