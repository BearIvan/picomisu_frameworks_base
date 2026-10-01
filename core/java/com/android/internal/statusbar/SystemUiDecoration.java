// Copyright 2026 Picomisu contributors
// SPDX-License-Identifier: Apache-2.0
package com.android.internal.statusbar;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Smartisan per-package system bar decoration (factory PICO OS 5.13.7
 * com.android.internal.statusbar.SystemUiDecoration).
 * @hide
 */
public class SystemUiDecoration implements Parcelable {
    public static int USER_CUSTOMIZED = 0;
    public static int COLOR_PICKER_CUSTOMIZED = 1;
    public static int COLOR_PICKER_CHANGE = 2;
    public static int WAITING_CUSTOMIZED = 3;

    public String mPackageName;
    public int mIconAndTextColor;
    public int mNotificationNumberColor;
    public int mStatusBarBackground;
    public int mNavigationBarBackground;
    public int mCustomMode = USER_CUSTOMIZED;

    public SystemUiDecoration(String pkg, int text, int number, int sbSrcid, int navSrcid) {
        this(pkg, text, number, sbSrcid, navSrcid, USER_CUSTOMIZED);
    }

    public SystemUiDecoration(String pkg, int text, int number, int sbSrcid, int navSrcid,
            int mode) {
        mPackageName = pkg;
        mIconAndTextColor = text;
        mNotificationNumberColor = number;
        mStatusBarBackground = sbSrcid;
        mNavigationBarBackground = navSrcid;
        mCustomMode = mode;
    }

    @Override
    public String toString() {
        return "(SystemUiDecoration (pkg = " + mPackageName
                + " icon and text color = 0x" + Integer.toHexString(mIconAndTextColor)
                + " notification number color = 0x" + Integer.toHexString(mNotificationNumberColor)
                + " status bar background id = 0x" + Integer.toHexString(mStatusBarBackground)
                + " navigation bar background id = 0x"
                + Integer.toHexString(mNavigationBarBackground)
                + " decoration mode:" + toModeString(mCustomMode) + ")";
    }

    @Override
    public SystemUiDecoration clone() {
        SystemUiDecoration that = new SystemUiDecoration(mPackageName, mIconAndTextColor,
                mNotificationNumberColor, mStatusBarBackground, mNavigationBarBackground,
                mCustomMode);
        return that;
    }

    public boolean equals(SystemUiDecoration that) {
        if (that == null
                || mIconAndTextColor != that.mIconAndTextColor
                || mNotificationNumberColor != that.mNotificationNumberColor
                || mStatusBarBackground != that.mStatusBarBackground
                || mNavigationBarBackground != that.mNavigationBarBackground
                || mCustomMode != that.mCustomMode) {
            return false;
        }
        return true;
    }

    public SystemUiDecoration(Parcel in) {
        readFromParcel(in);
    }

    public void readFromParcel(Parcel in) {
        mPackageName = in.readString();
        mIconAndTextColor = in.readInt();
        mNotificationNumberColor = in.readInt();
        mStatusBarBackground = in.readInt();
        mNavigationBarBackground = in.readInt();
        mCustomMode = in.readInt();
    }

    @Override
    public void writeToParcel(Parcel out, int flags) {
        out.writeString(mPackageName);
        out.writeInt(mIconAndTextColor);
        out.writeInt(mNotificationNumberColor);
        out.writeInt(mStatusBarBackground);
        out.writeInt(mNavigationBarBackground);
        out.writeInt(mCustomMode);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Parcelable.Creator<SystemUiDecoration> CREATOR =
            new Parcelable.Creator<SystemUiDecoration>() {
        @Override
        public SystemUiDecoration createFromParcel(Parcel parcel) {
            return new SystemUiDecoration(parcel);
        }

        @Override
        public SystemUiDecoration[] newArray(int size) {
            return new SystemUiDecoration[size];
        }
    };

    public static String toModeString(int mode) {
        if (mode == USER_CUSTOMIZED) {
            return "USER_CUSTOMIZED";
        } else if (mode == COLOR_PICKER_CUSTOMIZED) {
            return "COLOR_PICKER_CUSTOMIZED";
        } else if (mode == COLOR_PICKER_CHANGE) {
            return "COLOR_PICKER_CHANGE";
        } else if (mode == WAITING_CUSTOMIZED) {
            return "WAITING_CUSTOMIZED";
        }
        return "null";
    }
}
