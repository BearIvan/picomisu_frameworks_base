// Copyright 2026 Picomisu contributors
// SPDX-License-Identifier: Apache-2.0
package android.app;

import android.os.Bundle;
import android.os.Parcel;

import com.pico.util.IExtBase;

/**
 * PICO task info extension (factory PICO OS 5.13.7 android.app.IExtTaskInfo): extras parceled
 * after the AOSP fields (system_server puts the task's "callingPackage" there).
 * @hide
 */
public interface IExtTaskInfo extends IExtBase {
    Bundle getExtras();

    void readFromParcel(Parcel source);

    void writeToParcel(Parcel dest, int flags);
}
