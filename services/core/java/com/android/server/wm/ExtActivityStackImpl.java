// Copyright 2026 Picomisu contributors
// SPDX-License-Identifier: Apache-2.0

package com.android.server.wm;

import static android.app.ActivityTaskManager.INVALID_STACK_ID;
import static android.view.Display.DEFAULT_DISPLAY;

import android.app.ResultInfo;
import android.app.servertransaction.ActivityResultItem;
import android.os.RemoteException;
import android.pico.utils.Features;
import android.text.TextUtils;
import android.util.Slog;

import java.util.ArrayList;

/**
 * PICO activity stack extension (factory PICO OS 5.13.7 ExtActivityStackImpl).
 *
 * Caller-stack focus model: every stack created for an activity remembers the stack on a 2D
 * app display that launched it (following the source records while they stay on non-2D
 * displays with the same 2D app position). On a 2D app display focus only moves back to that
 * caller stack; any other next stack is on another display, so the panel's display is reported
 * to SystemExt as hidden and focus leaves it.
 * @hide
 */
public class ExtActivityStackImpl implements IExtActivityStack {
    private static final String TAG = "ExtActivityStack";
    private ActivityStack mBase;
    private int mCallerStackId = INVALID_STACK_ID;
    private ActivityRecord mDeferResumeActivity;

    public ExtActivityStackImpl(ActivityStack base) {
        mBase = base;
    }

    /**
     * ActivityStack.completePauseLocked: with 2D app displays only this stack's display updates
     * activity visibility after a pause.
     */
    @Override
    public boolean interruptCompletePauseLocked(ActivityRecord resuming) {
        final ActivityDisplay activityDisplay;
        if (!Features.isPvr2DEnabled() || (activityDisplay = mBase.getDisplay()) == null) {
            return false;
        }
        mBase.mStackSupervisor.getKeyguardController().beginActivityVisibilityUpdate();
        try {
            activityDisplay.ensureActivitiesVisible(resuming, 0, false /* preserveWindows */,
                    true /* notifyClients */);
            return true;
        } finally {
            mBase.mStackSupervisor.getKeyguardController().endActivityVisibilityUpdate();
        }
    }

    /** ActivityStack.resumeTopActivityInnerLocked with an empty stack on a 2D app display. */
    @Override
    public boolean disableResumeNextFocusableActivityWhenStackIsEmpty() {
        return ExtActivityStartControllerImpl.disableResumeNextFocusableActivityWhenStackIsEmpty(
                mBase.getDisplay());
    }

    /**
     * ActivityStack.destroyActivityLocked: an activity that is still the source of a pending
     * startActivityAsCaller (ATMS.mStartActivitySources) is destroyed 100 ms later.
     */
    @Override
    public boolean delayDestroyActivityLocked(final ActivityRecord r, final boolean removeFromApp,
            final String reason) {
        if (mBase.mService.mStartActivitySources.containsValue(r.appToken)) {
            Slog.d(TAG, "delayDestroyActivityLocked" + r + ",  reason: " + reason);
            mBase.mHandler.postDelayed(() -> {
                // The priority boost is added by the lockedregioncodeinjection pass, as on the
                // factory.
                synchronized (mBase.mService.mGlobalLock) {
                    mBase.destroyActivityLocked(r, removeFromApp, reason);
                }
            }, 100L);
            return true;
        }
        return false;
    }

    /**
     * Asks the process of {@code r} to log its main thread stack for an activity pause, stop,
     * destroy or top-resumed-state-loss timeout.
     */
    @Override
    public void scheduleActivityTimeout(ActivityRecord r, String reason) {
        if (!r.hasProcess()) {
            return;
        }
        try {
            r.app.getThread().scheduleActivityTimeout(reason);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void remove(ActivityStack currentStack) {
        for (int i = mBase.mRootActivityContainer.mActivityDisplays.size() - 1; i >= 0; i--) {
            final ActivityDisplay display = mBase.mRootActivityContainer.mActivityDisplays.get(i);
            for (int stackNdx = display.getChildCount() - 1; stackNdx >= 0; stackNdx--) {
                final ActivityStack stack = display.getChildAt(stackNdx);
                if (stack != currentStack
                        && stack.getExt().getCallerStackId() == currentStack.getStackId()) {
                    stack.getExt().setCallerStackId(currentStack.getExt().getCallerStackId(),
                            "stack remove");
                }
            }
        }
    }

    @Override
    public void updateCaller(ActivityRecord record) {
        if (record == null) {
            return;
        }
        final String position = record.info.getExt().get2dAppPosition();
        int callerStackId = INVALID_STACK_ID;
        ActivityRecord sourceRecord = record.getExt().getSourceRecord();
        if (sourceRecord == null) {
            sourceRecord = getResumedActivity(record.launchedFromPackage);
            Slog.w(TAG, "setIntent, get real sourceRecord: " + sourceRecord + ", record: "
                    + record);
        }
        while (sourceRecord != null && sourceRecord.getDisplay() != null) {
            final String callerPosition = sourceRecord.info.getExt().get2dAppPosition();
            Slog.i(TAG, "setIntent, find sourceRecord: " + sourceRecord + ", callerPosition: "
                    + callerPosition + ", position: " + position + ", display: "
                    + mBase.getDisplay());
            if (!TextUtils.equals(callerPosition, position)
                    || sourceRecord.getDisplay().mDisplayId == DEFAULT_DISPLAY) {
                break;
            }
            if (sourceRecord.getDisplay().getExt().isVr2dDisplay()) {
                callerStackId = sourceRecord.getStackId();
                break;
            }
            callerStackId = sourceRecord.getActivityStack().getExt().getCallerStackId();
            sourceRecord = sourceRecord.getExt().getSourceRecord();
        }
        setCallerStackId(callerStackId, "setIntent");
    }

    ActivityRecord getResumedActivity(String packageName) {
        for (int i = mBase.mRootActivityContainer.mActivityDisplays.size() - 1; i >= 0; i--) {
            final ActivityDisplay display = mBase.mRootActivityContainer.mActivityDisplays.get(i);
            final ActivityRecord resumedActivity = display.getResumedActivity();
            if (resumedActivity != null && resumedActivity.packageName.equals(packageName)) {
                return resumedActivity;
            }
        }
        return null;
    }

    @Override
    public void setCallerStackId(int callerStackId, String reason) {
        Slog.i(TAG, "updateCaller from: " + mCallerStackId + ", to: " + callerStackId
                + ", baseStackId: " + mBase.mStackId + ", reason: " + reason
                + ", current stack: " + mBase.getStackId());
        if (mBase.mStackId != callerStackId) {
            mCallerStackId = callerStackId;
        }
    }

    @Override
    public int getCallerStackId() {
        return mCallerStackId;
    }

    @Override
    public boolean allowUse(ActivityStack currentFocus) {
        if (currentFocus == null) {
            return false;
        }
        final int callerStackId = currentFocus.getExt().getCallerStackId();
        if (callerStackId != mBase.getStackId()) {
            return false;
        }
        Slog.i(TAG, "allowUse, useStack: " + mBase + ", callerStackId: " + callerStackId
                + ", topActivity: " + mBase.getTopActivity());
        return true;
    }

    @Override
    public boolean moveToBack(String reason) {
        final ActivityStack next = mBase.adjustFocusToNextFocusableStack(reason);
        if (next != null && next.getDisplay() == mBase.getDisplay()) {
            Slog.i(TAG, "moveToBack, use same display prev stack" + next);
            return false;
        }
        return true;
    }

    @Override
    public ActivityStack getNextFocusableStack(String reason, boolean ignoreCurrent) {
        final ActivityStack next = mBase.getDisplay().getNextFocusableStackInner(mBase,
                ignoreCurrent);
        if (next != null && (!mBase.getDisplay().getExt().isVr2dDisplay()
                || next.getExt().allowUse(mBase))) {
            return next;
        }
        if ("clear-task-top finishActivity adjustFocus".equals(reason)
                || "clear-task-all finishActivity adjustFocus".equals(reason)) {
            return null;
        }
        ActivityStack stack = mBase.mRootActivityContainer.getNextFocusableStack(mBase,
                ignoreCurrent);
        if (stack == null) {
            stack = getTopVisibleStack(mBase.getDisplay());
            if (ActivityTaskManagerDebugConfig.DEBUG_TASKS) {
                Slog.i(TAG, "getTopVisibleStack: " + stack + ", current: " + mBase.getStackId());
            }
        }
        if ("moveTaskToBackLocked".equals(reason) || next != null) {
            mBase.mService.getActivityStartController().getExt().onTaskMovedToBack(
                    mBase.getDisplay());
        }
        return stack;
    }

    /** Top visible stack of another display that has a resumed activity. */
    private ActivityStack getTopVisibleStack(ActivityDisplay currentDisplay) {
        for (int i = mBase.mRootActivityContainer.getChildCount() - 1; i >= 0; i--) {
            final ActivityDisplay display = mBase.mRootActivityContainer.getChildAt(i);
            if (display != mBase.getDisplay() && display.getResumedActivity() != null
                    && currentDisplay != display) {
                final ActivityStack topStack = display.getTopStack();
                if (topStack != null && topStack.shouldBeVisible(null /* starting */)) {
                    return topStack;
                }
            }
        }
        return null;
    }

    /** End of ActivityStack.destroyActivityLocked: SystemExt learns about the destroy. */
    @Override
    public void onActivityDestroy(ActivityRecord r) {
        SystemExt systemExt = mBase.mService.getActivityStartController().getExt().getSystemExt();
        systemExt.handleDestroyActivity(r.getActivityInfo());
    }

    /** ActivityStarter: the started activity that was not resumed (mDoResume false). */
    @Override
    public void setDeferResumeActivity(ActivityRecord r) {
        if (r != null) {
            Slog.i(TAG, r + " has been set to defer resume");
        }
        mDeferResumeActivity = r;
    }

    @Override
    public ActivityRecord getDeferResumeActivity() {
        return mDeferResumeActivity;
    }

    /**
     * ActivityStack.finishActivityResultsLocked: a VR activity that is attached to its process
     * gets its pending results right away (ActivityResultItem) instead of on its next resume.
     */
    @Override
    public void sendResultsToVrActivity(ActivityRecord r) {
        final ArrayList<ResultInfo> a;
        if (r.info.getExt().isVrActivity() && r.attachedToProcess()
                && (a = r.results) != null && a.size() > 0) {
            try {
                Slog.i(TAG, "trying to send the results to " + r);
                mBase.mService.getLifecycleManager().scheduleTransaction(r.app.getThread(),
                        r.appToken, ActivityResultItem.obtain(a));
                r.results = null;
            } catch (RemoteException e) {
                Slog.e(TAG, "RemoteException: of sendResultsToVrActivity" + e);
            }
        }
    }
}
