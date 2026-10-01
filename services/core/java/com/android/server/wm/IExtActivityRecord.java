// Copyright 2026 Picomisu contributors
// SPDX-License-Identifier: Apache-2.0
package com.android.server.wm;

import android.content.Intent;

import com.pico.util.IExtBase;

/**
 * PICO activity record extension (factory PICO OS 5.13.7
 * com.android.server.wm.IExtActivityRecord).
 * @hide
 */
public interface IExtActivityRecord extends IExtBase {
    ActivityRecord getSourceRecord();

    boolean isHomeAction(Intent intent);

    void setSourceRecord(ActivityRecord sourceRecord);
}
