// Copyright 2026 Picomisu contributors
// SPDX-License-Identifier: Apache-2.0
package com.android.server.wm;

import android.graphics.Rect;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Parcel;
import android.os.RemoteCallback;
import android.os.SystemProperties;
import android.text.TextUtils;
import android.util.Slog;

import com.android.internal.app.RunningAppInfo;
import com.android.internal.app.ScenesStateListener;
import com.android.internal.os.BackgroundThread;
import com.android.server.am.ActivityManagerDebugConfig;
import com.android.server.api.ApiLayerService;
import com.android.server.inputmethod.InputMethodManagerService;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * PICO window manager service extension (factory PICO OS 5.13.7
 * com.android.server.wm.ExtWindowManagerServiceImpl): the PICO IWindowManager transactions, the
 * IME target/visibility relay from InputMethodManagerService, the display frame of the IME
 * target of selected apps, the disabled strict-mode border, SystemExt display focus, the
 * visible app callbacks (fed by a scenes state listener on the API layer) and the run-time
 * debug switch of debuggable builds ({@link #CODE_ENABLE_DEBUG}).
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

    /**
     * Keeps the visible apps (the shown 3D app and the visible 2D apps), the seethrough state
     * and the XR runtime display state, and sends them to the registered callbacks
     * ("visible_app_list", "seethrough_status", "xr_runtime_display_state") on every change
     * (factory anonymous ExtWindowManagerServiceImpl$1).
     */
    private ScenesStateListener mScenesStateListener = new ScenesStateListener() {
        private int mSeethroughState;
        private String mShowing3dApp;
        private List<String> mVisible2dAppList = new ArrayList<>();
        private int mXrRuntimeDisplayState;

        @Override
        public void onRunning2dAppChanged(List<RunningAppInfo> visible2dAppList) {
            List<String> visible2dApps = new ArrayList<>();
            for (RunningAppInfo info : visible2dAppList) {
                if (info != null && info.visible && !TextUtils.isEmpty(info.packageName)) {
                    visible2dApps.add(info.packageName);
                }
            }
            mVisible2dAppList.clear();
            mVisible2dAppList.addAll(visible2dApps);
            dispatchVisibleAppChanged();
        }

        @Override
        public void on3dAppDisplayStateChanged(String showing3dApp, int xrRuntimeDisplayState) {
            mShowing3dApp = showing3dApp;
            mXrRuntimeDisplayState = xrRuntimeDisplayState;
            dispatchVisibleAppChanged();
        }

        @Override
        public void onSeethroughStateChanged(int state) {
            mSeethroughState = state;
            dispatchVisibleAppChanged();
        }

        private void dispatchVisibleAppChanged() {
            ArrayList<String> visibleAppList = new ArrayList<>();
            if (!TextUtils.isEmpty(mShowing3dApp)) {
                visibleAppList.add(mShowing3dApp);
            }
            visibleAppList.addAll(mVisible2dAppList);
            final Bundle result = new Bundle();
            result.putStringArrayList("visible_app_list", visibleAppList);
            result.putInt("seethrough_status", mSeethroughState);
            result.putInt("xr_runtime_display_state", mXrRuntimeDisplayState);
            BackgroundThread.getHandler().post(() -> {
                synchronized (mVisibleAppChangedCallbackList) {
                    for (RemoteCallback callback : mVisibleAppChangedCallbackList) {
                        callback.sendResult(result);
                    }
                }
            });
        }
    };

    static {
        sAdjustGetDisplayFrameList.add("com.xwms.pplevel");
    }

    public ExtWindowManagerServiceImpl(WindowManagerService base) {
        mBase = base;
        ApiLayerService.getInstance().registerScenesStateListener(mScenesStateListener);
    }

    /** Called by WindowManagerService.onTransact for codes IWindowManager does not know. */
    @Override
    public boolean onTransact(int code, Parcel data, Parcel reply, int flags) {
        if (code == CODE_ENABLE_DEBUG) {
            data.enforceInterface(DESCRIPTOR);
            try {
                String cmd = data.readString();
                enableDebug(cmd);
            } catch (Exception e) {
                e.printStackTrace();
            }
            return true;
        }
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

    /**
     * Debuggable builds only: resets the WM, AM, ATM and IMMS debug flags, then sets the DEBUG*
     * fields named in {@code cmd} ("wm:DEBUG_FOCUS,DEBUG_LAYOUT%atm:DEBUG_TASKS%imms:DEBUG").
     */
    private void enableDebug(String cmd) {
        if (!"1".equals(SystemProperties.get("ro.debuggable"))) {
            return;
        }
        WindowManagerDebugConfig.reset();
        ActivityManagerDebugConfig.reset();
        ActivityTaskManagerDebugConfig.reset();
        InputMethodManagerService.DEBUG = false;
        if (TextUtils.isEmpty(cmd)) {
            return;
        }
        Map<String, Class> debugMap = new HashMap<>();
        debugMap.put("wm", WindowManagerDebugConfig.class);
        debugMap.put("am", ActivityManagerDebugConfig.class);
        debugMap.put("atm", ActivityTaskManagerDebugConfig.class);
        debugMap.put("imms", InputMethodManagerService.class);
        Slog.w(TAG, "enableDebug [" + cmd + "]");
        for (String str : cmd.split("%")) {
            if (TextUtils.isEmpty(str)) {
                continue;
            }
            String[] arr = str.split(":");
            Class debugClass;
            if (arr.length != 2 || (debugClass = debugMap.get(arr[0])) == null) {
                continue;
            }
            Slog.w(TAG, "enable debug on [" + arr[0] + "], params [" + arr[1] + "]");
            for (String debugName : arr[1].split(",")) {
                if (TextUtils.isEmpty(debugName)) {
                    continue;
                }
                String name = debugName.trim();
                if (!name.contains("DEBUG")) {
                    continue;
                }
                Slog.w(TAG, "open debug [" + name + "]");
                try {
                    Field field = debugClass.getDeclaredField(name);
                    field.setBoolean(null, true);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
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
                // As on the factory, the top stack is dereferenced without a check.
                Task task = dc.getTopStack().getTopChild();
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
