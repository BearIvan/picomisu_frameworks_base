// Copyright 2026 Picomisu contributors
// SPDX-License-Identifier: Apache-2.0
package android.view;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * A typed point on a display (factory PICO OS 5.13.7 android.view.FooPointInfo). As in the
 * factory, writeToParcel writes displayId as a float while readFromParcel reads an int.
 * @hide
 */
public class FooPointInfo implements Parcelable {
    public static final int TYPE_DEFAULT = 0;
    public static final int TYPE_DISMISS = 1;

    public int type;
    public float x;
    public float y;
    public int displayId;

    public FooPointInfo() {
    }

    public FooPointInfo(float x, float y) {
        this(TYPE_DEFAULT, x, y);
    }

    public FooPointInfo(int type, float x, float y) {
        this(type, x, y, 0);
    }

    public FooPointInfo(float x, float y, int displayId) {
        this(TYPE_DEFAULT, x, y, displayId);
    }

    public FooPointInfo(int type, float x, float y, int displayId) {
        this.type = type;
        this.x = x;
        this.y = y;
        this.displayId = displayId;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(type);
        dest.writeFloat(x);
        dest.writeFloat(y);
        dest.writeFloat(displayId);
    }

    public void readFromParcel(Parcel in) {
        type = in.readInt();
        x = in.readFloat();
        y = in.readFloat();
        displayId = in.readInt();
    }

    public static final Parcelable.Creator<FooPointInfo> CREATOR =
            new Parcelable.Creator<FooPointInfo>() {
        @Override
        public FooPointInfo createFromParcel(Parcel in) {
            FooPointInfo r = new FooPointInfo();
            r.readFromParcel(in);
            return r;
        }

        @Override
        public FooPointInfo[] newArray(int size) {
            return new FooPointInfo[size];
        }
    };
}
