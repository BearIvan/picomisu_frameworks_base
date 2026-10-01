// Copyright 2026 Picomisu contributors
// SPDX-License-Identifier: Apache-2.0
package com.android.server.wm;

import android.pico.utils.Features;

/**
 * PICO root activity container extension (factory PICO OS 5.13.7
 * com.android.server.wm.ExtRootActivityContainerImpl).
 */
public class ExtRootActivityContainerImpl implements IExtRootActivityContainer {
    private static final String TAG = "ExtRootActivityContainer";
    private RootActivityContainer mBase;

    public ExtRootActivityContainerImpl(RootActivityContainer base) {
        mBase = base;
    }

    /** RootActivityContainer.removeChild: an activity display was removed. */
    @Override
    public void onRemoveChild(ActivityDisplay activityDisplay) {
        if (Features.isPvr2DEnabled()) {
            mBase.mService.getActivityStartController().getExt().onDisplayRemoved(activityDisplay);
        }
    }
}
