/*
 * Copyright 2026 Picomisu contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package com.android.server.wm;

import android.app.ActivityManager;
import android.app.ActivityOptions;
import android.app.IApplicationThread;
import android.app.WindowConfiguration;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.res.Configuration;
import android.os.Binder;
import android.os.Debug;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.pico.utils.Features;
import android.pico.utils.PicoUtils;
import android.text.TextUtils;
import android.util.MergedConfiguration;
import android.util.Slog;
import android.view.Display;
import android.view.DisplayInfo;

import com.android.server.am.ActivityManagerService;

import java.util.ArrayList;

/**
 * PICO VR routing of activity starts (factory PICO OS 5.13.7
 * com.android.server.wm.ExtActivityStartControllerImpl). Every start of a 2D activity is first
 * checked by the SystemExt app ({@link SystemExt#handleStartActivity}), which may allow, cancel
 * or defer it (deferred starts are replayed on its 1003/1006 answer). 2D activities run on
 * virtual displays the app creates; task and display changes are reported to it.
 *
 * 2D apps that use the new configuration solution (PicoUtils.usingNewConfigurationSolution) get a
 * process configuration sized from their 2D app metadata (getGlobalConfiguration,
 * onWindowProcessControllerInit, onDisplayConfigurationChanged, onReportResized), and an
 * orientation request on a 2D app display asks SystemExt to resize the virtual display
 * (handleResizeVirtualDisplay).
 *
 * Not ported yet: startActivityFromRecents and the Smartisan single-3D-app pending launch
 * notification.
 */
public class ExtActivityStartControllerImpl {
    private static final String TAG = "ActivityStartControllerExt";
    private static final String ACTIVITY_FALLBACK_HOME = "com.android.settings/.FallbackHome";
    private static final String ACTIVITY_VR_SHELL = "com.pvr.vrshell/.MainActivity";
    private static final String PKG_SYSTEMEXT = "com.picovr.systemext";
    private static final String PKG_SYSTEMUI = "com.android.systemui";
    public static final String APP_PROP_OPEN_VST = "open_vst";
    /** Display flag of the PICO 2D app virtual displays (factory constant 32768). */
    private static final int FLAG_VR_2D_VIRTUAL_DISPLAY = 32768;
    public static final ArrayList<String> FORCE_ONE_DISPLAY_APPS = new ArrayList<>();

    static {
        FORCE_ONE_DISPLAY_APPS.add("com.pico.browser");
        FORCE_ONE_DISPLAY_APPS.add("com.pico.browser.overseas");
        FORCE_ONE_DISPLAY_APPS.add("com.picovr.store");
        FORCE_ONE_DISPLAY_APPS.add("com.picoxr.tobstore");
    }

    private final ActivityStartController mBase;
    private final ActivityTaskManagerService mService;
    private final Handler mHandler;
    private final SystemExt mSystemExt;
    private ActivityManager.RunningTaskInfo mDefaultDisplayTopTaskInfo = null;
    private final Configuration mTmpConfiguration = new Configuration();
    private final ArrayList<PendingActivityLaunch> mPendingOnCheckingActivityLaunches =
            new ArrayList<>();

    private final class StartHandler extends Handler {
        static final int WORKAROUND_START_ACTIVITY_MSG = 2;

        StartHandler(Looper looper) {
            super(looper, null, true);
        }

        @Override
        public void handleMessage(Message msg) {
            if (msg.what == WORKAROUND_START_ACTIVITY_MSG) {
                mService.mContext.startActivityAsUser((Intent) msg.obj, UserHandle.CURRENT);
            }
        }
    }

    public ExtActivityStartControllerImpl(ActivityStartController base,
            ActivityTaskManagerService service) {
        mBase = base;
        mService = service;
        mHandler = new StartHandler(mService.mH.getLooper());
        mSystemExt = new SystemExt(service);
    }

    public SystemExt getSystemExt() {
        return mSystemExt;
    }

    public void onSystemReady() {
        if (!Features.isPvr2DEnabled()) {
            return;
        }
        mSystemExt.onSystemReady();
    }

    public void onSystemExtDied() {
        mPendingOnCheckingActivityLaunches.clear();
    }

    public void handleClientActivityOrTaskSwitchAllowedResult(long seq, boolean allowed) {
        PendingActivityLaunch pending = null;
        synchronized (mService.mGlobalLock) {
            try {
                WindowManagerService.boostPriorityForLockedSection();
                for (PendingActivityLaunch item : mPendingOnCheckingActivityLaunches) {
                    if (item.seq == seq) {
                        pending = item;
                        mPendingOnCheckingActivityLaunches.remove(item);
                        break;
                    }
                }
            } finally {
                WindowManagerService.resetPriorityAfterLockedSection();
            }
        }
        if (pending != null) {
            Slog.i(TAG, "checkAppSwitchAllowed : " + pending.r + " , allowed : " + allowed);
        }
        if (!allowed || pending == null) {
            return;
        }
        final long callingId = Binder.clearCallingIdentity();
        try {
            synchronized (mService.mGlobalLock) {
                try {
                    WindowManagerService.boostPriorityForLockedSection();
                    doPendingActivityLaunches(pending, true);
                } finally {
                    WindowManagerService.resetPriorityAfterLockedSection();
                }
            }
        } finally {
            Binder.restoreCallingIdentity(callingId);
        }
    }

    public void handleClientActivityOrTaskBatchAllowedResult(long[] seqs) {
        for (long seq : seqs) {
            handleClientActivityOrTaskSwitchAllowedResult(seq, true);
        }
    }

    public void onNewDisplayAdded(ActivityDisplay activityDisplay) {
        if (activityDisplay.mDisplayId > 0) {
            mService.updateDisplayOverrideConfiguration(null, activityDisplay.mDisplayId);
        }
    }

    private static boolean hasAppProperties(ActivityInfo activityInfo, String prop) {
        final String properties = activityInfo.getExt().getAppProperties();
        if (prop == null || TextUtils.isEmpty(properties)) {
            return false;
        }
        for (String item : properties.split("\\|")) {
            if (!TextUtils.isEmpty(item) && item.trim().toLowerCase().equals(prop)) {
                return true;
            }
        }
        return false;
    }

    /** Puts a 2D activity on the virtual display of its app, or asks SystemExt for a new one. */
    public void calculateDisplayId(ActivityRecord startActivity, TaskRecord candidateTask,
            ActivityOptions options, LaunchParamsController.LaunchParams launchParams) {
        if (!Features.isPvr2DEnabled() || startActivity == null
                || startActivity.info.getExt().isVrActivity()) {
            return;
        }
        if (SystemProperties.getInt("pvr.start_2d_app_in_default_display.enable", 0) == 1) {
            return;
        }
        if (candidateTask != null && candidateTask.getStack() != null) {
            launchParams.mPreferredDisplayId = candidateTask.getStack().mDisplayId;
            return;
        }
        int displayId = options != null ? options.getLaunchDisplayId() : 0;
        if (displayId > 0) {
            final Display display = mService.mWindowManager.mDisplayManager.getDisplay(displayId);
            if (display != null && "PvrShellDisplay".equals(display.getName())) {
                options.setLaunchDisplayId(-1);
                displayId = -1;
            }
            Slog.d(TAG, "calculateDisplayId: " + displayId + ", launch display: " + display);
        }
        if (displayId <= 0 && !hasAppProperties(startActivity.info, APP_PROP_OPEN_VST)) {
            final ArrayList<ActivityDisplay> displays =
                    mService.mRootActivityContainer.mActivityDisplays;
            for (int displayNdx = displays.size() - 1; displayNdx >= 0; displayNdx--) {
                final ActivityDisplay display = displays.get(displayNdx);
                final ActivityStack stack = display.getTopStack();
                final TaskRecord task = stack != null ? stack.topTask() : null;
                if (task == null || task.mCallingPackage == null
                        || !task.mCallingPackage.equals(startActivity.packageName)) {
                    continue;
                }
                final boolean sameAffinityAndPosition = task.rootAffinity != null
                        && task.rootAffinity.equals(startActivity.taskAffinity)
                        && task.getRootActivity() != null
                        && TextUtils.equals(startActivity.info.getExt().get2dAppPosition(),
                                task.getRootActivity().info.getExt().get2dAppPosition());
                if (sameAffinityAndPosition
                        || FORCE_ONE_DISPLAY_APPS.contains(startActivity.packageName)) {
                    displayId = display.mDisplayId;
                    Slog.d(TAG, "reuse displayId: " + displayId + ", for activity: "
                            + startActivity);
                    startActivity.appInfo.getExt().setDisplayId(displayId);
                    break;
                }
            }
        }
        if (displayId <= 0) {
            displayId = mSystemExt.requestCreateVirtualDisplay(startActivity,
                    startActivity.getExt().getSourceRecord());
            Slog.d(TAG, "calculateDisplayId: " + displayId + ", for activity: " + startActivity);
            startActivity.appInfo.getExt().setDisplayId(displayId);
        }
        if (displayId > 0) {
            launchParams.mPreferredDisplayId = displayId;
        }
    }

    public void reuseTask(ActivityRecord startActivity, RootActivityContainer.FindTaskResult tmpResult,
            RootActivityContainer.FindTaskResult result, boolean isPreferDisplay) {
        if (!Features.isPvr2DEnabled()) {
            return;
        }
        if (!allowReuse(startActivity, tmpResult.mRecord)) {
            tmpResult.clear();
        } else if (!tmpResult.mIdealMatch && !isPreferDisplay) {
            result.setTo(tmpResult);
        }
    }

    private boolean allowReuse(ActivityRecord startActivity, ActivityRecord findRecord) {
        if (startActivity == null || findRecord == null) {
            return false;
        }
        if (startActivity.info.getExt().isVrActivity()) {
            return findRecord.info.getExt().isVrActivity();
        }
        final ActivityRecord rootActivity = findRecord.getTaskRecord().getRootActivity();
        if (rootActivity != null && TextUtils.equals(startActivity.info.getExt().get2dAppPosition(),
                rootActivity.info.getExt().get2dAppPosition())) {
            Slog.d(TAG, "reuse prev task: " + findRecord.getTaskRecord() + ", findRecord: "
                    + findRecord + ", rootActivity: " + rootActivity);
            return true;
        }
        return false;
    }

    public void onFinishActivity(ActivityRecord r) {
        if (Features.isPvr2DEnabled()) {
            checkForDisplayTopTaskChanged();
        }
    }

    public void onTaskRemoved(TaskRecord tr) {
        if (!Features.isPvr2DEnabled()) {
            return;
        }
        checkForDisplayTopTaskChanged();
        checkForDisplayTaskRemoved(tr.taskId);
    }

    public void onTaskMovedToBack(ActivityDisplay display) {
        if (!Features.isPvr2DEnabled()) {
            return;
        }
        checkForDisplayTopTaskChanged();
        if (display == null || (display.mDisplay.getFlags() & FLAG_VR_2D_VIRTUAL_DISPLAY) == 0) {
            return;
        }
        final ExtActivityDisplayImpl displayExt = display.getExt();
        if (displayExt.getVisibility() == ExtActivityDisplayImpl.INVISIBLE
                || displayExt.getVisibility() == ExtActivityDisplayImpl.PENDING_INVISIBLE) {
            return;
        }
        displayExt.setVisibility(ExtActivityDisplayImpl.PENDING_INVISIBLE);
        Slog.i(TAG, "onTaskMovedToBack and notify hide VirtualDisplay, displayId : "
                + display.mDisplayId);
        mSystemExt.notifyVirtualDisplayVisibilityChanged(display.mDisplayId, false);
    }

    public void onDisplayRemoved(ActivityDisplay activityDisplay) {
    }

    public void onTaskMovedToFront(ActivityDisplay activityDisplay, String reason) {
        if (!Features.isPvr2DEnabled() || activityDisplay == null) {
            return;
        }
        checkForDisplayTopTaskChanged();
        if ((activityDisplay.mDisplay.getFlags() & FLAG_VR_2D_VIRTUAL_DISPLAY) != 0
                && !"setFocusedTask".equals(reason)) {
            final ExtActivityDisplayImpl displayExt = activityDisplay.getExt();
            if (displayExt.getVisibility() == ExtActivityDisplayImpl.INVISIBLE
                    || displayExt.getVisibility() == ExtActivityDisplayImpl.PENDING_INVISIBLE) {
                displayExt.setVisibility(ExtActivityDisplayImpl.PENDING_VISIBLE);
                if (ActivityTaskManagerDebugConfig.DEBUG_TASKS) {
                    Slog.i(TAG, "onTaskMovedToFront and notify show VirtualDisplay : "
                            + activityDisplay.topRunningActivity() + ", displayId : "
                            + activityDisplay.mDisplayId + "," + Debug.getCallers(12));
                }
                mSystemExt.notifyVirtualDisplayVisibilityChanged(activityDisplay.mDisplayId, true);
            }
        }
    }

    public void onActivityResumed(ActivityRecord activityRecord) {
        if (!Features.isPvr2DEnabled()
                || !ACTIVITY_VR_SHELL.equals(activityRecord.mActivityComponent.flattenToShortString())) {
            return;
        }
        final ActivityDisplay display = mService.mRootActivityContainer.getActivityDisplay(0);
        final ActivityStack stack = display != null
                ? display.getTopStackInWindowingMode(WindowConfiguration.WINDOWING_MODE_FULLSCREEN)
                : null;
        final ActivityRecord record = stack != null ? stack.getTopActivity() : null;
        if (record != null && !activityRecord.packageName.equals(record.packageName)) {
            Slog.w(TAG, "abandon onActivityResumed, top full screen app is " + record.packageName);
            return;
        }
        mSystemExt.notifyVrShellResumed();
    }

    private void checkForDisplayTopTaskChanged() {
        final ArrayList<ActivityDisplay> displays = mService.mRootActivityContainer.mActivityDisplays;
        for (int i = displays.size() - 1; i >= 0; i--) {
            final ActivityDisplay display = displays.get(i);
            if (is2dAppDisplay(display) || display.mDisplayId == 0) {
                display.getExt().checkForTopTaskChanged();
            }
        }
    }

    private void checkForDisplayTaskRemoved(int taskId) {
        final ArrayList<ActivityDisplay> displays = mService.mRootActivityContainer.mActivityDisplays;
        for (int i = displays.size() - 1; i >= 0; i--) {
            final ActivityDisplay display = displays.get(i);
            if (is2dAppDisplay(display) || display.mDisplayId == 0) {
                display.getExt().onTaskRemoved(taskId);
            }
        }
    }

    public void onDefaultDisplayTopTaskChanged(ActivityManager.RunningTaskInfo runningTaskInfo) {
        mDefaultDisplayTopTaskInfo = runningTaskInfo;
    }

    public ActivityManager.RunningTaskInfo getDefaultDisplayTopTaskInfo() {
        return mDefaultDisplayTopTaskInfo;
    }

    private TaskRecord getDisplayTopTask(int displayId) {
        final ActivityDisplay display = mService.mRootActivityContainer.getActivityDisplay(displayId);
        final ActivityRecord top = display != null ? display.topRunningActivity() : null;
        return top != null ? top.getTaskRecord() : null;
    }

    private static ComponentName getComponentName(ActivityManager.RunningTaskInfo taskInfo) {
        if (taskInfo == null) {
            return null;
        }
        return taskInfo.baseActivity != null ? taskInfo.baseActivity : taskInfo.topActivity;
    }

    private void doPendingActivityLaunches(PendingActivityLaunch launch, boolean checkVrShell) {
        final ComponentName topActivity = getComponentName(mDefaultDisplayTopTaskInfo);
        if (checkVrShell && !launch.r.info.getExt().isVrActivity() && (topActivity == null
                || !ACTIVITY_VR_SHELL.equals(topActivity.flattenToShortString()))) {
            Slog.w(TAG, "do pending launch 2D App failed : " + launch.r + ", vr app :"
                    + topActivity);
            return;
        }
        final ApplicationInfo fwInfo = mService.getPackageManagerInternalLocked()
                .getApplicationInfo(launch.r.packageName, 0, Binder.getCallingUid(),
                        mService.getCurrentUserId());
        Slog.d(TAG, "launch.r.appInfo=" + launch.r.appInfo + ",fwInfo=" + fwInfo);
        if (fwInfo == null) {
            Slog.w(TAG, "do pending launch 2D App failed: ApplicationInfo is null");
            return;
        }
        if (launch.r.appInfo != null && fwInfo.longVersionCode != launch.r.appInfo.longVersionCode) {
            Slog.w(TAG, "do pending launch 2D App failed: ApplicationInfo versionCode is changed,"
                    + " old = " + launch.r.appInfo.longVersionCode + ", new = "
                    + fwInfo.longVersionCode);
            return;
        }
        if (launch.isTask) {
            final TaskRecord task = launch.r.getTaskRecord();
            final ActivityStack currentStack = task != null ? task.getStack() : null;
            if (task == null || task.getRootActivity() == null || currentStack == null) {
                return;
            }
            Slog.i(TAG, "do pending launch Task : " + task);
            currentStack.moveTaskToFrontLocked(task, false, null, launch.r.appTimeTracker, TAG);
            return;
        }
        Slog.i(TAG, "do pending launch App : " + launch.r);
        if (launch.r.pendingOptions != null) {
            final int displayId = launch.r.pendingOptions.getLaunchDisplayId();
            if (displayId != -1) {
                final ActivityDisplay display =
                        mService.mRootActivityContainer.getActivityDisplayOrCreate(displayId);
                if (display == null || display.isRemoved()) {
                    Slog.w(TAG, "Launch on display check: display not found when start: "
                            + launch.r);
                    return;
                }
            }
        }
        try {
            mBase.obtainStarter(null, "pendingActivityLaunchExt").startResolvedActivity(launch.r,
                    launch.sourceRecord, null, null, launch.startFlags, true,
                    launch.r.pendingOptions, null);
        } catch (Exception e) {
            Slog.w(TAG, "do pending launch failed: " + launch.r, e);
        }
    }

    /** SystemExt moved a virtual display to the front (show) or to the back (hide). */
    public void handleClientVirtualDisplayVisibilityChanged(int displayId, boolean show) {
        final ActivityDisplay activityDisplay =
                mService.mRootActivityContainer.getActivityDisplay(displayId);
        if (activityDisplay == null) {
            return;
        }
        final DisplayContent displayContent = activityDisplay.mDisplayContent;
        final WindowContainer parent = displayContent.getParent();
        if (show) {
            if (ActivityTaskManagerDebugConfig.DEBUG_TASKS) {
                Slog.i(TAG, "move virtual display to top: " + displayId + ", show :" + show);
            }
            activityDisplay.getExt().setVisibility(ExtActivityDisplayImpl.VISIBLE);
            if (parent != null) {
                parent.positionChildAt(Integer.MAX_VALUE, displayContent, true);
                mService.mRootActivityContainer.resumeFocusedStacksTopActivities();
            }
            return;
        }
        if (ActivityTaskManagerDebugConfig.DEBUG_TASKS) {
            Slog.i(TAG, "move virtual display to bottom: " + displayId + ", show :" + show);
        }
        activityDisplay.getExt().setVisibility(ExtActivityDisplayImpl.INVISIBLE);
        if (parent == null) {
            return;
        }
        parent.positionChildAt(Integer.MIN_VALUE, displayContent, true);
        final TaskRecord task = getDisplayTopTask(displayId);
        final ActivityRecord r = task != null ? task.getTopActivity() : null;
        if (r != null && !r.finishing && ((r.intent.getFlags() & Intent.FLAG_ACTIVITY_NO_HISTORY)
                != 0 || (r.info.flags & ActivityInfo.FLAG_NO_HISTORY) != 0)) {
            Slog.i(TAG, "no-history finish of " + r);
            final ActivityStack stack = task.getStack();
            if (stack.requestFinishActivityLocked(r.appToken, 0, null, "stop-no-history", false)) {
                r.resumeKeyDispatchingLocked();
            }
        }
        mService.mRootActivityContainer.resumeFocusedStacksTopActivities();
    }

    /** A 2D activity started from a VR activity (or vice versa) always gets its own task. */
    public boolean forceNewTask(ActivityRecord startActivity, ActivityRecord sourceRecord) {
        return Features.isPvr2DEnabled() && sourceRecord != null
                && startActivity.info.getExt().isVrActivity()
                        != sourceRecord.info.getExt().isVrActivity();
    }

    /** Portrait (orientation 1) or landscape size of the 2D app, with its density, into config. */
    private static void adjustTo2dAppConfiguration(Configuration config, ApplicationInfo info,
            int orientation) {
        final int width = orientation == 1 ? info.getExt().get2dAppPortraitWidth()
                : info.getExt().get2dAppLandscapeWidth();
        final int height = orientation == 1 ? info.getExt().get2dAppPortraitHeight()
                : info.getExt().get2dAppLandscapeHeight();
        PicoUtils.adjustConfiguration(config, info.getExt().get2dAppDensity(), width, height);
    }

    /**
     * Global configuration reported to a 2D app activity (ActivityRecord
     * ensureActivityConfiguration / relaunchActivityLocked): its process configuration, or the
     * global configuration sized for the 2D app. Null means the normal global configuration.
     */
    public Configuration getGlobalConfiguration(ActivityRecord activityRecord) {
        if (!Features.isAdjustConfigurationEnabled()) {
            return null;
        }
        final ApplicationInfo info = activityRecord.info.applicationInfo;
        if (!PicoUtils.usingNewConfigurationSolution(info)
                || activityRecord.info.getExt().isVrActivity()) {
            return null;
        }
        if (activityRecord.app != null) {
            return activityRecord.app.getConfiguration();
        }
        mTmpConfiguration.setTo(mService.getGlobalConfiguration());
        adjustTo2dAppConfiguration(mTmpConfiguration, info, info.getExt().get2dAppOrientation());
        Slog.i(TAG, "getGlobalConfiguration: " + activityRecord + "," + mTmpConfiguration);
        return mTmpConfiguration;
    }

    /**
     * WindowProcessController creation: a 2D app process starts with the global configuration
     * sized for the 2D app. Returns false when the normal global configuration applies.
     */
    public boolean onWindowProcessControllerInit(WindowProcessController app) {
        if (!Features.isAdjustConfigurationEnabled()) {
            return false;
        }
        final ApplicationInfo info = app.mInfo;
        if (!PicoUtils.usingNewConfigurationSolution(info) || info.getExt().isVrApp()) {
            return false;
        }
        mTmpConfiguration.setTo(mService.getGlobalConfiguration());
        adjustTo2dAppConfiguration(mTmpConfiguration, info, info.getExt().get2dAppOrientation());
        app.onConfigurationChanged(mTmpConfiguration);
        Slog.i(TAG, "onWindowProcessControllerInit: " + info + "," + mTmpConfiguration);
        return true;
    }

    /**
     * DisplayContent.onConfigurationChanged of a 2D app display: the processes of its activities
     * get their configuration re-sized for the display's current orientation.
     */
    public void onDisplayConfigurationChanged(DisplayContent displayContent) {
        if (!Features.isAdjustConfigurationEnabled() || displayContent.mAcitvityDisplay == null
                || !displayContent.getDisplay().getExt().isVr2dDisplay()) {
            return;
        }
        final ArrayList<WindowProcessController> apps = new ArrayList<>();
        final ActivityDisplay activityDisplay = displayContent.mAcitvityDisplay;
        for (int stackNdx = activityDisplay.getChildCount() - 1; stackNdx >= 0; stackNdx--) {
            final ActivityStack stack = activityDisplay.getChildAt(stackNdx);
            for (int taskNdx = stack.getChildCount() - 1; taskNdx >= 0; taskNdx--) {
                final ArrayList<ActivityRecord> activities = stack.getChildAt(taskNdx).mActivities;
                for (int activityNdx = activities.size() - 1; activityNdx >= 0; activityNdx--) {
                    final ActivityRecord r = activities.get(activityNdx);
                    if (r.app != null && PicoUtils.usingNewConfigurationSolution(r.appInfo)
                            && !apps.contains(r.app)) {
                        apps.add(r.app);
                    }
                }
            }
        }
        if (apps.isEmpty()) {
            return;
        }
        final DisplayInfo displayInfo = displayContent.getDisplayInfo();
        final int orientation = displayInfo.logicalWidth > displayInfo.logicalHeight ? 0 : 1;
        for (WindowProcessController app : apps) {
            final ApplicationInfo info = app.mInfo;
            mTmpConfiguration.setTo(app.getConfiguration());
            adjustTo2dAppConfiguration(mTmpConfiguration, info, orientation);
            app.onConfigurationChanged(mTmpConfiguration);
            Slog.i(TAG, "onDisplayConfigurationChanged: " + info + "," + mTmpConfiguration);
        }
    }

    /**
     * WindowState.reportResized: a 2D app window gets its own merged configuration (process
     * configuration plus overrides) instead of the root configuration.
     */
    public void onReportResized(WindowState w, MergedConfiguration mergedConfiguration) {
        if (!Features.isAdjustConfigurationEnabled()) {
            return;
        }
        final WindowState parentWindow = w.getParentWindow();
        final Session session = parentWindow != null ? parentWindow.mSession : w.mSession;
        if (session.mPid == ActivityManagerService.MY_PID || session.mPid < 0) {
            return;
        }
        final WindowProcessController app =
                mService.getProcessController(session.mPid, session.mUid);
        if (app == null || PicoUtils.isSystemApp(app.mInfo)
                || !PicoUtils.usingNewConfigurationSolution(app.mInfo)) {
            return;
        }
        w.getMergedConfiguration(mergedConfiguration);
    }

    /**
     * DisplayContent.updateOrientationFromAppTokens on a 2D app display: instead of rotating, a
     * changed requested orientation (or a forced update) asks SystemExt to resize the virtual
     * display while the display is on and has an activity. Returns true when the display is a 2D
     * app display (the rotation update is skipped).
     */
    public boolean handleResizeVirtualDisplay(DisplayContent displayContent, int reqOrientation,
            boolean forceUpdate) {
        if (!Features.isResizeVirtualDisplayEnabled() || displayContent.mAcitvityDisplay == null
                || !displayContent.getDisplay().getExt().isVr2dDisplay()) {
            return false;
        }
        final ExtActivityDisplayImpl displayExt = displayContent.mAcitvityDisplay.getExt();
        if (reqOrientation != displayExt.getReqOrientation() || forceUpdate) {
            displayExt.setReqOrientation(reqOrientation);
            if (displayExt.isScreenOn()
                    && displayContent.mAcitvityDisplay.topRunningActivity() != null) {
                mSystemExt.notifyResizeVirtualDisplay(displayContent.getDisplayId(),
                        reqOrientation);
            }
        }
        return true;
    }

    private static boolean sameStartActivity(ActivityRecord one, ActivityRecord another) {
        if (one.intent == null && another.intent == null) {
            return one.mActivityComponent.equals(another.mActivityComponent);
        }
        return one.intent != null && another.intent != null
                && one.mActivityComponent.equals(another.mActivityComponent)
                && one.intent.filterEquals(another.intent);
    }

    /** Virtual displays of 2D apps keep their stack order when a stack there becomes empty. */
    public static boolean disableResumeNextFocusableActivityWhenStackIsEmpty(
            ActivityDisplay activityDisplay) {
        return Features.isPvr2DEnabled() && activityDisplay != null
                && activityDisplay.mDisplay.getType() == Display.TYPE_VIRTUAL;
    }

    private static class PendingActivityLaunch {
        private static long sSeqCount = 0;
        final boolean isTask;
        final ActivityRecord r;
        final long requestTime = SystemClock.uptimeMillis();
        final long seq;
        final ActivityRecord sourceRecord;
        final int startFlags;

        PendingActivityLaunch(ActivityRecord r, ActivityRecord sourceRecord, int startFlags,
                boolean isTask) {
            this.seq = sSeqCount++;
            this.r = r;
            this.sourceRecord = sourceRecord;
            this.startFlags = startFlags;
            this.isTask = isTask;
        }

        @Override
        public boolean equals(Object o) {
            if (!(o instanceof PendingActivityLaunch)) {
                return false;
            }
            final PendingActivityLaunch other = (PendingActivityLaunch) o;
            if (isTask != other.isTask) {
                return false;
            }
            return isTask ? r.getTaskRecord() == other.r.getTaskRecord()
                    : sameStartActivity(r, other.r);
        }

        @Override
        public int hashCode() {
            return r.mActivityComponent.hashCode();
        }
    }

    private static boolean is2dAppDisplay(ActivityDisplay activityDisplay) {
        return activityDisplay != null && activityDisplay.mDisplay != null
                && activityDisplay.mDisplay.getExt().isVr2dDisplay();
    }

    /** Intents for SystemUI go to the SystemExt app, which replaces SystemUI in VR. */
    public void obtainStarter(Intent intent, String reason) {
        if (intent == null) {
            return;
        }
        final ComponentName comp = intent.getComponent();
        if (comp != null && PKG_SYSTEMUI.equals(comp.getPackageName())) {
            Slog.d(TAG, "replace systemui intent component: " + intent);
            intent.setComponent(new ComponentName(PKG_SYSTEMEXT, comp.getClassName()));
        }
        if (intent.getPackage() != null && PKG_SYSTEMUI.equals(intent.getPackage())) {
            Slog.d(TAG, "replace systemui intent package: " + intent);
            intent.setPackage(PKG_SYSTEMEXT);
        }
    }

    /**
     * Asks SystemExt whether {@code startActivity} may start now. Returns true when the start is
     * intercepted (canceled, or pending until SystemExt allows it).
     */
    public boolean interceptStart(IApplicationThread caller, ActivityRecord startActivity,
            ActivityRecord reusedActivity, ActivityRecord sourceRecord, int startFlags,
            boolean isTask, String reason) {
        if (!Features.isPvr2DEnabled() || startActivity == null
                || "startResolvedActivity".equals(reason)) {
            return false;
        }
        if (!mSystemExt.checkClientService()) {
            Slog.w(TAG, "waiting SystemExt service : " + startActivity);
            return false;
        }
        if (ACTIVITY_FALLBACK_HOME.equals(startActivity.mActivityComponent.flattenToShortString())) {
            Slog.i(TAG, "allow start activity : " + startActivity);
            return false;
        }
        if (!isTask && startActivity.appInfo.getSmtEx().isPrefetch) {
            return false;
        }
        final ActivityInfo sourceActivityInfo = sourceRecord != null ? sourceRecord.info : null;
        final int targetDisplayId = reusedActivity != null ? reusedActivity.getDisplayId()
                : startActivity.getDisplayId();
        final int callingDisplayId = sourceRecord != null ? sourceRecord.getDisplayId() : -1;
        final boolean targetResumed = startActivity.isState(ActivityStack.ActivityState.RESUMED)
                || (reusedActivity != null
                        && reusedActivity.isState(ActivityStack.ActivityState.RESUMED));
        final PendingActivityLaunch pending =
                new PendingActivityLaunch(startActivity, sourceRecord, startFlags, isTask);
        final int startResult = mSystemExt.handleStartActivity(pending.seq, startActivity.intent,
                startActivity.info, sourceActivityInfo, targetDisplayId, callingDisplayId,
                startActivity.launchedFromPackage, targetResumed);
        Slog.i(TAG, "check if allow start activity : " + startActivity + ", source: "
                + sourceRecord + ", startResult: " + startResult);
        if (startResult == SystemExt.START_PENDING) {
            if (mPendingOnCheckingActivityLaunches.remove(pending)) {
                Slog.i(TAG, "remove duplicate starting activity : " + startActivity);
            }
            mPendingOnCheckingActivityLaunches.add(pending);
            return true;
        }
        final boolean intercept = startResult != SystemExt.START_SUCCESS;
        if (!intercept && reusedActivity != null && !mService.isSleepingLocked()) {
            final ActivityDisplay display = reusedActivity.getDisplay();
            if (display != null && display.mDisplayId != 0 && !display.mAllSleepTokens.isEmpty()) {
                Slog.i(TAG, "clear sleepToken for display: " + display + ", reusedActivity: "
                        + reusedActivity);
                display.mAllSleepTokens.clear();
                display.setIsSleeping(false);
            }
        }
        return intercept;
    }

    /** VRShell forwards 2D launches wrapped in a pvr.intent.action.VRSHELL intent. */
    public Intent getIntentFromVRShell(IApplicationThread caller, Intent intent) {
        synchronized (mService.getGlobalLock()) {
            try {
                WindowManagerService.boostPriorityForLockedSection();
                if (caller == null || intent == null
                        || !"pvr.intent.action.VRSHELL".equals(intent.getAction())) {
                    return intent;
                }
                final WindowProcessController callerApp = mService.getProcessController(caller);
                if (callerApp == null || !PicoUtils.isSystemApp(callerApp.mInfo)) {
                    return intent;
                }
                final Intent realIntent = intent.getParcelableExtra("intent");
                if (realIntent == null) {
                    return intent;
                }
                Slog.i(TAG, "find real intent from vrshell : " + realIntent);
                return realIntent;
            } finally {
                WindowManagerService.resetPriorityAfterLockedSection();
            }
        }
    }
}
