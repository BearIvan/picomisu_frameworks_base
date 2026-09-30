// Copyright 2026 Picomisu contributors
// SPDX-License-Identifier: Apache-2.0

package android.app;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * A slow main-thread message reported through
 * {@code com.android.internal.app.ITransferServer#reportAppMainTerribleMsg}. Reconstructed from
 * the PICO OS 5.13.7 factory framework.
 *
 * @hide
 */
public class AppMainMsgInfo implements Parcelable {
    public String weightMsg;
    public int count;
    public long dispatchMills;
    public long finishMills;
    public long idleMills;
    public long msgDispatchMills;

    public AppMainMsgInfo() {
    }

    public AppMainMsgInfo(String weightMsg, int count, long dispatchMills, long finishMills,
            long idleMills, long msgDispatchMills) {
        this.weightMsg = weightMsg;
        this.count = count;
        this.dispatchMills = dispatchMills;
        this.finishMills = finishMills;
        this.idleMills = idleMills;
        this.msgDispatchMills = msgDispatchMills;
    }

    protected AppMainMsgInfo(Parcel in) {
        weightMsg = in.readString();
        count = in.readInt();
        dispatchMills = in.readLong();
        finishMills = in.readLong();
        idleMills = in.readLong();
        msgDispatchMills = in.readLong();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(weightMsg);
        dest.writeInt(count);
        dest.writeLong(dispatchMills);
        dest.writeLong(finishMills);
        dest.writeLong(idleMills);
        dest.writeLong(msgDispatchMills);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<AppMainMsgInfo> CREATOR = new Creator<AppMainMsgInfo>() {
        @Override
        public AppMainMsgInfo createFromParcel(Parcel in) {
            return new AppMainMsgInfo(in);
        }

        @Override
        public AppMainMsgInfo[] newArray(int size) {
            return new AppMainMsgInfo[size];
        }
    };
}
