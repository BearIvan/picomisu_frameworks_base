// Copyright 2026 Picomisu contributors
// SPDX-License-Identifier: Apache-2.0

package android.app;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * A prefetched application killed by the Smartisan prefetch manager, reported to
 * {@link IPrefetchObserver}. Reconstructed from the PICO OS 5.13.7 factory framework.
 *
 * @hide
 */
public class PrefetchInfo implements Parcelable {
    public String pkgName;
    public int cantSwapMem;
    public int swapPercent;
    public String killReason;

    protected PrefetchInfo(Parcel in) {
        pkgName = in.readString();
        cantSwapMem = in.readInt();
        swapPercent = in.readInt();
        killReason = in.readString();
    }

    public PrefetchInfo(String pkgName, int cantSwapMem, int swapPercent, String killReason) {
        this.pkgName = pkgName;
        this.cantSwapMem = cantSwapMem;
        this.swapPercent = swapPercent;
        this.killReason = killReason;
    }

    public PrefetchInfo() {
    }

    public static final Creator<PrefetchInfo> CREATOR = new Creator<PrefetchInfo>() {
        @Override
        public PrefetchInfo createFromParcel(Parcel in) {
            return new PrefetchInfo(in);
        }

        @Override
        public PrefetchInfo[] newArray(int size) {
            return new PrefetchInfo[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(pkgName);
        dest.writeInt(cantSwapMem);
        dest.writeInt(swapPercent);
        dest.writeString(killReason);
    }

    public void readFromParcel(Parcel source) {
        pkgName = source.readString();
        cantSwapMem = source.readInt();
        swapPercent = source.readInt();
        killReason = source.readString();
    }
}
