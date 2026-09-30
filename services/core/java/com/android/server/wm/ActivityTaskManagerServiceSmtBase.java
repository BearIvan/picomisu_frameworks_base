// Copyright 2026 Picomisu contributors
// SPDX-License-Identifier: Apache-2.0

package com.android.server.wm;

import android.content.pm.ApplicationInfo;

/**
 * Smartisan extension state of the {@link ActivityTaskManagerService}. Reconstructed from the
 * PICO OS 5.13.7 factory services; only the members reached by the Smartisan
 * {@code IActivityManagerSmtEx} methods are present.
 *
 * @hide
 */
public class ActivityTaskManagerServiceSmtBase {
    protected ActivityTaskManagerService mAtmServices;

    public ActivityTaskManagerServiceSmtBase(ActivityTaskManagerService atmServices) {
        mAtmServices = atmServices;
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
