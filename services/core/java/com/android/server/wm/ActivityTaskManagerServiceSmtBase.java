// Copyright 2026 Picomisu contributors
// SPDX-License-Identifier: Apache-2.0

package com.android.server.wm;

import android.content.pm.ApplicationInfo;

import com.android.internal.app.ProcessMap;

/**
 * Smartisan extension state of the {@link ActivityTaskManagerService}. Reconstructed from the
 * PICO OS 5.13.7 factory services; only the members reached by the Smartisan
 * {@code IActivityManagerSmtEx} methods are present.
 *
 * @hide
 */
public class ActivityTaskManagerServiceSmtBase {
    protected ActivityTaskManagerService mAtmServices;
    /** Process of the previous VR activity, kept by the sys services JAR. */
    public WindowProcessController mPreviousVrProcess = null;
    /** Prefetched (pre-started) application processes by name and uid. */
    final ProcessMap<WindowProcessController> mPrefetchProcessNames = new ProcessMap<>();

    public ActivityTaskManagerServiceSmtBase(ActivityTaskManagerService atmServices) {
        mAtmServices = atmServices;
    }

    public WindowProcessController getPreviousVrProcess() {
        synchronized (mAtmServices.mGlobalLock) {
            return mPreviousVrProcess;
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
}
