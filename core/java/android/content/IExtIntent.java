// Copyright 2026 Picomisu contributors
// SPDX-License-Identifier: Apache-2.0
package android.content;

import android.os.Bundle;
import android.os.Parcel;

import com.pico.util.IExtBase;

/**
 * PICO intent extension (factory PICO OS 5.13.7 android.content.IExtIntent): a private extras
 * bundle that is copied and parceled with the intent.
 * @hide
 */
public interface IExtIntent extends IExtBase {
    void copyFrom(Intent intent);

    Bundle getExtras();

    void readFromParcel(Parcel in);

    void writeToParcel(Parcel out, int flags);
}
