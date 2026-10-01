// Copyright 2026 Picomisu contributors
// SPDX-License-Identifier: Apache-2.0

package android.content;

import android.os.Parcel;

/**
 * Smartisan launch timing carried by an intent (factory PICO OS 5.13.7
 * {@code android.content.IntentMonitorEx}; nothing in the factory jars creates it).
 *
 * @hide
 */
public class IntentMonitorEx {
    private long mLaunchStartTime = -1;
    private long mAMSStartTime = -1;

    public void readFromParcel(Parcel in) {
        mLaunchStartTime = in.readLong();
    }

    public void writeToParcel(Parcel dest, int flags) {
        dest.writeLong(mLaunchStartTime);
    }

    public void markLaunchStartTime(long time) {
        mLaunchStartTime = time;
    }

    public long getLaunchStartTime() {
        return mLaunchStartTime;
    }

    public void markAMSStartTime(long time) {
        mAMSStartTime = time;
    }

    public long getAMSStartTime() {
        return mAMSStartTime;
    }
}
