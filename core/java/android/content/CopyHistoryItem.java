// Copyright 2026 Picomisu contributors
// SPDX-License-Identifier: Apache-2.0
package android.content;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Smartisan clipboard copy-history entry (factory PICO OS 5.13.7 android.content.CopyHistoryItem),
 * exchanged through {@link IClipboardSmtEx}. Newest first.
 * @hide
 */
public class CopyHistoryItem implements Parcelable, Comparable<CopyHistoryItem> {
    public final String mContent;
    public final long mTimeStamp;

    public CopyHistoryItem(String content, long timestamp) {
        mContent = content;
        mTimeStamp = timestamp;
    }

    public CopyHistoryItem(Parcel source) {
        mContent = source.readString();
        mTimeStamp = source.readLong();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(mContent);
        dest.writeLong(mTimeStamp);
    }

    public static final Parcelable.Creator<CopyHistoryItem> CREATOR =
            new Parcelable.Creator<CopyHistoryItem>() {
        @Override
        public CopyHistoryItem createFromParcel(Parcel source) {
            return new CopyHistoryItem(source);
        }

        @Override
        public CopyHistoryItem[] newArray(int size) {
            return new CopyHistoryItem[size];
        }
    };

    @Override
    public boolean equals(Object o) {
        return o != null && (o instanceof CopyHistoryItem) && compareTo((CopyHistoryItem) o) == 0;
    }

    @Override
    public int compareTo(CopyHistoryItem another) {
        if (mTimeStamp != another.mTimeStamp) {
            return mTimeStamp > another.mTimeStamp ? -1 : 1;
        }
        int res = mContent.compareTo(another.mContent);
        if (res != 0) {
            return res;
        }
        return 0;
    }
}
