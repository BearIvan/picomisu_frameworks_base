// Copyright 2026 Picomisu contributors
// SPDX-License-Identifier: Apache-2.0

package com.android.server.wm;

import static android.app.ActivityTaskManager.INVALID_STACK_ID;
import static android.view.Display.DEFAULT_DISPLAY;

import android.text.TextUtils;
import android.util.Slog;

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

    public ExtActivityStackImpl(ActivityStack base) {
        mBase = base;
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
        final RootActivityContainer root = mBase.mRootActivityContainer;
        for (int i = root.mActivityDisplays.size() - 1; i >= 0; i--) {
            final ActivityDisplay display = root.mActivityDisplays.get(i);
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
        ActivityRecord sourceRecord = record.mPicoSourceRecord;
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
            final ActivityStack sourceStack = sourceRecord.getActivityStack();
            callerStackId = sourceStack != null
                    ? sourceStack.getExt().getCallerStackId() : INVALID_STACK_ID;
            sourceRecord = sourceRecord.mPicoSourceRecord;
        }
        setCallerStackId(callerStackId, "setIntent");
    }

    private ActivityRecord getResumedActivity(String packageName) {
        final RootActivityContainer root = mBase.mRootActivityContainer;
        for (int i = root.mActivityDisplays.size() - 1; i >= 0; i--) {
            final ActivityRecord resumedActivity =
                    root.mActivityDisplays.get(i).getResumedActivity();
            if (resumedActivity != null && resumedActivity.packageName.equals(packageName)) {
                return resumedActivity;
            }
        }
        return null;
    }

    @Override
    public void setCallerStackId(int callerStackId, String reason) {
        Slog.i(TAG, "updateCaller from: " + mCallerStackId + ", to: " + callerStackId
                + ", baseStackId: " + mBase.mStackId + ", reason: " + reason);
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
        final ActivityDisplay display = mBase.getDisplay();
        final ActivityStack next = display != null
                ? display.getNextFocusableStackInner(mBase, ignoreCurrent) : null;
        if (next != null && (!display.getExt().isVr2dDisplay() || next.getExt().allowUse(mBase))) {
            return next;
        }
        if ("clear-task-top finishActivity adjustFocus".equals(reason)
                || "clear-task-all finishActivity adjustFocus".equals(reason)) {
            return null;
        }
        ActivityStack stack = mBase.mRootActivityContainer.getNextFocusableStack(mBase,
                ignoreCurrent);
        if (stack == null) {
            stack = getTopVisibleStack(display);
            if (ActivityTaskManagerDebugConfig.DEBUG_TASKS) {
                Slog.i(TAG, "getTopVisibleStack: " + stack + ", current: " + mBase.getStackId());
            }
        }
        if ("moveTaskToBackLocked".equals(reason) || next != null) {
            mBase.mService.getActivityStartController().getExt().onTaskMovedToBack(display);
        }
        return stack;
    }

    /** Top visible stack of another display that has a resumed activity. */
    private ActivityStack getTopVisibleStack(ActivityDisplay currentDisplay) {
        final RootActivityContainer root = mBase.mRootActivityContainer;
        for (int i = root.getChildCount() - 1; i >= 0; i--) {
            final ActivityDisplay display = root.getChildAt(i);
            if (display == currentDisplay || display.getResumedActivity() == null) {
                continue;
            }
            final ActivityStack topStack = display.getTopStack();
            if (topStack != null && topStack.shouldBeVisible(null /* starting */)) {
                return topStack;
            }
        }
        return null;
    }
}
