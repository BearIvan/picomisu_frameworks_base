// Copyright 2026 Picomisu contributors
// SPDX-License-Identifier: Apache-2.0

package com.android.server.wm;

import android.content.pm.ApplicationInfo;
import android.hardware.display.DisplayManager;
import android.os.Handler;
import android.util.BoostFramework;
import android.util.TimeUtils;
import android.view.Display;

import com.android.internal.app.ProcessMap;
import com.android.internal.os.BackgroundThread;
import com.android.server.SysOptBridge;
import com.android.server.am.ActivityManagerService;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

/**
 * Smartisan extension state of the {@link ActivityTaskManagerService}. Reconstructed from the
 * PICO OS 5.13.7 factory services (com.android.server.wm.ActivityTaskManagerServiceSmtBase).
 *
 * The Single3DApp implementation lives in the factory sys-services JAR.
 *
 * @hide
 */
public abstract class ActivityTaskManagerServiceSmtBase {
    protected static final String TAG = "ActivityTaskManagerService";
    private static boolean mIsPerfBoost_V5_Acquired = false;
    public static boolean mIsPerfLockAcquired = false;
    private static BoostFramework mPerfBoost_V5;
    ActivityManagerService mAMS;
    protected ActivityTaskManagerService mAtmServices;
    private DisplayManager mDisplayManager;
    public Handler mHandler;
    public WindowProcessController mHomePreviousProcess;
    protected boolean mIsColdLaunch;
    private RootActivityContainerSmtBase mRacSmtBase;
    private boolean mReleaseInQueue;
    Handler mPerfHandler = new Handler();
    public boolean mBoostWhiteListEnabled = true;
    private int mLaunchTimeOut = 2000;
    protected int boost_v5_TimeOut = 2000;
    protected int mDefaultBoostTimeOut = 2000;
    private int mBoostUid = -1;
    public boolean uiFirstSwitch = true;
    /** Process of the previous VR activity, kept by the sys services JAR. */
    public WindowProcessController mPreviousVrProcess = null;
    public long mPreviousVrProcessVisibleTime = 0;
    final ProcessMap<WindowProcessController> mProcessFrozenNames = new ProcessMap<>();
    /** Prefetched (pre-started) application processes by name and uid. */
    final ProcessMap<WindowProcessController> mPrefetchProcessNames = new ProcessMap<>();
    final List<WindowProcessController> unFreezeAppsForVisibility = new ArrayList<>();
    Runnable mReleaseRunnable = new Runnable() {
        @Override
        public void run() {
            mReleaseInQueue = false;
            mPerfHandler.removeCallbacks(this);
            releasePerfLock();
        }
    };

    public ActivityTaskManagerServiceSmtBase(ActivityTaskManagerService atmServices) {
        mHandler = null;
        mAtmServices = atmServices;
        SysOptBridge.getFactory().getAtmOptEx().init(atmServices);
        mHandler = new Handler(BackgroundThread.getHandler().getLooper());
    }

    public void setAMS(ActivityManagerService ams) {
        mAMS = ams;
    }

    public boolean isColdLaunch() {
        return mIsColdLaunch;
    }

    public void setIsColdLaunch(boolean value) {
        mIsColdLaunch = value;
    }

    WindowProcessController getFrozenProcessController(String processName, int uid) {
        return mProcessFrozenNames.get(processName, uid);
    }

    boolean isPrefetchProcess(String processName, int uid) {
        return mPrefetchProcessNames.get(processName, uid) != null;
    }

    public void perfLockRelease(long delay) {
        if (delay <= 0) {
            releasePerfLock();
        } else {
            if (!mIsPerfBoost_V5_Acquired) {
                return;
            }
            mReleaseInQueue = true;
            mPerfHandler.postDelayed(mReleaseRunnable, delay);
        }
    }

    private void releasePerfLock() {
        BoostFramework boostFramework = mPerfBoost_V5;
        if (boostFramework != null) {
            synchronized (boostFramework) {
                if (mIsPerfBoost_V5_Acquired) {
                    mIsPerfBoost_V5_Acquired = false;
                    mPerfBoost_V5.perfLockRelease();
                }
            }
        }
    }

    public WindowProcessController getPreviousVrProcess() {
        synchronized (mAtmServices.mGlobalLock) {
            try {
                WindowManagerService.boostPriorityForLockedSection();
                return mPreviousVrProcess;
            } finally {
                WindowManagerService.resetPriorityAfterLockedSection();
            }
        }
    }

    public void dumpVrPreviousInfo(String dumpPackage, boolean needSep, PrintWriter pw,
            boolean dumpAll) {
        if (mPreviousVrProcess != null && (dumpPackage == null
                || mPreviousVrProcess.mPkgList.contains(dumpPackage))) {
            if (needSep) {
                pw.println();
            }
            pw.println("  mPreviousVrProcess: " + mPreviousVrProcess);
        }
        if (dumpAll) {
            if (mPreviousVrProcess == null || dumpPackage == null
                    || mPreviousVrProcess.mPkgList.contains(dumpPackage)) {
                StringBuilder sb = new StringBuilder(128);
                sb.append("  mPreviousVrProcessVisibleTime: ");
                TimeUtils.formatDuration(mPreviousVrProcessVisibleTime, sb);
                pw.println(sb);
            }
        }
    }

    public Display getDisplay(int displayId) {
        if (mDisplayManager == null) {
            mDisplayManager = (DisplayManager) mAtmServices.mContext.getSystemService("display");
        }
        if (mDisplayManager != null) {
            return mDisplayManager.getDisplay(displayId);
        }
        return null;
    }

    /**
     * ActivityStackSupervisor.updateTopResumedActivityIfNeeded: the Single3DApp policy of the
     * sys services JAR learns the new top resumed activity and the type of its display.
     */
    public void updateTopResumedActivityToSingle3DApp(ActivityRecord topResumedActivity) {
        if (topResumedActivity != null && topResumedActivity.getDisplay() != null
                && topResumedActivity.getDisplay().mDisplay != null) {
            topResumedActivity.getActivityRecordSmtEx().setLaunchSource(
                    topResumedActivity.launchedFromUid, topResumedActivity.launchedFromPackage);
            SysOptBridge.getFactory().getSingle3DApp().updateTopResumedActivity(
                    topResumedActivity, topResumedActivity.getDisplay().mDisplay.getType());
        }
    }

    /** Application of the resumed activity of the top full screen stack, if any. */
    public ApplicationInfo getTopApplication() {
        ActivityStack stack = ((RootActivityContainerSmtBase) mAtmServices.mRootActivityContainer
                .getSmtEx()).getTopDisplayFullScreenStack();
        if (stack != null) {
            ActivityRecord ar = stack.mResumedActivity;
            if (ar != null) {
                return ar.appInfo;
            }
        }
        return null;
    }

    /** First activity of the recent tasks that is on display 0. */
    public ActivityRecord getLast3DActivity() {
        ArrayList<TaskRecord> taskRecords = mAtmServices.getRecentTasks().getRawTasks();
        if (taskRecords != null) {
            for (int i = 0; i < taskRecords.size(); i++) {
                TaskRecord curTask = taskRecords.get(i);
                for (int j = 0; j < curTask.mActivities.size(); j++) {
                    ActivityRecord curActivity = curTask.getChildAt(j);
                    if (curActivity.getDisplayId() == 0) {
                        return curActivity;
                    }
                }
            }
            return null;
        }
        return null;
    }

    public void perfLockAcquire(String pkg_name) {
        if (mPerfBoost_V5 == null) {
            synchronized (mAtmServices) {
                if (mPerfBoost_V5 == null) {
                    mPerfBoost_V5 = new BoostFramework();
                }
            }
        }
        BoostFramework boostFramework = mPerfBoost_V5;
        if (boostFramework != null) {
            synchronized (boostFramework) {
                if (mReleaseInQueue) {
                    mPerfHandler.removeCallbacks(mReleaseRunnable);
                    mReleaseInQueue = false;
                    mIsPerfBoost_V5_Acquired = false;
                }
                if (!mIsPerfBoost_V5_Acquired) {
                    if (mIsColdLaunch) {
                        mLaunchTimeOut = boost_v5_TimeOut;
                    } else {
                        mLaunchTimeOut = mDefaultBoostTimeOut;
                    }
                    mPerfBoost_V5.perfHint(BoostFramework.VENDOR_HINT_FIRST_LAUNCH_BOOST,
                            pkg_name, mLaunchTimeOut, 8);
                    mIsPerfBoost_V5_Acquired = true;
                    perfLockRelease(mLaunchTimeOut);
                }
            }
        }
    }

    /** ActivityStack.startPausingLocked: remember the process paused for the home activity. */
    public void adjustHomePrevProcess(ActivityRecord resuming, ActivityRecord prev) {
        if (resuming != null && resuming.isActivityTypeHome()) {
            mHomePreviousProcess = prev.app;
        }
    }

    public void onCleanUpApplicationRecord(WindowProcessController proc) {
        if (proc == mHomePreviousProcess) {
            mHomePreviousProcess = null;
        }
    }

    /** Name of the bottom activity's package of the display whose top activity is pkg's. */
    public String isAnyDisplayStackTop(String pkg) {
        if (mRacSmtBase == null) {
            mRacSmtBase = (RootActivityContainerSmtBase) mAtmServices.mStackSupervisor
                    .mRootActivityContainer.getSmtEx();
        }
        synchronized (mAtmServices.mGlobalLock) {
            try {
                WindowManagerService.boostPriorityForLockedSection();
                if (mRacSmtBase == null) {
                    return "mRacSmtBase is null";
                }
                return mRacSmtBase.isAnyDisplayStackTopLocked(pkg);
            } finally {
                WindowManagerService.resetPriorityAfterLockedSection();
            }
        }
    }

    public TaskRecord getTaskRecordByTaskIdForDeepClean(int taskId) {
        return null;
    }

    /** LocalService.getTopApp: the top app process learns the type of its display. */
    public void updateTopDisplayType(ActivityRecord top) {
        if (top != null && top.getDisplay() != null && top.app != null) {
            top.app.getWPCSmtEx().mTopDisplayType = top.getDisplay().mDisplay.getType();
        }
    }
}
