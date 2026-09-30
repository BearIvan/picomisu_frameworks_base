// Copyright 2026 Picomisu contributors
// SPDX-License-Identifier: Apache-2.0

package com.pxr.net;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Per-process network throughput sample reported to INetworkQualityListener.
 *
 * @hide
 */
public class NetworkQuality implements Parcelable {
    int mPid;
    long mCurrentTime;
    int mCurrentPidTx;
    int mCurrentPidRx;
    int mSystemTx;
    int mSystemRx;
    int mTxLinkSpeed;
    int mRxLinkSpeed;
    int mWifiThroughput;

    public NetworkQuality() {
    }

    public NetworkQuality(int pid, long currentTime, int currentPidTx, int currentPidRx,
            int systemTx, int systemRx, int tlp, int rlp, int wifiThroughput) {
        mPid = pid;
        mCurrentTime = currentTime;
        mCurrentPidTx = currentPidTx;
        mCurrentPidRx = currentPidRx;
        mSystemTx = systemTx;
        mSystemRx = systemRx;
        mTxLinkSpeed = tlp;
        mRxLinkSpeed = rlp;
        mWifiThroughput = wifiThroughput;
    }

    private NetworkQuality(Parcel in) {
        readFromParcel(in);
    }

    public long getPid() {
        return mPid;
    }

    public long getCurrentTime() {
        return mCurrentTime;
    }

    public int getCurrentPidTx() {
        return mCurrentPidTx;
    }

    public int getCurrentPidRx() {
        return mCurrentPidRx;
    }

    public int getSystemTx() {
        return mSystemTx;
    }

    public int getSystemRx() {
        return mSystemRx;
    }

    public int getTxLinkSpeed() {
        return mTxLinkSpeed;
    }

    public int getRxLinkSpeed() {
        return mRxLinkSpeed;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        // Note: the factory writes the pid as a long but reads it back as an int.
        dest.writeLong(mPid);
        dest.writeLong(mCurrentTime);
        dest.writeInt(mCurrentPidTx);
        dest.writeInt(mCurrentPidRx);
        dest.writeInt(mSystemTx);
        dest.writeInt(mSystemRx);
        dest.writeInt(mTxLinkSpeed);
        dest.writeInt(mRxLinkSpeed);
        dest.writeInt(mWifiThroughput);
    }

    public void readFromParcel(Parcel in) {
        mPid = in.readInt();
        mCurrentTime = in.readLong();
        mCurrentPidTx = in.readInt();
        mCurrentPidRx = in.readInt();
        mSystemTx = in.readInt();
        mSystemRx = in.readInt();
        mTxLinkSpeed = in.readInt();
        mRxLinkSpeed = in.readInt();
        mWifiThroughput = in.readInt();
    }

    public static final Parcelable.Creator<NetworkQuality> CREATOR =
            new Parcelable.Creator<NetworkQuality>() {
        public NetworkQuality createFromParcel(Parcel in) {
            return new NetworkQuality(in);
        }

        public NetworkQuality[] newArray(int size) {
            return new NetworkQuality[size];
        }
    };

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("pxr network tools data {");
        builder.append(", mPid=").append(mPid);
        builder.append("mCurrentTime=").append(mCurrentTime);
        builder.append(", mCurrentPidTx=").append(mCurrentPidTx);
        builder.append(", mCurrentPidRx=").append(mCurrentPidRx);
        builder.append(", mSystemTx=").append(mSystemTx);
        builder.append(", mSystemRx=").append(mSystemRx);
        builder.append(", mTxLinkSpeed=").append(mTxLinkSpeed);
        builder.append(", mRxLinkSpeed=").append(mRxLinkSpeed);
        builder.append(", mWifiThroughput=").append(mWifiThroughput);
        builder.append('}');
        return builder.toString();
    }
}
