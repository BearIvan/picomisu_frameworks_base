// Copyright 2026 Picomisu contributors
// SPDX-License-Identifier: Apache-2.0
package com.android.server.wm;

import android.app.TaskInfo;
import android.pico.utils.Features;

/**
 * PICO task record extension (factory PICO OS 5.13.7 com.android.server.wm.ExtTaskRecordImpl):
 * TaskInfo extras carry the calling package of the task ("callingPackage").
 */
public class ExtTaskRecordImpl implements IExtTaskRecord {
    private static final String KEY_CALLING_PACKAGE = "callingPackage";
    private TaskRecord mBase;

    public ExtTaskRecordImpl(TaskRecord base) {
        mBase = base;
    }

    @Override
    public void fillTaskInfo(TaskInfo info) {
        if (!Features.isPvr2DEnabled() || info == null) {
            return;
        }
        info.getExt().getExtras().putString(KEY_CALLING_PACKAGE, mBase.mCallingPackage);
    }
}
