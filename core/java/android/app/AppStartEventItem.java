// Copyright 2026 Picomisu contributors
// SPDX-License-Identifier: Apache-2.0

package android.app;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.Date;

/**
 * Smartisan application start event payload, delivered to an
 * {@link IAppStartEventObserver}. Reconstructed from the PICO OS 5.13.7 factory framework.
 *
 * @hide
 */
public class AppStartEventItem implements Parcelable {
    public String pkgName;
    public int uid;
    public String versionCode;
    public String versionName;
    public String label;
    public String isSystemApp;
    public long duration;
    public long focusTime;

    protected AppStartEventItem(Parcel in) {
        pkgName = in.readString();
        uid = in.readInt();
        versionCode = in.readString();
        versionName = in.readString();
        label = in.readString();
        isSystemApp = in.readString();
        duration = in.readLong();
        focusTime = in.readLong();
    }

    public AppStartEventItem(String pkgName, int uid, String versionCode, String versionName,
            String label, String isSystemApp, long duration, long focusTime) {
        this.pkgName = pkgName;
        this.uid = uid;
        this.versionCode = versionCode;
        this.versionName = versionName;
        this.label = label;
        this.isSystemApp = isSystemApp;
        this.duration = duration;
        this.focusTime = focusTime;
    }

    public AppStartEventItem() {
    }

    public static final Creator<AppStartEventItem> CREATOR = new Creator<AppStartEventItem>() {
        @Override
        public AppStartEventItem createFromParcel(Parcel in) {
            return new AppStartEventItem(in);
        }

        @Override
        public AppStartEventItem[] newArray(int size) {
            return new AppStartEventItem[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(pkgName);
        dest.writeInt(uid);
        dest.writeString(versionCode);
        dest.writeString(versionName);
        dest.writeString(label);
        dest.writeString(isSystemApp);
        dest.writeLong(duration);
        dest.writeLong(focusTime);
    }

    public void readFromParcel(Parcel source) {
        pkgName = source.readString();
        uid = source.readInt();
        versionCode = source.readString();
        versionName = source.readString();
        label = source.readString();
        isSystemApp = source.readString();
        duration = source.readLong();
        focusTime = source.readLong();
    }

    @Override
    public String toString() {
        return "item pkgName:" + pkgName + " uid:" + uid + " label:" + label + " duration:"
                + (duration / 1000) + "s focusTime:" + new Date(focusTime) + " isSystemApp:"
                + isSystemApp + " versionCode:" + versionCode + " versionName:" + versionName;
    }
}
