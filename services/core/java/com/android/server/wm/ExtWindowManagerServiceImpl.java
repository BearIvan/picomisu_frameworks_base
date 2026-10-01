// Copyright 2026 Picomisu contributors
// SPDX-License-Identifier: Apache-2.0
package com.android.server.wm;

import android.graphics.Rect;
import android.os.IBinder;
import android.os.Parcel;
import android.os.RemoteCallback;
import android.util.Slog;

import com.android.server.api.ApiLayerService;

import java.util.ArrayList;
import java.util.List;

/**
 * PICO window manager service extension (factory PICO OS 5.13.7
 * com.android.server.wm.ExtWindowManagerServiceImpl): the PICO IWindowManager transactions, the
 * IME target/visibility relay from InputMethodManagerService, the display frame of the IME
 * target of selected apps, the disabled strict-mode border and SystemExt display focus.
 *
 * Not ported: the factory ScenesStateListener registered on ApiLayerService (it fills the
 * visible app list sent to the {@link #CODE_REGISTER_VISIBLE_APP_CHANGED_CALLBACK} callbacks;
 * the Source API layer has no scenes listener yet) and {@link #CODE_ENABLE_DEBUG}, which needs
 * the runtime-switchable WM/AM/ATM debug configuration of the factory.
 */
public class ExtWindowManagerServiceImpl implements IExtWindowManagerService {
    public static final int CODE_ENABLE_DEBUG = 10001;
    public static final int CODE_GET_API_LAYER = 10003;
    public static final int CODE_REGISTER_VISIBLE_APP_CHANGED_CALLBACK = 10004;
    private static final String DESCRIPTOR = "android.view.IWindowManager";
    private static final String TAG = "WindowManager";
    private static final List<String> sAdjustGetDisplayFrameList = new ArrayList<>();

    private WindowManagerService mBase;
    private List<RemoteCallback> mVisibleAppChangedCallbackList = new ArrayList<>();

    static {
        sAdjustGetDisplayFrameList.add("com.xwms.pplevel");
    }

    public ExtWindowManagerServiceImpl(WindowManagerService base) {
        mBase = base;
    }

    /** Called by WindowManagerService.onTransact for codes IWindowManager does not know. */
    @Override
    public boolean onTransact(int code, Parcel data, Parcel reply, int flags) {
        if (code == CODE_GET_API_LAYER) {
            // The PICO SDK reads an int flag and the IApiLayer binder without an exception
            // header.
            data.enforceInterface(DESCRIPTOR);
            try {
                IBinder binder = ApiLayerService.getInstance().getApiLayer();
                if (binder != null) {
                    reply.writeInt(1);
                    reply.writeStrongBinder(binder);
                } else {
                    reply.writeInt(0);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            return true;
        }
        if (code == CODE_REGISTER_VISIBLE_APP_CHANGED_CALLBACK) {
            data.enforceInterface(DESCRIPTOR);
            if (data.readInt() > 0) {
                RemoteCallback callback = RemoteCallback.CREATOR.createFromParcel(data);
                registerVisibleAppChangedCallback(callback);
            }
            return true;
        }
        return false;
    }

    @Override
    public boolean disableShowStrictModeViolation() {
        return true;
    }

    /** InputMethodManagerService reports the window token of the current IME target. */
    @Override
    public void notifyImeTargetChanged(final IBinder target) {
        mBase.mH.post(() -> mBase.mRoot.getExt().onImeTargetChanged(target));
    }

    /** InputMethodManagerService reports that the IME was shown or hidden. */
    @Override
    public void notifyImeVisibleChanged(boolean visible) {
        mBase.mRoot.getExt().onImeVisibleChanged(visible);
    }

    /**
     * WindowManagerService.getWindowDisplayFrame: the display frame of the window, 50 px shorter
     * for the listed apps while they are the target of the shown IME.
     */
    @Override
    public boolean adjustGetWindowDisplayFrame(WindowState win, Rect outDisplayFrame) {
        Rect displayFrame = new Rect(win.getDisplayFrameLw());
        String pkg = win.getAttrs().packageName;
        if (sAdjustGetDisplayFrameList.contains(pkg)
                && mBase.mRoot.getExt().getInputMethodTargetWindow() == win) {
            displayFrame.bottom -= 50;
        }
        outDisplayFrame.set(displayFrame);
        return true;
    }

    private void registerVisibleAppChangedCallback(RemoteCallback callback) {
        synchronized (mVisibleAppChangedCallbackList) {
            mVisibleAppChangedCallbackList.add(callback);
        }
    }

    /** SystemExt (USER_UPDATE_DISPLAY_FOCUS) focuses the top task of a display. */
    @Override
    public void updateDisplayFocus(int displayId, String reason) {
        synchronized (mBase.mGlobalLock) {
            try {
                WindowManagerService.boostPriorityForLockedSection();
                DisplayContent dc = mBase.mRoot.getDisplayContent(displayId);
                if (dc == null) {
                    Slog.w(TAG, "updateDisplayFocus failed by dc is null");
                    return;
                }
                // The factory dereferences the top stack without a check.
                final TaskStack stack = dc.getTopStack();
                Task task = stack != null ? stack.getTopChild() : null;
                if (task == null) {
                    Slog.w(TAG, "updateDisplayFocus failed by getTopRootTask is null");
                    return;
                }
                Slog.w(TAG, "updateDisplayFocus displayId " + displayId + ", task " + task
                        + ", reason[" + reason + "]");
                mBase.mAtmService.setFocusedTask(task.mTaskId);
            } finally {
                WindowManagerService.resetPriorityAfterLockedSection();
            }
        }
    }
}
