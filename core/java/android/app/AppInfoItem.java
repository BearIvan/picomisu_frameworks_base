// Copyright 2026 Picomisu contributors
// SPDX-License-Identifier: Apache-2.0

package android.app;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Smartisan activity life-cycle event payload, delivered to an
 * {@link IActivityLifeCycleObserver}. Reconstructed from the PICO OS 5.13.7 factory framework.
 *
 * @hide
 */
public class AppInfoItem implements Parcelable {
    public String pkgName;
    public String activityName;
    public int userId;
    public long versionCode;
    public String label;
    public boolean isSystemApp;

    protected AppInfoItem(Parcel in) {
        pkgName = in.readString();
        activityName = in.readString();
        userId = in.readInt();
        versionCode = in.readLong();
        label = in.readString();
        isSystemApp = in.readBoolean();
    }

    public AppInfoItem(String pkgName, String activityName, int userId, long versionCode,
            String label, boolean isSystemApp) {
        this.pkgName = pkgName;
        this.activityName = activityName;
        this.userId = userId;
        this.versionCode = versionCode;
        this.label = label;
        this.isSystemApp = isSystemApp;
    }

    public AppInfoItem() {
    }

    public static final Creator<AppInfoItem> CREATOR = new Creator<AppInfoItem>() {
        @Override
        public AppInfoItem createFromParcel(Parcel in) {
            return new AppInfoItem(in);
        }

        @Override
        public AppInfoItem[] newArray(int size) {
            return new AppInfoItem[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(pkgName);
        dest.writeString(activityName);
        dest.writeInt(userId);
        dest.writeLong(versionCode);
        dest.writeString(label);
        dest.writeBoolean(isSystemApp);
    }

    public void readFromParcel(Parcel source) {
        pkgName = source.readString();
        activityName = source.readString();
        userId = source.readInt();
        versionCode = source.readLong();
        label = source.readString();
        isSystemApp = source.readBoolean();
    }
}
