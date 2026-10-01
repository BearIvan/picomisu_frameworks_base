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
import android.content.pm.PackageManager;
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
 * action as the first int; each method builds its parcels as on the factory.
 */
public class SystemExt implements IBinder.DeathRecipient {
    private static final int ACTION_CREATE_VIRTUAL_DISPLAY = 1;
    private static final int ACTION_DESTROY_ACTIVITY = 10;
    private static final int ACTION_NOTIFY_FOCUS_CHANGED = 11;
    private static final int ACTION_NOTIFY_TASK_EMPTY = 6;
    private static final int ACTION_NOTIFY_TASK_MOVED_TO_FRONT = 4;
    private static final int ACTION_NOTIFY_TASK_REMOVED = 5;
    private static final int ACTION_NOTIFY_VIRTUAL_DISPLAY_VISIBILITY_CHANGED = 2;
    private static final int ACTION_NOTIFY_VRSHELL_RESUMED = 3;
    private static final int ACTION_RESIZE_VIRTUAL_DISPLAY = 8;
    private static final int ACTION_SCREEN_STATE_CHANGE = 7;
    private static final int ACTION_START_ACTIVITY = 9;
    private static final int ACTION_WMS_INIT = 0;
    private static final int CODE_SYSTEM_EXT_CLIENT = 400002;
    private static boolean DEBUG_SYSTEMEXT = false;
    public static final int START_CANCELED = 1;
    public static final int START_PENDING = 2;
    public static final int START_SUCCESS = 0;
    private static final String TAG = "SystemExt";
    public static final String sAction = "picovr.system_ext.action.HOME";
    public static final String sCurrentPkg = "com.picovr.systemext";
    public static boolean sUseSystemExt;
    private final Handler mHandler;
    private volatile IBinder mNativeShellService;
    private PackageManager mPM;
    private ActivityTaskManagerService mService;
    private BroadcastReceiver mScreenStateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (Intent.ACTION_SCREEN_OFF.equals(action)) {
                notifyScreenStateChanged(false);
            } else if (Intent.ACTION_SCREEN_ON.equals(action)) {
                notifyScreenStateChanged(true);
            }
        }
    };
    private IBinder mClientBinder = new ClientBinder();
    private boolean mSystemExtDied = false;

    public SystemExt(ActivityTaskManagerService service) {
        mService = service;
        mHandler = new StartHandler(mService.mH.getLooper());
    }

    public void onSystemReady() {
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(Intent.ACTION_SCREEN_ON);
        intentFilter.addAction(Intent.ACTION_SCREEN_OFF);
        mService.mContext.registerReceiver(mScreenStateReceiver, intentFilter);
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
                        1000L);
            } finally {
                WindowManagerService.resetPriorityAfterLockedSection();
            }
        }
    }

    /** Connects to the SystemExt app's "native_shell" service; retries every second until up. */
    public boolean checkClientService() {
        if (mNativeShellService == null) {
            mNativeShellService = ServiceManager.getService("native_shell");
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
                        1000L);
            }
        }
        return mNativeShellService != null;
    }

    /** After SystemExt died: remove the 2D tasks above the top VR task on display 0. */
    private void clear2DTaskOnMainScreen() {
        ArrayList<Integer> taskIds = new ArrayList<>();
        ActivityDisplay defaultDisplay = mService.mRootActivityContainer.getDefaultDisplay();
        for (int stackNdx = defaultDisplay.getChildCount() - 1; stackNdx >= 0; stackNdx--) {
            ActivityStack stack = defaultDisplay.getChildAt(stackNdx);
            ActivityRecord activityRecord = stack.topRunningActivityLocked();
            if (activityRecord != null) {
                if (activityRecord.info.getExt().isVrActivity()) {
                    break;
                }
                Slog.i(TAG, "clear task : " + activityRecord.getTaskRecord());
                taskIds.add(activityRecord.getTaskRecord().taskId);
            }
        }
        for (int taskId : taskIds) {
            mService.mStackSupervisor.removeTaskByIdLocked(taskId, false, true,
                    "remove-task-by-SystemExt");
        }
    }

    private void requestInit(IBinder clientBinder) {
        Parcel data = Parcel.obtain();
        Parcel reply = Parcel.obtain();
        try {
            data.writeInterfaceToken("com.bytedance.IRemoteCallback");
            data.writeInt(ACTION_WMS_INIT);
            data.writeStrongBinder(clientBinder);
            mNativeShellService.transact(CODE_SYSTEM_EXT_CLIENT, data, reply, 0);
        } catch (RemoteException e) {
            e.printStackTrace();
        } finally {
            reply.recycle();
            data.recycle();
        }
    }

    /** Asks SystemExt whether an activity may start: START_SUCCESS, _CANCELED or _PENDING. */
    public int handleStartActivity(long seq, Intent intent, ActivityInfo startActivity,
            ActivityInfo sourceActivity, int targetDisplayId, int callingDisplayId,
            String callingPackage, boolean resumed) {
        if (mNativeShellService == null) {
            return START_SUCCESS;
        }
        Parcel data = Parcel.obtain();
        Parcel reply = Parcel.obtain();
        try {
            data.writeInterfaceToken("com.bytedance.IRemoteCallback");
            data.writeInt(ACTION_START_ACTIVITY);
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
            mNativeShellService.transact(CODE_SYSTEM_EXT_CLIENT, data, reply, 0);
            return reply.readInt();
        } catch (RemoteException e) {
            e.printStackTrace();
            return START_SUCCESS;
        } finally {
            reply.recycle();
            data.recycle();
        }
    }

    public void handleDestroyActivity(ActivityInfo activityInfo) {
        if (mNativeShellService == null) {
            return;
        }
        Parcel data = Parcel.obtain();
        Parcel reply = Parcel.obtain();
        try {
            data.writeInterfaceToken("com.bytedance.IRemoteCallback");
            data.writeInt(ACTION_DESTROY_ACTIVITY);
            activityInfo.writeToParcel(data, 0);
            mNativeShellService.transact(CODE_SYSTEM_EXT_CLIENT, data, reply, 0);
        } catch (RemoteException e) {
            e.printStackTrace();
        } finally {
            reply.recycle();
            data.recycle();
        }
    }

    /** Asks SystemExt for a virtual display for a 2D activity; its id, or -1. */
    public int requestCreateVirtualDisplay(ActivityRecord startActivity,
            ActivityRecord sourceRecord) {
        if (mNativeShellService == null) {
            return -1;
        }
        int callingDisplayId = sourceRecord != null ? sourceRecord.getDisplayId() : -1;
        String callingPackage = startActivity.launchedFromPackage;
        int parentDisplayId = findParentDisplayId(callingDisplayId, callingPackage);
        if (parentDisplayId != -1) {
            callingDisplayId = parentDisplayId;
        }
        ActivityInfo activityInfo = startActivity.info;
        Slog.i(TAG, "requestCreateVirtualDisplay : " + activityInfo.packageName + ", "
                + activityInfo.name + ", callingDisplayId : " + callingDisplayId
                + ", callingPackage : " + callingPackage);
        Parcel data = Parcel.obtain();
        Parcel reply = Parcel.obtain();
        try {
            data.writeInterfaceToken("com.bytedance.IRemoteCallback");
            data.writeInt(ACTION_CREATE_VIRTUAL_DISPLAY);
            activityInfo.writeToParcel(data, 0);
            data.writeInt(callingDisplayId);
            data.writeString(callingPackage);
            mNativeShellService.transact(CODE_SYSTEM_EXT_CLIENT, data, reply, 0);
            return reply.readInt();
        } catch (RemoteException e) {
            e.printStackTrace();
            return -1;
        } finally {
            reply.recycle();
            data.recycle();
        }
    }

    public void notifyTaskMovedToFront(int displayId, ActivityManager.RunningTaskInfo taskInfo) {
        if (mNativeShellService == null) {
            return;
        }
        Parcel data = Parcel.obtain();
        Parcel reply = Parcel.obtain();
        try {
            data.writeInterfaceToken("com.bytedance.IRemoteCallback");
            data.writeInt(ACTION_NOTIFY_TASK_MOVED_TO_FRONT);
            data.writeInt(displayId);
            taskInfo.writeToParcel(data, 0);
            mNativeShellService.transact(CODE_SYSTEM_EXT_CLIENT, data, reply,
                    IBinder.FLAG_ONEWAY);
        } catch (RemoteException e) {
            e.printStackTrace();
        } finally {
            reply.recycle();
            data.recycle();
        }
    }

    public void notifyTaskRemoved(int displayId, ActivityManager.RunningTaskInfo taskInfo) {
        if (mNativeShellService == null) {
            return;
        }
        Parcel data = Parcel.obtain();
        Parcel reply = Parcel.obtain();
        try {
            data.writeInterfaceToken("com.bytedance.IRemoteCallback");
            data.writeInt(ACTION_NOTIFY_TASK_REMOVED);
            data.writeInt(displayId);
            data.writeInt(taskInfo.taskId);
            mNativeShellService.transact(CODE_SYSTEM_EXT_CLIENT, data, reply,
                    IBinder.FLAG_ONEWAY);
        } catch (RemoteException e) {
            e.printStackTrace();
        } finally {
            reply.recycle();
            data.recycle();
        }
    }

    public void notifyTaskEmpty(int displayId) {
        if (mNativeShellService == null) {
            return;
        }
        Parcel data = Parcel.obtain();
        Parcel reply = Parcel.obtain();
        try {
            data.writeInterfaceToken("com.bytedance.IRemoteCallback");
            data.writeInt(ACTION_NOTIFY_TASK_EMPTY);
            data.writeInt(displayId);
            mNativeShellService.transact(CODE_SYSTEM_EXT_CLIENT, data, reply,
                    IBinder.FLAG_ONEWAY);
        } catch (RemoteException e) {
            e.printStackTrace();
        } finally {
            reply.recycle();
            data.recycle();
        }
    }

    public void notifyVirtualDisplayVisibilityChanged(int displayId, boolean isVisible) {
        if (mNativeShellService == null) {
            return;
        }
        if (DEBUG_SYSTEMEXT) {
            Slog.i(TAG, "notifyVirtualDisplayVisibilityChanged displayId: " + displayId
                    + ", isVisible : " + isVisible);
        }
        Parcel data = Parcel.obtain();
        Parcel reply = Parcel.obtain();
        try {
            data.writeInterfaceToken("com.bytedance.IRemoteCallback");
            data.writeInt(ACTION_NOTIFY_VIRTUAL_DISPLAY_VISIBILITY_CHANGED);
            data.writeInt(displayId);
            data.writeInt(isVisible ? 1 : 0);
            mNativeShellService.transact(CODE_SYSTEM_EXT_CLIENT, data, reply,
                    IBinder.FLAG_ONEWAY);
        } catch (RemoteException e) {
            e.printStackTrace();
        } finally {
            reply.recycle();
            data.recycle();
        }
    }

    public void notifyVrShellResumed() {
        if (mNativeShellService == null) {
            return;
        }
        Slog.i(TAG, "on vrshell resumed");
        Parcel data = Parcel.obtain();
        Parcel reply = Parcel.obtain();
        try {
            data.writeInterfaceToken("com.bytedance.IRemoteCallback");
            data.writeInt(ACTION_NOTIFY_VRSHELL_RESUMED);
            mNativeShellService.transact(CODE_SYSTEM_EXT_CLIENT, data, reply,
                    IBinder.FLAG_ONEWAY);
        } catch (RemoteException e) {
            e.printStackTrace();
        } finally {
            reply.recycle();
            data.recycle();
        }
    }

    public void notifyScreenStateChanged(boolean screenOn) {
        ApiLayerService.getInstance().updateScreenState(screenOn);
        if (mNativeShellService == null) {
            return;
        }
        Parcel data = Parcel.obtain();
        Parcel reply = Parcel.obtain();
        try {
            data.writeInterfaceToken("com.bytedance.IRemoteCallback");
            data.writeInt(ACTION_SCREEN_STATE_CHANGE);
            data.writeInt(screenOn ? 1 : 0);
            mNativeShellService.transact(CODE_SYSTEM_EXT_CLIENT, data, reply,
                    IBinder.FLAG_ONEWAY);
        } catch (RemoteException e) {
            e.printStackTrace();
        } finally {
            reply.recycle();
            data.recycle();
        }
    }

    public void notifyResizeVirtualDisplay(int displayId, int reqOrientation) {
        if (mNativeShellService == null) {
            return;
        }
        Slog.i(TAG, "notifyResizeVirtualDisplay displayId : " + displayId + ", reqOrientation : "
                + reqOrientation);
        Parcel data = Parcel.obtain();
        Parcel reply = Parcel.obtain();
        try {
            data.writeInterfaceToken("com.bytedance.IRemoteCallback");
            data.writeInt(ACTION_RESIZE_VIRTUAL_DISPLAY);
            data.writeInt(displayId);
            data.writeInt(reqOrientation);
            mNativeShellService.transact(CODE_SYSTEM_EXT_CLIENT, data, reply,
                    IBinder.FLAG_ONEWAY);
        } catch (RemoteException e) {
            e.printStackTrace();
        } finally {
            reply.recycle();
            data.recycle();
        }
    }

    public void notifyFocusDisplayChanged(int displayId) {
        if (mNativeShellService == null) {
            return;
        }
        Slog.i(TAG, "notifyFocusDisplayChanged displayId : " + displayId);
        Parcel data = Parcel.obtain();
        Parcel reply = Parcel.obtain();
        try {
            data.writeInterfaceToken("com.bytedance.IRemoteCallback");
            data.writeInt(ACTION_NOTIFY_FOCUS_CHANGED);
            data.writeInt(displayId);
            mNativeShellService.transact(CODE_SYSTEM_EXT_CLIENT, data, reply,
                    IBinder.FLAG_ONEWAY);
        } catch (RemoteException e) {
            e.printStackTrace();
        } finally {
            reply.recycle();
            data.recycle();
        }
    }

    /**
     * The display that embeds the calling display (an ActivityView inside a 2D app panel), so
     * SystemExt places the new panel next to the panel the user sees; -1 when there is none.
     */
    private int findParentDisplayId(int callingDisplayId, String callingPackage) {
        DisplayContent displayContent;
        WindowState parentWindow;
        DisplayContent parentDisplay;
        WindowState focusWindow;
        if (callingDisplayId == 0) {
            return -1;
        }
        if (callingDisplayId == -1) {
            if (TextUtils.isEmpty(callingPackage)
                    || (focusWindow = mService.mWindowManager.mRoot
                            .getTopFocusedDisplayContent().mCurrentFocus) == null
                    || !callingPackage.equals(focusWindow.getOwningPackage())) {
                return -1;
            }
            displayContent = focusWindow.getDisplayContent();
        } else {
            displayContent = mService.mWindowManager.mRoot.getDisplayContent(callingDisplayId);
        }
        if (displayContent == null || displayContent.getDisplay().getExt().isVr2dDisplay()
                || (parentWindow = displayContent.getParentWindow()) == null
                || (parentDisplay = parentWindow.getDisplayContent()) == null) {
            return -1;
        }
        return parentDisplay.getDisplayId();
    }

    private final class StartHandler extends Handler {
        private static final int RETRY_GET_NATIVE_SHELL_SERVICE_MSG = 1;

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
        private static final int CODE_WMS_APP_SWITCH_ALLOWED_RESULT = 1003;
        private static final int CODE_WMS_APP_SWITCH_ALLOWED_RESULT_BATCH = 1006;
        private static final int CODE_WMS_BEFORE_EXIT_3D_APP = 1007;
        private static final int CODE_WMS_FORCE_HIDE_SOFT_INPUT_METHOD = 1004;
        private static final int CODE_WMS_GET_DEFAULT_DISPLAY_TOP_RUNNING_TASK = 1005;
        private static final int CODE_WMS_MOVE_DISPLAY_TO_BACK = 1000;
        private static final int CODE_WMS_MOVE_DISPLAY_TO_FRONT = 1001;
        private static final int CODE_WMS_UPDATE_DISPLAY_FOCUS = 1008;
        private static final String DESCRIPTOR = "com.bytedance.IRemoteCallback";

        ClientBinder() {
            attachInterface(this, DESCRIPTOR);
        }

        @Override
        protected boolean onTransact(int code, Parcel data, Parcel reply, int flags)
                throws RemoteException {
            switch (code) {
                case CODE_WMS_MOVE_DISPLAY_TO_BACK: {
                    data.enforceInterface(DESCRIPTOR);
                    int displayId = data.readInt();
                    long callingId = Binder.clearCallingIdentity();
                    try {
                        synchronized (mService.mGlobalLock) {
                            try {
                                WindowManagerService.boostPriorityForLockedSection();
                                mService.getActivityStartController().getExt()
                                        .handleClientVirtualDisplayVisibilityChanged(displayId,
                                                false);
                            } finally {
                                WindowManagerService.resetPriorityAfterLockedSection();
                            }
                        }
                    } finally {
                        Binder.restoreCallingIdentity(callingId);
                    }
                    return true;
                }
                case CODE_WMS_MOVE_DISPLAY_TO_FRONT: {
                    data.enforceInterface(DESCRIPTOR);
                    int displayId = data.readInt();
                    long callingId = Binder.clearCallingIdentity();
                    try {
                        synchronized (mService.mGlobalLock) {
                            try {
                                WindowManagerService.boostPriorityForLockedSection();
                                mService.getActivityStartController().getExt()
                                        .handleClientVirtualDisplayVisibilityChanged(displayId,
                                                true);
                            } finally {
                                WindowManagerService.resetPriorityAfterLockedSection();
                            }
                        }
                    } finally {
                        Binder.restoreCallingIdentity(callingId);
                    }
                    return true;
                }
                case CODE_WMS_APP_SWITCH_ALLOWED_RESULT: {
                    data.enforceInterface(DESCRIPTOR);
                    long seq = data.readLong();
                    boolean allowed = data.readInt() != 0;
                    mService.getActivityStartController().getExt()
                            .handleClientActivityOrTaskSwitchAllowedResult(seq, allowed);
                    return true;
                }
                case CODE_WMS_FORCE_HIDE_SOFT_INPUT_METHOD:
                    try {
                        InputMethodManagerInternal inputMethodManagerInternal =
                                LocalServices.getService(InputMethodManagerInternal.class);
                        if (inputMethodManagerInternal != null) {
                            inputMethodManagerInternal.hideCurrentInputMethod();
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    return true;
                case CODE_WMS_GET_DEFAULT_DISPLAY_TOP_RUNNING_TASK: {
                    data.enforceInterface(DESCRIPTOR);
                    ActivityManager.RunningTaskInfo taskInfo = mService
                            .getActivityStartController().getExt().getDefaultDisplayTopTaskInfo();
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
                    int length = data.readInt();
                    if (length > 0) {
                        long[] seqArray = new long[length];
                        data.readLongArray(seqArray);
                        mService.getActivityStartController().getExt()
                                .handleClientActivityOrTaskBatchAllowedResult(seqArray);
                    }
                    return true;
                }
                case CODE_WMS_UPDATE_DISPLAY_FOCUS: {
                    data.enforceInterface(DESCRIPTOR);
                    Slog.w(TAG, "CODE_WMS_UPDATE_DISPLAY_FOCUS");
                    int displayId = data.readInt();
                    mService.mWindowManager.getExt().updateDisplayFocus(displayId,
                            "USER_UPDATE_DISPLAY_FOCUS");
                    return true;
                }
                default:
                    // 1002 (MSG_DO_TRANVERSAL) and 1007 are not handled here on the factory.
                    return super.onTransact(code, data, reply, flags);
            }
        }

        @Override
        public IBinder asBinder() {
            return this;
        }
    }
}
