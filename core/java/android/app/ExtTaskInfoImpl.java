// Copyright 2026 Picomisu contributors
// SPDX-License-Identifier: Apache-2.0
package android.app;

import android.os.Bundle;
import android.os.Parcel;

/**
 * PICO task info extension (factory PICO OS 5.13.7 android.app.ExtTaskInfoImpl).
 * @hide
 */
public class ExtTaskInfoImpl implements IExtTaskInfo {
    private Bundle extras = new Bundle();
    private TaskInfo mBase;

    public ExtTaskInfoImpl(TaskInfo base) {
        mBase = base;
    }

    @Override
    public Bundle getExtras() {
        return extras;
    }

    @Override
    public void readFromParcel(Parcel source) {
        extras = source.readBundle();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeBundle(extras);
    }
}
