// Copyright 2026 Picomisu contributors
// SPDX-License-Identifier: Apache-2.0
package com.android.server.wm;

import android.content.ComponentName;
import android.content.Intent;

/**
 * PICO activity record extension (factory PICO OS 5.13.7
 * com.android.server.wm.ExtActivityRecordImpl): the activity that started this one, and the
 * VRShell intent as a home intent.
 */
public class ExtActivityRecordImpl implements IExtActivityRecord {
    private ActivityRecord mBase;
    private ActivityRecord mSourceRecord;

    public ExtActivityRecordImpl(ActivityRecord base) {
        mBase = base;
    }

    /** ActivityRecord.isHomeIntent: pvr.intent.action.VRSHELL for com.pvr.vrshell. */
    @Override
    public boolean isHomeAction(Intent intent) {
        ComponentName componentName;
        if ("pvr.intent.action.VRSHELL".equals(intent.getAction())
                && (componentName = intent.getComponent()) != null
                && "com.pvr.vrshell".equals(componentName.getPackageName())) {
            return true;
        }
        return false;
    }

    @Override
    public void setSourceRecord(ActivityRecord sourceRecord) {
        mSourceRecord = sourceRecord;
    }

    @Override
    public ActivityRecord getSourceRecord() {
        return mSourceRecord;
    }
}
