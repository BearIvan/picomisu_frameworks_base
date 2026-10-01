// Copyright 2026 Picomisu contributors
// SPDX-License-Identifier: Apache-2.0

package com.android.server.wm;

import com.pico.util.IExtBase;

/**
 * PICO activity stack extension. Reconstructed from the PICO OS 5.13.7 factory services: the
 * activity timeout report (IApplicationThread.scheduleActivityTimeout) and the caller-stack
 * focus model of 2D app displays.
 * @hide
 */
public interface IExtActivityStack extends IExtBase {
    void scheduleActivityTimeout(ActivityRecord r, String reason);

    /** The stack is being removed: stacks it launched inherit its caller stack. */
    void remove(ActivityStack currentStack);

    /** Records the 2D app stack that launched {@code record} as this stack's caller. */
    void updateCaller(ActivityRecord record);

    void setCallerStackId(int callerStackId, String reason);

    int getCallerStackId();

    /** Whether focus may pass from {@code currentFocus} to this stack (its caller stack). */
    boolean allowUse(ActivityStack currentFocus);

    /**
     * Moves focus to the next stack after a task went to the back. Returns false when the next
     * stack is on the same display (the caller continues the normal path).
     */
    boolean moveToBack(String reason);

    /** Next focusable stack for ActivityStack.adjustFocusToNextFocusableStack. */
    ActivityStack getNextFocusableStack(String reason, boolean ignoreCurrent);

    /** Destroys {@code r} later while it is the source of a pending startActivityAsCaller. */
    boolean delayDestroyActivityLocked(ActivityRecord r, boolean removeFromApp, String reason);

    boolean disableResumeNextFocusableActivityWhenStackIsEmpty();

    ActivityRecord getDeferResumeActivity();

    void setDeferResumeActivity(ActivityRecord r);

    /** Ensures visibility on this stack's display only after a pause; true when done. */
    boolean interruptCompletePauseLocked(ActivityRecord resuming);

    void onActivityDestroy(ActivityRecord r);

    /** Delivers the pending results of a VR activity right away. */
    void sendResultsToVrActivity(ActivityRecord r);
}
