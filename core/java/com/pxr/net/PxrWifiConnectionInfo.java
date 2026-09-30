// Copyright 2026 Picomisu contributors
// SPDX-License-Identifier: Apache-2.0

package com.pxr.net;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Wi-Fi link layer statistics snapshot (per access category MPDU counters, radio and
 * signal poll data) pushed to IPxrNetworkManager.updateLinkLayerQuality().
 *
 * @hide
 */
public class PxrWifiConnectionInfo implements Parcelable {
    private static final String TAG = "PxrWifiConnectionInfo";

    public long txmpdu_be;
    public long txmpdu_bk;
    public long txmpdu_vi;
    public long txmpdu_vo;
    public long retries_be;
    public long retries_bk;
    public long retries_vi;
    public long retries_vo;
    public long lostmpdu_be;
    public long lostmpdu_bk;
    public long lostmpdu_vi;
    public long lostmpdu_vo;
    public long rxmpdu_be;
    public long rxmpdu_bk;
    public long rxmpdu_vi;
    public long rxmpdu_vo;
    public int contention_atime_be;
    public int contention_atime_bk;
    public int contention_atime_vi;
    public int contention_atime_vo;
    public int radioOnTimeMs;
    public int ccaBusyTimeMs;
    public long llstatTimeStamp;
    public int fcsError;
    public int txBytes;
    public int rxBytes;
    public int txRateInfo;
    public int rxRateInfo;
    public long signalPollTimeStamp;

    public PxrWifiConnectionInfo() {
    }

    public void reset() {
    }

    /** Copy constructor; the factory implementation does not copy any field. */
    public PxrWifiConnectionInfo(PxrWifiConnectionInfo source) {
        if (source != null) {
        }
    }

    @Override
    public String toString() {
        StringBuilder sbuf = new StringBuilder();
        sbuf.append(" PxrWifiConnectionInfo: ");
        sbuf.append(" txmpdu_be: ").append(Long.toString(this.txmpdu_be));
        sbuf.append(" txmpdu_bk: ").append(Long.toString(this.txmpdu_bk));
        sbuf.append(" txmpdu_vi: ").append(Long.toString(this.txmpdu_vi));
        sbuf.append(" txmpdu_vo: ").append(Long.toString(this.txmpdu_vo));
        sbuf.append(" retries_be: ").append(Long.toString(this.retries_be));
        sbuf.append(" retries_bk: ").append(Long.toString(this.retries_bk));
        sbuf.append(" retries_vi: ").append(Long.toString(this.retries_vi));
        sbuf.append(" retries_vo: ").append(Long.toString(this.retries_be));  // Note: the factory prints retries_be here.
        sbuf.append(" lostmpdu_be: ").append(Long.toString(this.lostmpdu_be));
        sbuf.append(" lostmpdu_bk: ").append(Long.toString(this.lostmpdu_bk));
        sbuf.append(" lostmpdu_vi: ").append(Long.toString(this.lostmpdu_vi));
        sbuf.append(" lostmpdu_vo: ").append(Long.toString(this.lostmpdu_vo));
        sbuf.append(" rxmpdu_be: ").append(Long.toString(this.rxmpdu_be));
        sbuf.append(" rxmpdu_bk: ").append(Long.toString(this.rxmpdu_bk));
        sbuf.append(" rxmpdu_vi: ").append(Long.toString(this.rxmpdu_vi));
        sbuf.append(" rxmpdu_vo: ").append(Long.toString(this.rxmpdu_vo));
        sbuf.append(" contention_atime_be: ").append(Integer.toString(this.contention_atime_be));
        sbuf.append(" contention_atime_bk: ").append(Integer.toString(this.contention_atime_bk));
        sbuf.append(" contention_atime_vi: ").append(Integer.toString(this.contention_atime_vi));
        sbuf.append(" contention_atime_vo: ").append(Integer.toString(this.contention_atime_vo));
        sbuf.append(" radioOnTimeMs: ").append(Integer.toString(this.radioOnTimeMs));
        sbuf.append(" ccaBusyTimeMs: ").append(Integer.toString(this.ccaBusyTimeMs));
        sbuf.append(" llstatTimeStamp: ").append(Long.toString(this.llstatTimeStamp));
        sbuf.append(" fcsError: ").append(Integer.toString(this.fcsError));
        sbuf.append(" txBytes: ").append(Integer.toString(this.txBytes));
        sbuf.append(" rxBytes: ").append(Integer.toString(this.rxBytes));
        sbuf.append(" txRateInfo: ").append(Integer.toString(this.txRateInfo));
        sbuf.append(" rxRateInfo: ").append(Integer.toString(this.rxRateInfo));
        sbuf.append(" signalPollTimeStamp: ").append(Long.toString(this.signalPollTimeStamp));
        return sbuf.toString();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeLong(txmpdu_be);
        dest.writeLong(txmpdu_bk);
        dest.writeLong(txmpdu_vi);
        dest.writeLong(txmpdu_vo);
        dest.writeLong(retries_be);
        dest.writeLong(retries_bk);
        dest.writeLong(retries_vi);
        dest.writeLong(retries_vo);
        dest.writeLong(lostmpdu_be);
        dest.writeLong(lostmpdu_bk);
        dest.writeLong(lostmpdu_vi);
        dest.writeLong(lostmpdu_vo);
        dest.writeLong(rxmpdu_be);
        dest.writeLong(rxmpdu_bk);
        dest.writeLong(rxmpdu_vi);
        dest.writeLong(rxmpdu_vo);
        dest.writeInt(contention_atime_be);
        dest.writeInt(contention_atime_bk);
        dest.writeInt(contention_atime_vi);
        dest.writeInt(contention_atime_vo);
        dest.writeInt(radioOnTimeMs);
        dest.writeInt(ccaBusyTimeMs);
        dest.writeLong(llstatTimeStamp);
        dest.writeInt(fcsError);
        dest.writeInt(txBytes);
        dest.writeInt(rxBytes);
        dest.writeInt(txRateInfo);
        dest.writeInt(rxRateInfo);
        dest.writeLong(signalPollTimeStamp);
    }

    public static final Parcelable.Creator<PxrWifiConnectionInfo> CREATOR =
            new Parcelable.Creator<PxrWifiConnectionInfo>() {
        public PxrWifiConnectionInfo createFromParcel(Parcel in) {
            PxrWifiConnectionInfo info = new PxrWifiConnectionInfo();
            info.txmpdu_be = in.readLong();
            info.txmpdu_bk = in.readLong();
            info.txmpdu_vi = in.readLong();
            info.txmpdu_vo = in.readLong();
            info.retries_be = in.readLong();
            info.retries_bk = in.readLong();
            info.retries_vi = in.readLong();
            info.retries_vo = in.readLong();
            info.lostmpdu_be = in.readLong();
            info.lostmpdu_bk = in.readLong();
            info.lostmpdu_vi = in.readLong();
            info.lostmpdu_vo = in.readLong();
            info.rxmpdu_be = in.readLong();
            info.rxmpdu_bk = in.readLong();
            info.rxmpdu_vi = in.readLong();
            info.rxmpdu_vo = in.readLong();
            info.contention_atime_be = in.readInt();
            info.contention_atime_bk = in.readInt();
            info.contention_atime_vi = in.readInt();
            info.contention_atime_vo = in.readInt();
            info.radioOnTimeMs = in.readInt();
            info.ccaBusyTimeMs = in.readInt();
            info.llstatTimeStamp = in.readLong();
            info.fcsError = in.readInt();
            info.txBytes = in.readInt();
            info.rxBytes = in.readInt();
            info.txRateInfo = in.readInt();
            info.rxRateInfo = in.readInt();
            // Note: the factory writes signalPollTimeStamp as a long but reads an int.
            info.signalPollTimeStamp = in.readInt();
            return info;
        }

        public PxrWifiConnectionInfo[] newArray(int size) {
            return new PxrWifiConnectionInfo[size];
        }
    };
}
