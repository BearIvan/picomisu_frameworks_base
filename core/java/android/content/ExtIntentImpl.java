// Copyright 2026 Picomisu contributors
// SPDX-License-Identifier: Apache-2.0
package android.content;

import android.os.Bundle;
import android.os.Parcel;

/**
 * PICO intent extension (factory PICO OS 5.13.7 android.content.ExtIntentImpl).
 * @hide
 */
public class ExtIntentImpl implements IExtIntent {
    private Intent mBase;
    private Bundle mExtras = new Bundle();

    public ExtIntentImpl(Intent base) {
        mBase = base;
    }

    @Override
    public void writeToParcel(Parcel out, int flags) {
        out.writeBundle(mExtras);
    }

    @Override
    public void readFromParcel(Parcel in) {
        mExtras = in.readBundle();
    }

    @Override
    public void copyFrom(Intent source) {
        mExtras = (Bundle) source.getExt().getExtras().clone();
    }

    @Override
    public Bundle getExtras() {
        return mExtras;
    }
}
