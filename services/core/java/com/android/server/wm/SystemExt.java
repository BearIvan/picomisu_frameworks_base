/*
 * Copyright 2026 Picomisu contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package com.android.server.wm;

import android.app.ActivityManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Looper;
import android.os.Message;
import android.os.Parcel;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemProperties;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Slog;

import com.android.server.LocalServices;
import com.android.server.api.ApiLayerService;
import com.android.server.inputmethod.InputMethodManagerInternal;

import java.util.ArrayList;

/**
 * system_server side of the PICO VR 2D app shell (factory PICO OS 5.13.7
 * com.android.server.wm.SystemExt). The SystemExt app (com.picovr.systemext) publishes the
 * "native_shell" service; system_server asks it whether an activity may start
 * ({@link #handleStartActivity}), lets it create the virtual display a 2D activity runs on
 * ({@link #requestCreateVirtualDisplay}) and reports task, display and screen changes. The app
 * calls back through {@link ClientBinder}. Every call is transaction
 * {@link #CODE_SYSTEM_EXT_CLIENT} with interface token "com.bytedance.IRemoteCallback" and the
 * action as the first int, as in the factory.
 */
public class SystemExt implements IBinder.DeathRecipient {
    private static final String TAG = "SystemExt";
    private static final String DESCRIPTOR = "com.bytedance.IRemoteCallback";
    private static final int CODE_SYSTEM_EXT_CLIENT = 400002;

    private static final int ACTION_WMS_INIT = 0;
    private static final int ACTION_CREATE_VIRTUAL_DISPLAY = 1;
    private static final int ACTION_NOTIFY_VIRTUAL_DISPLAY_VISIBILITY_CHANGED = 2;
    private static final int ACTION_NOTIFY_VRSHELL_RESUMED = 3;
    private static final int ACTION_NOTIFY_TASK_MOVED_TO_FRONT = 4;
    private static final int ACTION_NOTIFY_TASK_REMOVED = 5;
    private static final int ACTION_NOTIFY_TASK_EMPTY = 6;
    private static final int ACTION_SCREEN_STATE_CHANGE = 7;
    private static final int ACTION_RESIZE_VIRTUAL_DISPLAY = 8;
    private static final int ACTION_START_ACTIVITY = 9;
    private static final int ACTION_DESTROY_ACTIVITY = 10;
    private static final int ACTION_NOTIFY_FOCUS_CHANGED = 11;

    public static final int START_SUCCESS = 0;
    public static final int START_CANCELED = 1;
    public static final int START_PENDING = 2;

    public static final String sAction = "picovr.system_ext.action.HOME";
    public static final String sCurrentPkg = "com.picovr.systemext";
    private static final boolean DEBUG_SYSTEMEXT = false;

    private final ActivityTaskManagerService mService;
    private final Handler mHandler;
    private volatile IBinder mNativeShellService;
    private final IBinder mClientBinder = new ClientBinder();
    private boolean mSystemExtDied = false;

    private final BroadcastReceiver mScreenStateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (Intent.ACTION_SCREEN_OFF.equals(action)) {
                notifyScreenStateChanged(false);
            } else if (Intent.ACTION_SCREEN_ON.equals(action)) {
                notifyScreenStateChanged(true);
            }
        }
    };

    public SystemExt(ActivityTaskManagerService service) {
        mService = service;
        mHandler = new StartHandler(mService.mH.getLooper());
    }

    public void onSystemReady() {
        final IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_SCREEN_ON);
        filter.addAction(Intent.ACTION_SCREEN_OFF);
        mService.mContext.registerReceiver(mScreenStateReceiver, filter);
        Settings.Global.putInt(mService.mContext.getContentResolver(), "systemext_enabled", 1);
    }

    @Override
    public void binderDied() {
        synchronized (mService.mGlobalLock) {
            try {
                WindowManagerService.boostPriorityForLockedSection();
                if (mNativeShellService != null) {
                    Slog.i(TAG, "died");
                    mNativeShellService.unlinkToDeath(this, 0);
                    mNativeShellService = null;
                    mSystemExtDied = true;
                    mService.getActivityStartController().getExt().onSystemExtDied();
                    SystemProperties.set("pvr.boot.start.tobapp", "0");
                }
                mHandler.removeMessages(StartHandler.RETRY_GET_NATIVE_SHELL_SERVICE_MSG);
                mHandler.sendEmptyMessageDelayed(StartHandler.RETRY_GET_NATIVE_SHELL_SERVICE_MSG,
                        1000);
            } finally {
                WindowManagerService.resetPriorityAfterLockedSection();
            }
        }
    }

    /** Connects to the SystemExt app's "native_shell" service; retries every second until up. */
    public boolean checkClientService() {
        if (mNativeShellService == null) {
            // checkService: this runs under the global lock on activity starts, so it must not
            // wait for the service like getService() does.
            mNativeShellService = ServiceManager.checkService("native_shell");
            Slog.i(TAG, "checkNativeShellService : " + mNativeShellService);
            if (mNativeShellService != null) {
                requestInit(mClientBinder);
                try {
                    mNativeShellService.linkToDeath(this, 0);
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
                if (mSystemExtDied) {
                    mSystemExtDied = false;
                    clear2DTaskOnMainScreen();
                }
            } else {
                mHandler.removeMessages(StartHandler.RETRY_GET_NATIVE_SHELL_SERVICE_MSG);
                mHandler.sendEmptyMessageDelayed(StartHandler.RETRY_GET_NATIVE_SHELL_SERVICE_MSG,
                        1000);
            }
        }
        return mNativeShellService != null;
    }

    private void clear2DTaskOnMainScreen() {
        final ArrayList<Integer> taskIds = new ArrayList<>();
        final ActivityDisplay defaultDisplay = mService.mRootActivityContainer.getDefaultDisplay();
        for (int stackNdx = defaultDisplay.getChildCount() - 1; stackNdx >= 0; stackNdx--) {
            final ActivityRecord r = defaultDisplay.getChildAt(stackNdx).topRunningActivityLocked();
            if (r != null) {
                if (r.info.getExt().isVrActivity()) {
                    break;
                }
                Slog.i(TAG, "clear task : " + r.getTaskRecord());
                taskIds.add(r.getTaskRecord().taskId);
            }
        }
        for (int taskId : taskIds) {
            mService.mStackSupervisor.removeTaskByIdLocked(taskId, false, true,
                    "remove-task-by-SystemExt");
        }
    }

    /** A prepared call: token and action written; {@link #call} sends it. */
    private Parcel obtain(int action) {
        final Parcel data = Parcel.obtain();
        data.writeInterfaceToken(DESCRIPTOR);
        data.writeInt(action);
        return data;
    }

    /** Sends {@code data} (recycled here); returns the reply int, or {@code def} on failure. */
    private int call(Parcel data, boolean oneway, int def) {
        final IBinder service = mNativeShellService;
        final Parcel reply = Parcel.obtain();
        try {
            if (service == null) {
                return def;
            }
            service.transact(CODE_SYSTEM_EXT_CLIENT, data, reply,
                    oneway ? IBinder.FLAG_ONEWAY : 0);
            return oneway ? def : reply.readInt();
        } catch (RemoteException e) {
            e.printStackTrace();
            return def;
        } finally {
            reply.recycle();
            data.recycle();
        }
    }

    private void requestInit(IBinder clientBinder) {
        final Parcel data = obtain(ACTION_WMS_INIT);
        data.writeStrongBinder(clientBinder);
        final Parcel reply = Parcel.obtain();
        try {
            mNativeShellService.transact(CODE_SYSTEM_EXT_CLIENT, data, reply, 0);
        } catch (RemoteException e) {
            e.printStackTrace();
        } finally {
            reply.recycle();
            data.recycle();
        }
    }

    public int handleStartActivity(long seq, Intent intent, ActivityInfo startActivity,
            ActivityInfo sourceActivity, int targetDisplayId, int callingDisplayId,
            String callingPackage, boolean resumed) {
        if (mNativeShellService == null) {
            return START_SUCCESS;
        }
        final Parcel data = obtain(ACTION_START_ACTIVITY);
        data.writeLong(seq);
        startActivity.writeToParcel(data, 0);
        if (sourceActivity != null) {
            data.writeInt(1);
            sourceActivity.writeToParcel(data, 0);
        } else {
            data.writeInt(0);
        }
        data.writeInt(targetDisplayId);
        data.writeInt(callingDisplayId);
        if (callingPackage != null) {
            data.writeInt(1);
            data.writeString(callingPackage);
        } else {
            data.writeInt(0);
        }
        if (intent != null) {
            data.writeInt(1);
            intent.writeToParcel(data, 0);
        } else {
            data.writeInt(0);
        }
        data.writeBoolean(resumed);
        return call(data, false, START_SUCCESS);
    }

    public void handleDestroyActivity(ActivityInfo activityInfo) {
        if (mNativeShellService == null) {
            return;
        }
        final Parcel data = obtain(ACTION_DESTROY_ACTIVITY);
        activityInfo.writeToParcel(data, 0);
        call(data, false, 0);
    }

    public int requestCreateVirtualDisplay(ActivityRecord startActivity,
            ActivityRecord sourceRecord) {
        if (mNativeShellService == null) {
            return -1;
        }
        int callingDisplayId = sourceRecord != null ? sourceRecord.getDisplayId() : -1;
        final String callingPackage = startActivity.launchedFromPackage;
        final int parentDisplayId = findParentDisplayId(callingDisplayId, callingPackage);
        if (parentDisplayId != -1) {
            callingDisplayId = parentDisplayId;
        }
        final ActivityInfo activityInfo = startActivity.info;
        Slog.i(TAG, "requestCreateVirtualDisplay : " + activityInfo.packageName + ", "
                + activityInfo.name + ", callingDisplayId : " + callingDisplayId
                + ", callingPackage : " + callingPackage);
        final Parcel data = obtain(ACTION_CREATE_VIRTUAL_DISPLAY);
        activityInfo.writeToParcel(data, 0);
        data.writeInt(callingDisplayId);
        data.writeString(callingPackage);
        return call(data, false, -1);
    }

    public void notifyTaskMovedToFront(int displayId, ActivityManager.RunningTaskInfo taskInfo) {
        if (mNativeShellService == null) {
            return;
        }
        final Parcel data = obtain(ACTION_NOTIFY_TASK_MOVED_TO_FRONT);
        data.writeInt(displayId);
        taskInfo.writeToParcel(data, 0);
        call(data, true, 0);
    }

    public void notifyTaskRemoved(int displayId, ActivityManager.RunningTaskInfo taskInfo) {
        if (mNativeShellService == null) {
            return;
        }
        final Parcel data = obtain(ACTION_NOTIFY_TASK_REMOVED);
        data.writeInt(displayId);
        data.writeInt(taskInfo.taskId);
        call(data, true, 0);
    }

    public void notifyTaskEmpty(int displayId) {
        if (mNativeShellService == null) {
            return;
        }
        final Parcel data = obtain(ACTION_NOTIFY_TASK_EMPTY);
        data.writeInt(displayId);
        call(data, true, 0);
    }

    public void notifyVirtualDisplayVisibilityChanged(int displayId, boolean isVisible) {
        if (mNativeShellService == null) {
            return;
        }
        if (DEBUG_SYSTEMEXT) {
            Slog.i(TAG, "notifyVirtualDisplayVisibilityChanged displayId: " + displayId
                    + ", isVisible : " + isVisible);
        }
        final Parcel data = obtain(ACTION_NOTIFY_VIRTUAL_DISPLAY_VISIBILITY_CHANGED);
        data.writeInt(displayId);
        data.writeInt(isVisible ? 1 : 0);
        call(data, true, 0);
    }

    public void notifyVrShellResumed() {
        if (mNativeShellService == null) {
            return;
        }
        Slog.i(TAG, "on vrshell resumed");
        call(obtain(ACTION_NOTIFY_VRSHELL_RESUMED), true, 0);
    }

    public void notifyScreenStateChanged(boolean screenOn) {
        ApiLayerService.getInstance().updateScreenState(screenOn);
        if (mNativeShellService == null) {
            return;
        }
        final Parcel data = obtain(ACTION_SCREEN_STATE_CHANGE);
        data.writeInt(screenOn ? 1 : 0);
        call(data, true, 0);
    }

    public void notifyResizeVirtualDisplay(int displayId, int reqOrientation) {
        if (mNativeShellService == null) {
            return;
        }
        Slog.i(TAG, "notifyResizeVirtualDisplay displayId : " + displayId + ", reqOrientation : "
                + reqOrientation);
        final Parcel data = obtain(ACTION_RESIZE_VIRTUAL_DISPLAY);
        data.writeInt(displayId);
        data.writeInt(reqOrientation);
        call(data, true, 0);
    }

    public void notifyFocusDisplayChanged(int displayId) {
        if (mNativeShellService == null) {
            return;
        }
        Slog.i(TAG, "notifyFocusDisplayChanged displayId : " + displayId);
        final Parcel data = obtain(ACTION_NOTIFY_FOCUS_CHANGED);
        data.writeInt(displayId);
        call(data, true, 0);
    }

    private int findParentDisplayId(int callingDisplayId, String callingPackage) {
        if (callingDisplayId == 0) {
            return -1;
        }
        final DisplayContent displayContent;
        if (callingDisplayId == -1) {
            final WindowState focusWindow =
                    mService.mWindowManager.mRoot.getTopFocusedDisplayContent().mCurrentFocus;
            if (TextUtils.isEmpty(callingPackage) || focusWindow == null
                    || !callingPackage.equals(focusWindow.getOwningPackage())) {
                return -1;
            }
            displayContent = focusWindow.getDisplayContent();
        } else {
            displayContent = mService.mWindowManager.mRoot.getDisplayContent(callingDisplayId);
        }
        if (displayContent == null || displayContent.getDisplay().getExt().isVr2dDisplay()) {
            return -1;
        }
        final WindowState parentWindow = displayContent.getParentWindow();
        final DisplayContent parentDisplay =
                parentWindow != null ? parentWindow.getDisplayContent() : null;
        return parentDisplay != null ? parentDisplay.getDisplayId() : -1;
    }

    /** Factory ExtWindowManagerServiceImpl.updateDisplayFocus. */
    private void updateDisplayFocus(int displayId, String reason) {
        synchronized (mService.mGlobalLock) {
            try {
                WindowManagerService.boostPriorityForLockedSection();
                final DisplayContent dc = mService.mWindowManager.mRoot.getDisplayContent(displayId);
                if (dc == null) {
                    Slog.w(TAG, "updateDisplayFocus failed by dc is null");
                    return;
                }
                final TaskStack stack = dc.getTopStack();
                final Task task = stack != null ? stack.getTopChild() : null;
                if (task == null) {
                    Slog.w(TAG, "updateDisplayFocus failed by getTopRootTask is null");
                    return;
                }
                Slog.w(TAG, "updateDisplayFocus displayId " + displayId + ", task " + task
                        + ", reason[" + reason + "]");
                mService.setFocusedTask(task.mTaskId);
            } finally {
                WindowManagerService.resetPriorityAfterLockedSection();
            }
        }
    }

    private final class StartHandler extends Handler {
        static final int RETRY_GET_NATIVE_SHELL_SERVICE_MSG = 1;

        StartHandler(Looper looper) {
            super(looper, null, true);
        }

        @Override
        public void handleMessage(Message msg) {
            if (msg.what == RETRY_GET_NATIVE_SHELL_SERVICE_MSG) {
                synchronized (mService.mGlobalLock) {
                    try {
                        WindowManagerService.boostPriorityForLockedSection();
                        checkClientService();
                    } finally {
                        WindowManagerService.resetPriorityAfterLockedSection();
                    }
                }
            }
        }
    }

    /** Calls from the SystemExt app into system_server. */
    private class ClientBinder extends Binder implements IInterface {
        private static final int CODE_WMS_MOVE_DISPLAY_TO_BACK = 1000;
        private static final int CODE_WMS_MOVE_DISPLAY_TO_FRONT = 1001;
        private static final int CODE_WMS_APP_SWITCH_ALLOWED_RESULT = 1003;
        private static final int CODE_WMS_FORCE_HIDE_SOFT_INPUT_METHOD = 1004;
        private static final int CODE_WMS_GET_DEFAULT_DISPLAY_TOP_RUNNING_TASK = 1005;
        private static final int CODE_WMS_APP_SWITCH_ALLOWED_RESULT_BATCH = 1006;
        private static final int CODE_WMS_BEFORE_EXIT_3D_APP = 1007;
        private static final int CODE_WMS_UPDATE_DISPLAY_FOCUS = 1008;

        ClientBinder() {
            attachInterface(this, DESCRIPTOR);
        }

        private void setVirtualDisplayVisible(int displayId, boolean visible) {
            final long callingId = Binder.clearCallingIdentity();
            try {
                synchronized (mService.mGlobalLock) {
                    try {
                        WindowManagerService.boostPriorityForLockedSection();
                        mService.getActivityStartController().getExt()
                                .handleClientVirtualDisplayVisibilityChanged(displayId, visible);
                    } finally {
                        WindowManagerService.resetPriorityAfterLockedSection();
                    }
                }
            } finally {
                Binder.restoreCallingIdentity(callingId);
            }
        }

        @Override
        protected boolean onTransact(int code, Parcel data, Parcel reply, int flags)
                throws RemoteException {
            switch (code) {
                case CODE_WMS_MOVE_DISPLAY_TO_BACK:
                    data.enforceInterface(DESCRIPTOR);
                    setVirtualDisplayVisible(data.readInt(), false);
                    return true;
                case CODE_WMS_MOVE_DISPLAY_TO_FRONT:
                    data.enforceInterface(DESCRIPTOR);
                    setVirtualDisplayVisible(data.readInt(), true);
                    return true;
                case CODE_WMS_APP_SWITCH_ALLOWED_RESULT: {
                    data.enforceInterface(DESCRIPTOR);
                    final long seq = data.readLong();
                    final boolean allowed = data.readInt() != 0;
                    mService.getActivityStartController().getExt()
                            .handleClientActivityOrTaskSwitchAllowedResult(seq, allowed);
                    return true;
                }
                case CODE_WMS_FORCE_HIDE_SOFT_INPUT_METHOD:
                    try {
                        final InputMethodManagerInternal imm =
                                LocalServices.getService(InputMethodManagerInternal.class);
                        if (imm != null) {
                            imm.hideCurrentInputMethod();
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    return true;
                case CODE_WMS_GET_DEFAULT_DISPLAY_TOP_RUNNING_TASK: {
                    data.enforceInterface(DESCRIPTOR);
                    final ActivityManager.RunningTaskInfo taskInfo =
                            mService.getActivityStartController().getExt()
                                    .getDefaultDisplayTopTaskInfo();
                    if (taskInfo == null) {
                        reply.writeInt(0);
                    } else {
                        reply.writeInt(1);
                        taskInfo.writeToParcel(reply, 0);
                    }
                    return true;
                }
                case CODE_WMS_APP_SWITCH_ALLOWED_RESULT_BATCH: {
                    data.enforceInterface(DESCRIPTOR);
                    final int length = data.readInt();
                    if (length > 0) {
                        final long[] seqArray = new long[length];
                        data.readLongArray(seqArray);
                        mService.getActivityStartController().getExt()
                                .handleClientActivityOrTaskBatchAllowedResult(seqArray);
                    }
                    return true;
                }
                case CODE_WMS_UPDATE_DISPLAY_FOCUS:
                    data.enforceInterface(DESCRIPTOR);
                    Slog.w(TAG, "CODE_WMS_UPDATE_DISPLAY_FOCUS");
                    updateDisplayFocus(data.readInt(), "USER_UPDATE_DISPLAY_FOCUS");
                    return true;
                default:
                    // 1002 (MSG_DO_TRANVERSAL) and 1007 are not handled here in the factory either.
                    return super.onTransact(code, data, reply, flags);
            }
        }

        @Override
        public IBinder asBinder() {
            return this;
        }
    }
}
