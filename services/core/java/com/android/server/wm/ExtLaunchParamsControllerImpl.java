// Copyright 2026 Picomisu contributors
// SPDX-License-Identifier: Apache-2.0
package com.android.server.wm;

import android.content.Context;

/**
 * PICO launch params controller extension (factory PICO OS 5.13.7
 * com.android.server.wm.ExtLaunchParamsControllerImpl). Not called by the factory either.
 */
public class ExtLaunchParamsControllerImpl implements IExtLaunchParamsController {
    private LaunchParamsController mBase;

    public ExtLaunchParamsControllerImpl(LaunchParamsController base) {
        mBase = base;
    }

    /** A VR activity that is not prefetched launches on display 0. */
    @Override
    public void modifyResultDisplayId(Context context, ActivityRecord record,
            LaunchParamsController.LaunchParams result) {
        if (result.mPreferredDisplayId != 0 && record != null
                && !record.appInfo.getSmtEx().isPrefetch && record.info.getExt().isVrActivity()) {
            result.mPreferredDisplayId = 0;
        }
    }
}
