// Copyright 2026 Picomisu contributors
// SPDX-License-Identifier: Apache-2.0

package com.android.server.wm;

import android.content.pm.ApplicationInfo;
import android.graphics.Rect;
import android.view.WindowManagerPolicyConstantsSmtEx;
import com.android.server.LocalServices;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashSet;

/**
 * Reconstructed from the PICO OS 5.13.7 factory services.
 *
 * @hide
 */
public class WindowManagerServiceSmtBase {
    public static final int HIDE_STATUS_BAR_TIMEOUT_DURATION = 360;
    public static final int MSG_DO_TRANVERSAL = 1002;
    public static final int MSG_HIDE_STATUS_BAR_TIMEOUT = 1005;
    public static final int MSG_ROTATED_ANIM_TIMEOUT = 1004;
    public static final int MSG_UPDATE_REORDER_TASK = 1003;
    public static final int ROTATED_ANIM_TIMEOUT_DURATION = 3000;
    protected static final String TAG = "WindowManager";
    private WindowState mCurrentFocusWindow;
    LocalServiceSmtBase mLocalServiceSmtEx;
    protected WindowManagerService mWmService;
    protected static int sDW = -1;
    protected static int sDH = -1;
    protected static int sDSquare = -1;
    protected static int sOperatibleSquare = -1;
    protected boolean mIsStartFromHome = false;
    ArrayList<WindowManagerPolicyConstantsSmtEx.VisibleWindowChangeListenerSmtEx> mVisibleWindowChangeListeners = new ArrayList<>();
    protected HashSet<ApplicationInfo> mVisibleApplicationInfos = new HashSet<>();
    HashSet<ApplicationInfo> mApplicationInfosBK = new HashSet<>();
    protected HashSet<Integer> mVisibleUids = new HashSet<>();
    HashSet<Integer> mVisibleUidsBK = new HashSet<>();

    protected WindowManagerServiceSmtBase(WindowManagerService wmService) {
        this.mWmService = wmService;
    }

    protected void initLocalServices() {
        this.mLocalServiceSmtEx = (this).new LocalServiceSmtBase();
        LocalServices.addService(WindowManagerInternalSmtBase.class, this.mLocalServiceSmtEx);
    }

    class LocalServiceSmtBase extends WindowManagerInternalSmtBase {
        LocalServiceSmtBase() {
        }

        @Override
        public void registerVisibleWindowChangeListener(WindowManagerPolicyConstantsSmtEx.VisibleWindowChangeListenerSmtEx listener) {
            synchronized (WindowManagerServiceSmtBase.this.mWmService.mGlobalLock) {
                try {
                    WindowManagerService.boostPriorityForLockedSection();
                    if (listener == null) {
                        throw new IllegalArgumentException("registerVisibleWindowChangeListener listener null");
                    }
                    if (WindowManagerServiceSmtBase.this.mVisibleWindowChangeListeners.contains(listener)) {
                        throw new IllegalStateException("registerVisibleWindowChangeListener: trying to register" + listener + " twice.");
                    }
                    WindowManagerServiceSmtBase.this.mVisibleWindowChangeListeners.add(listener);
                } catch (Throwable th) {
                    WindowManagerService.resetPriorityAfterLockedSection();
                    throw th;
                }
            }
            WindowManagerService.resetPriorityAfterLockedSection();
        }

        @Override
        public void unRegisterVisibleWindowChangeListener(WindowManagerPolicyConstantsSmtEx.VisibleWindowChangeListenerSmtEx listener) {
            synchronized (WindowManagerServiceSmtBase.this.mWmService.mGlobalLock) {
                try {
                    WindowManagerService.boostPriorityForLockedSection();
                    if (!WindowManagerServiceSmtBase.this.mVisibleWindowChangeListeners.contains(listener)) {
                        throw new IllegalStateException("unRegisterVisibleWindowChangeListener: " + listener + "not registered.");
                    }
                    WindowManagerServiceSmtBase.this.mVisibleWindowChangeListeners.remove(listener);
                } catch (Throwable th) {
                    WindowManagerService.resetPriorityAfterLockedSection();
                    throw th;
                }
            }
            WindowManagerService.resetPriorityAfterLockedSection();
        }
    }

    static boolean isOperatible(WindowState w) {
        Rect r = w.getDisplayFrameLw();
        if (w.isDisplayedLw() && r.width() * r.height() > sOperatibleSquare) {
            return true;
        }
        return false;
    }

    void switchApplicationInfos() {
        HashSet<ApplicationInfo> tmp = this.mVisibleApplicationInfos;
        this.mVisibleApplicationInfos = this.mApplicationInfosBK;
        this.mApplicationInfosBK = tmp;
    }

    public HashSet<ApplicationInfo> getApplicationInfos() {
        return this.mVisibleApplicationInfos;
    }

    void switchVisibleUids() {
        HashSet<Integer> tmp = this.mVisibleUids;
        this.mVisibleUids = this.mVisibleUidsBK;
        this.mVisibleUidsBK = tmp;
    }

    public HashSet<Integer> getVisibleUids() {
        return this.mVisibleUids;
    }

    void clearStartingWindowFiles() {
        SmartisanStartingWindowManager.getInstance().clearStartingWindowFiles();
    }

    public void dump(PrintWriter pw) {
        pw.print("  mVisibleUidsBK: {");
        for (Integer uid : this.mVisibleUidsBK) {
            pw.print(uid);
            pw.print(", ");
        }
        pw.println(" }");
        pw.print("  mVisibleUids: {");
        for (Integer uid2 : this.mVisibleUids) {
            pw.print(uid2);
            pw.print(", ");
        }
        pw.println(" }");
    }

    public void updateWindowDisplayFlag(int flag) {
        synchronized (this.mWmService.mGlobalLock) {
            try {
                WindowManagerService.boostPriorityForLockedSection();
                if (this.mCurrentFocusWindow != null && this.mCurrentFocusWindow.mAppToken != null && this.mCurrentFocusWindow.mAppToken.mActivityRecord != null) {
                    this.mCurrentFocusWindow.mAppToken.mActivityRecord.info.getSmtEx().autoDisplayFlags = flag;
                }
            } finally {
                WindowManagerService.resetPriorityAfterLockedSection();
            }
        }
    }

    public boolean isPreExitPcMode() {
        return false;
    }

    public void setIsStartFromHome(boolean b) {
        this.mIsStartFromHome = b;
    }

    public void forceUpdateAllWindowVisibleTime(final boolean screenOn) {
        synchronized (this.mWmService.mGlobalLock) {
            try {
                WindowManagerService.boostPriorityForLockedSection();
                DisplayContent displayContent = this.mWmService.getDefaultDisplayContentLocked();
                displayContent.forAllWindows(w -> {
                    w.getWindowStateSmtBase().forceUpdateVisibleTime(screenOn);
                }, true);
            } finally {
                WindowManagerService.resetPriorityAfterLockedSection();
            }
        }
    }

    public void onDisplayFpsModeChanged(final int displayMode) {
        synchronized (this.mWmService.mGlobalLock) {
            try {
                WindowManagerService.boostPriorityForLockedSection();
                DisplayContent displayContent = this.mWmService.getDefaultDisplayContentLocked();
                displayContent.forAllWindows(w -> {
                    w.getWindowStateSmtBase().onDisplayFpsModeChanged(displayMode);
                }, true);
            } finally {
                WindowManagerService.resetPriorityAfterLockedSection();
            }
        }
    }
}
