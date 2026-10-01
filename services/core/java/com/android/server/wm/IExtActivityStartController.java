// Copyright 2026 Picomisu contributors
// SPDX-License-Identifier: Apache-2.0
package com.android.server.wm;

import android.app.ActivityManager;
import android.app.ActivityOptions;
import android.app.IApplicationThread;
import android.content.Intent;
import android.content.res.Configuration;
import android.util.MergedConfiguration;

import com.pico.util.IExtBase;

/**
 * PICO activity start controller extension (factory PICO OS 5.13.7
 * com.android.server.wm.IExtActivityStartController): the 2D app virtual display routing
 * through SystemExt.
 * @hide
 */
public interface IExtActivityStartController extends IExtBase {
    void calculateDisplayId(ActivityRecord startActivity, TaskRecord candidateTask,
            ActivityOptions options, LaunchParamsController.LaunchParams launchParams);

    boolean forceNewTask(ActivityRecord startActivity, ActivityRecord sourceRecord);

    ActivityManager.RunningTaskInfo getDefaultDisplayTopTaskInfo();

    Configuration getGlobalConfiguration(ActivityRecord activityRecord);

    Intent getIntentFromVRShell(IApplicationThread caller, Intent intent);

    SystemExt getSystemExt();

    void handleClientActivityOrTaskBatchAllowedResult(long[] seq);

    void handleClientActivityOrTaskSwitchAllowedResult(long seq, boolean allowed);

    void handleClientVirtualDisplayVisibilityChanged(int displayId, boolean show);

    boolean handleResizeVirtualDisplay(DisplayContent displayContent, int reqOrientation,
            boolean forceUpdate);

    boolean interceptStart(IApplicationThread caller, ActivityRecord startActivity,
            ActivityRecord reusedActivity, ActivityRecord sourceRecord, int startFlags,
            boolean isTask, String reason);

    void obtainStarter(Intent intent, String reason);

    void onActivityResumed(ActivityRecord activityRecord);

    void onDefaultDisplayTopTaskChanged(ActivityManager.RunningTaskInfo runningTaskInfo);

    void onDisplayConfigurationChanged(DisplayContent displayContent);

    void onDisplayRemoved(ActivityDisplay activityDisplay);

    void onFinishActivity(ActivityRecord r);

    void onNewDisplayAdded(ActivityDisplay activityDisplay);

    void onReportResized(WindowState w, MergedConfiguration mergedConfiguration);

    void onSystemExtDied();

    void onSystemReady();

    void onTaskMovedToBack(ActivityDisplay display);

    void onTaskMovedToFront(ActivityDisplay activityDisplay, String reason);

    void onTaskRemoved(TaskRecord tr);

    boolean onWindowProcessControllerInit(WindowProcessController app);

    void reuseTask(ActivityRecord startActivity, RootActivityContainer.FindTaskResult tmpResult,
            RootActivityContainer.FindTaskResult result, boolean isPreferDisplay);

    int startActivityFromRecents(int taskId, int callingPid, int callingUid,
            SafeActivityOptions options);
}
