/*
 * Copyright 2026 Picomisu contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package com.android.server.wm;

import android.app.ActivityManager;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Slog;
import android.view.Display;

import com.android.server.api.ApiLayerService;

import java.util.ArrayList;

/**
 * Per-display PICO VR state (factory PICO OS 5.13.7 com.android.server.wm.ExtActivityDisplayImpl):
 * visibility of a 2D app virtual display as SystemExt sees it, and the top task history used to
 * report "task moved to front", "task removed" and "display empty" to SystemExt.
 */
public class ExtActivityDisplayImpl {
    private static final String TAG = "ActivityStartControllerExt";
    public static final int PENDING_INVISIBLE = 1;
    public static final int INVISIBLE = 2;
    public static final int PENDING_VISIBLE = 3;
    public static final int VISIBLE = 4;

    private final ActivityDisplay mBase;
    private final ArrayList<ActivityManager.RunningTaskInfo> mAllTasks = new ArrayList<>();
    private ActivityTaskManagerService mService;
    private Handler mHandler;
    private int mDisplayState;
    private int mReqOrientation;
    private int mVisibility;

    private final class H extends Handler {
        static final int CHECK_TASK_EMPTY_MSG = 1;

        H(Looper looper) {
            super(looper, null, true);
        }

        @Override
        public void handleMessage(Message msg) {
            if (msg.what != CHECK_TASK_EMPTY_MSG) {
                return;
            }
            synchronized (mService.mGlobalLock) {
                try {
                    WindowManagerService.boostPriorityForLockedSection();
                    if (mAllTasks.isEmpty()) {
                        onTaskEmpty();
                    } else if (mBase.topRunningActivity() == null) {
                        mAllTasks.clear();
                        onTaskEmpty();
                    }
                } finally {
                    WindowManagerService.resetPriorityAfterLockedSection();
                }
            }
        }
    }

    public ExtActivityDisplayImpl(ActivityDisplay base) {
        mBase = base;
    }

    public void init(ActivityTaskManagerService atms, Display display) {
        mService = atms;
        mDisplayState = display.getState();
        mVisibility = INVISIBLE;
        mHandler = new H(mService.mH.getLooper());
    }

    public void setVisibility(int visibility) {
        mVisibility = visibility;
    }

    public int getVisibility() {
        return mVisibility;
    }

    public void onDisplayChanged() {
        // Factory: a screen-on state change re-sends the requested orientation of a 2D app
        // display (handleResizeVirtualDisplay); virtual display resizing is not ported yet.
        mDisplayState = mBase.mDisplay.getState();
    }

    public boolean isScreenOn() {
        return mDisplayState == Display.STATE_ON;
    }

    public boolean isVr2dDisplay() {
        return mBase.mDisplay.getExt().isVr2dDisplay();
    }

    public void setReqOrientation(int reqOrientation) {
        mReqOrientation = reqOrientation;
    }

    public int getReqOrientation() {
        return mReqOrientation;
    }

    private void onTaskMovedToFront(ActivityManager.RunningTaskInfo taskInfo) {
        final ExtActivityStartControllerImpl controller =
                mService.getActivityStartController().getExt();
        if (mBase.mDisplayId == 0) {
            Slog.i(TAG, "notifyDefaultDisplayTaskMoveToFront : " + taskInfo);
            controller.onDefaultDisplayTopTaskChanged(taskInfo);
            ApiLayerService.getInstance().updateTopAppOnDefaultDisplay(taskInfo);
        } else {
            Slog.i(TAG, "notifyVirtualDisplayTaskMoveToFront : " + taskInfo + ", display ID : "
                    + mBase.mDisplayId);
        }
        controller.getSystemExt().notifyTaskMovedToFront(mBase.mDisplayId, taskInfo);
    }

    private void onTaskRemoved(ActivityManager.RunningTaskInfo taskInfo) {
        if (mBase.mDisplayId == 0) {
            Slog.i(TAG, "do not notifyDefaultDisplayTaskRemoved : " + taskInfo);
            return;
        }
        Slog.i(TAG, "notifyVirtualDisplayTaskRemoved : " + taskInfo + ", display ID : "
                + mBase.mDisplayId);
        mService.getActivityStartController().getExt().getSystemExt()
                .notifyTaskRemoved(mBase.mDisplayId, taskInfo);
    }

    private void onTaskEmpty() {
        if (mBase.mDisplayId != 0) {
            Slog.i(TAG, "notifyVirtualDisplayEmpty display ID : " + mBase.mDisplayId);
            mService.getActivityStartController().getExt().getSystemExt()
                    .notifyTaskEmpty(mBase.mDisplayId);
        }
    }

    public boolean checkForTopTaskChanged() {
        final TaskRecord focusedTask = getDisplayTopTask();
        final int topTaskId = focusedTask != null ? focusedTask.taskId : -1;
        if (topTaskId == getTopTaskId()) {
            return false;
        }
        final ActivityManager.RunningTaskInfo topTaskInfo =
                focusedTask != null ? focusedTask.getTaskInfo() : null;
        if (topTaskInfo == null) {
            mHandler.removeMessages(H.CHECK_TASK_EMPTY_MSG);
            mHandler.sendEmptyMessageDelayed(H.CHECK_TASK_EMPTY_MSG, 100);
            return false;
        }
        removeTaskInfo(topTaskInfo.taskId);
        mAllTasks.add(topTaskInfo);
        onTaskMovedToFront(topTaskInfo);
        mHandler.removeMessages(H.CHECK_TASK_EMPTY_MSG);
        return false;
    }

    public void onTaskRemoved(int taskId) {
        final ActivityManager.RunningTaskInfo taskInfo = removeTaskInfo(taskId);
        if (taskInfo != null) {
            onTaskRemoved(taskInfo);
            mHandler.removeMessages(H.CHECK_TASK_EMPTY_MSG);
            mHandler.sendEmptyMessageDelayed(H.CHECK_TASK_EMPTY_MSG, 100);
        }
    }

    private TaskRecord getDisplayTopTask() {
        final ActivityRecord top = mBase.topRunningActivity();
        return top != null ? top.getTaskRecord() : null;
    }

    private ActivityManager.RunningTaskInfo removeTaskInfo(int taskId) {
        for (int i = mAllTasks.size() - 1; i >= 0; i--) {
            if (mAllTasks.get(i).taskId == taskId) {
                return mAllTasks.remove(i);
            }
        }
        return null;
    }

    public ActivityManager.RunningTaskInfo getTopTaskInfo() {
        return mAllTasks.isEmpty() ? null : mAllTasks.get(mAllTasks.size() - 1);
    }

    public int getTopTaskId() {
        final ActivityManager.RunningTaskInfo top = getTopTaskInfo();
        return top != null ? top.taskId : -1;
    }
}
