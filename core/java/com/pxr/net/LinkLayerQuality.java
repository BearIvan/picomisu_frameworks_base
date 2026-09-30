// Copyright 2026 Picomisu contributors
// SPDX-License-Identifier: Apache-2.0

package com.pxr.net;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Wi-Fi link layer quality evaluation (deltas of link layer statistics and derived quality
 * levels) produced by the PICO network service.
 *
 * @hide
 */
public class LinkLayerQuality implements Parcelable {
    private static final String TAG = "LinkLayerQuality";

    private int deltaCcaBusyTime;
    private int deltaRadioOnTime;
    private int ccaLevel;
    private long deltaTxSuccess;
    private long deltaTxRetries;
    private int txRetranLevel;
    private long deltaTxBad;
    private int txBadLevel;
    private int deltaRxSuccess;
    private int deltaRxFcsErr;
    private int rxFcsErrLevel;
    private int totalTxBytes;
    private int deltaTxBytes;
    private int txRealSpeed;
    private int totalRxBytes;
    private int deltaRxBytes;
    private int rxRealSpeed;
    private int txMode;
    private int txMcs;
    private int txBw;
    private int rxMode;
    private int rxMcs;
    private int rxBw;
    private int currentChannelAPCount;
    private int arpNeighbourCount;
    private int contention_atime_be;
    private int contention_atime_bk;
    private int contention_atime_vi;
    private int contention_atime_vo;
    private long totalTxMpduBe;
    private long totalTxRetriesBe;
    private int totalTxRetranLevelBe;
    private long totalTxMpduVi;
    private long totalTxRetriesVi;
    private int totalTxRetranLevelVi;
    private long totalTxMpduVo;
    private long totalTxRetriesVo;
    private int totalTxRetranLevelVo;
    private int linklayerLevel;
    private int snr;

    public LinkLayerQuality() {
    }

    /** Copy constructor; note that the factory implementation does not copy snr. */
    public LinkLayerQuality(LinkLayerQuality source) {
        if (source == null) {
            return;
        }
        deltaCcaBusyTime = source.deltaCcaBusyTime;
        deltaRadioOnTime = source.deltaRadioOnTime;
        ccaLevel = source.ccaLevel;
        deltaTxSuccess = source.deltaTxSuccess;
        deltaTxRetries = source.deltaTxRetries;
        txRetranLevel = source.txRetranLevel;
        deltaTxBad = source.deltaTxBad;
        txBadLevel = source.txBadLevel;
        deltaRxSuccess = source.deltaRxSuccess;
        deltaRxFcsErr = source.deltaRxFcsErr;
        rxFcsErrLevel = source.rxFcsErrLevel;
        totalTxBytes = source.totalTxBytes;
        deltaTxBytes = source.deltaTxBytes;
        txRealSpeed = source.txRealSpeed;
        totalRxBytes = source.totalRxBytes;
        deltaRxBytes = source.deltaRxBytes;
        rxRealSpeed = source.rxRealSpeed;
        txMode = source.txMode;
        txMcs = source.txMcs;
        txBw = source.txBw;
        rxMode = source.rxMode;
        rxMcs = source.rxMcs;
        rxBw = source.rxBw;
        currentChannelAPCount = source.currentChannelAPCount;
        arpNeighbourCount = source.arpNeighbourCount;
        contention_atime_be = source.contention_atime_be;
        contention_atime_bk = source.contention_atime_bk;
        contention_atime_vi = source.contention_atime_vi;
        contention_atime_vo = source.contention_atime_vo;
        totalTxMpduBe = source.totalTxMpduBe;
        totalTxRetriesBe = source.totalTxRetriesBe;
        totalTxRetranLevelBe = source.totalTxRetranLevelBe;
        totalTxMpduVi = source.totalTxMpduVi;
        totalTxRetriesVi = source.totalTxRetriesVi;
        totalTxRetranLevelVi = source.totalTxRetranLevelVi;
        totalTxMpduVo = source.totalTxMpduVo;
        totalTxRetriesVo = source.totalTxRetriesVo;
        totalTxRetranLevelVo = source.totalTxRetranLevelVo;
        linklayerLevel = source.linklayerLevel;
    }

    public int getDeltaCcaBusyTime() {
        return deltaCcaBusyTime;
    }

    public void setDeltaCcaBusyTime(int deltaCcaBusyTime) {
        this.deltaCcaBusyTime = deltaCcaBusyTime;
    }

    public int getDeltaRadioOnTime() {
        return deltaRadioOnTime;
    }

    public void setDeltaRadioOnTime(int deltaRadioOnTime) {
        this.deltaRadioOnTime = deltaRadioOnTime;
    }

    public int getCcaLevel() {
        return ccaLevel;
    }

    public void setCcaLevel(int ccaLevel) {
        this.ccaLevel = ccaLevel;
    }

    public long getDeltaTxSuccess() {
        return deltaTxSuccess;
    }

    public void setDeltaTxSuccess(long deltaTxSuccess) {
        this.deltaTxSuccess = deltaTxSuccess;
    }

    public long getDeltaTxRetries() {
        return deltaTxRetries;
    }

    public void setDeltaTxRetries(long deltaTxRetries) {
        this.deltaTxRetries = deltaTxRetries;
    }

    public int getTxRetranLevel() {
        return txRetranLevel;
    }

    public void setTxRetranLevel(int txRetranLevel) {
        this.txRetranLevel = txRetranLevel;
    }

    public long getDeltaTxBad() {
        return deltaTxBad;
    }

    public void setDeltaTxBad(long deltaTxBad) {
        this.deltaTxBad = deltaTxBad;
    }

    public int getTxBadLevel() {
        return txBadLevel;
    }

    public void setTxBadLevel(int txBadLevel) {
        this.txBadLevel = txBadLevel;
    }

    public int getDeltaRxSuccess() {
        return deltaRxSuccess;
    }

    public void setDeltaRxSuccess(int deltaRxSuccess) {
        this.deltaRxSuccess = deltaRxSuccess;
    }

    public int getDeltaRxFcsErr() {
        return deltaRxFcsErr;
    }

    public void setDeltaRxFcsErr(int deltaRxFcsErr) {
        this.deltaRxFcsErr = deltaRxFcsErr;
    }

    public int getRxFcsErrLevel() {
        return rxFcsErrLevel;
    }

    public void setRxFcsErrLevel(int rxFcsErrLevel) {
        this.rxFcsErrLevel = rxFcsErrLevel;
    }

    public int getTotalTxBytes() {
        return totalTxBytes;
    }

    public void setTotalTxBytes(int totalTxBytes) {
        this.totalTxBytes = totalTxBytes;
    }

    public int getDeltaTxBytes() {
        return deltaTxBytes;
    }

    public void setDeltaTxBytes(int deltaTxBytes) {
        this.deltaTxBytes = deltaTxBytes;
    }

    public int getTxRealSpeed() {
        return txRealSpeed;
    }

    public void setTxRealSpeed(int txRealSpeed) {
        this.txRealSpeed = txRealSpeed;
    }

    public int getTotalRxBytes() {
        return totalRxBytes;
    }

    public void setTotalRxBytes(int totalRxBytes) {
        this.totalRxBytes = totalRxBytes;
    }

    public int getDeltaRxBytes() {
        return deltaRxBytes;
    }

    public void setDeltaRxBytes(int deltaRxBytes) {
        this.deltaRxBytes = deltaRxBytes;
    }

    public int getRxRealSpeed() {
        return rxRealSpeed;
    }

    public void setRxRealSpeed(int rxRealSpeed) {
        this.rxRealSpeed = rxRealSpeed;
    }

    public int getTxMode() {
        return txMode;
    }

    public void setTxMode(int txMode) {
        this.txMode = txMode;
    }

    public int getTxMcs() {
        return txMcs;
    }

    public void setTxMcs(int txMcs) {
        this.txMcs = txMcs;
    }

    public int getTxBw() {
        return txBw;
    }

    public void setTxBw(int txBw) {
        this.txBw = txBw;
    }

    public int getRxMode() {
        return rxMode;
    }

    public void setRxMode(int rxMode) {
        this.rxMode = rxMode;
    }

    public int getRxMcs() {
        return rxMcs;
    }

    public void setRxMcs(int rxMcs) {
        this.rxMcs = rxMcs;
    }

    public int getRxBw() {
        return rxBw;
    }

    public void setRxBw(int rxBw) {
        this.rxBw = rxBw;
    }

    public int getCurrentChannelAPCount() {
        return currentChannelAPCount;
    }

    public void setCurrentChannelAPCount(int currentChannelAPCount) {
        this.currentChannelAPCount = currentChannelAPCount;
    }

    public int getArpNeighbourCount() {
        return arpNeighbourCount;
    }

    public void setArpNeighbourCount(int arpNeighbourCount) {
        this.arpNeighbourCount = arpNeighbourCount;
    }

    public int getContentionAtimeBe() {
        return contention_atime_be;
    }

    public void setContentionAtimeBe(int contention_atime_be) {
        this.contention_atime_be = contention_atime_be;
    }

    public int getContentionAtimeBk() {
        return contention_atime_bk;
    }

    public void setContentionAtimeBk(int contention_atime_bk) {
        this.contention_atime_bk = contention_atime_bk;
    }

    public int getContentionAtimeVi() {
        return contention_atime_vi;
    }

    public void setContentionAtimeVi(int contention_atime_vi) {
        this.contention_atime_vi = contention_atime_vi;
    }

    public int getContentionAtimeVo() {
        return contention_atime_vo;
    }

    public void setContentionAtimeVo(int contention_atime_vo) {
        this.contention_atime_vo = contention_atime_vo;
    }

    public long getTotalTxMpduBe() {
        return totalTxMpduBe;
    }

    public void setTotalTxMpduBe(long totalTxMpduBe) {
        this.totalTxMpduBe = totalTxMpduBe;
    }

    public long getTotalTxRetriesBe() {
        return totalTxRetriesBe;
    }

    public void setTotalTxRetriesBe(long totalTxRetriesBe) {
        this.totalTxRetriesBe = totalTxRetriesBe;
    }

    public int getTotalTxRetranLevelBe() {
        return totalTxRetranLevelBe;
    }

    public void setTotalTxRetranLevelBe(int totalTxRetranLevelBe) {
        this.totalTxRetranLevelBe = totalTxRetranLevelBe;
    }

    public long getTotalTxMpduVi() {
        return totalTxMpduVi;
    }

    public void setTotalTxMpduVi(long totalTxMpduVi) {
        this.totalTxMpduVi = totalTxMpduVi;
    }

    public long getTotalTxRetriesVi() {
        return totalTxRetriesVi;
    }

    public void setTotalTxRetriesVi(long totalTxRetriesVi) {
        this.totalTxRetriesVi = totalTxRetriesVi;
    }

    public int getTotalTxRetranLevelVi() {
        return totalTxRetranLevelVi;
    }

    public void setTotalTxRetranLevelVi(int totalTxRetranLevelVi) {
        this.totalTxRetranLevelVi = totalTxRetranLevelVi;
    }

    public long getTotalTxMpduVo() {
        return totalTxMpduVo;
    }

    public void setTotalTxMpduVo(long totalTxMpduVo) {
        this.totalTxMpduVo = totalTxMpduVo;
    }

    public long getTotalTxRetriesVo() {
        return totalTxRetriesVo;
    }

    public void setTotalTxRetriesVo(long totalTxRetriesVo) {
        this.totalTxRetriesVo = totalTxRetriesVo;
    }

    public int getTotalTxRetranLevelVo() {
        return totalTxRetranLevelVo;
    }

    public void setTotalTxRetranLevelVo(int totalTxRetranLevelVo) {
        this.totalTxRetranLevelVo = totalTxRetranLevelVo;
    }

    public int getLinkLayerLevel() {
        return linklayerLevel;
    }

    public void setLinkLayerLevel(int linklayerLevel) {
        this.linklayerLevel = linklayerLevel;
    }

    public int getSNR() {
        return snr;
    }

    public void setSNR(int snr) {
        this.snr = snr;
    }

    @Override
    public String toString() {
        StringBuilder sbuf = new StringBuilder();
        sbuf.append(" LinkLayerQuality: ");
        sbuf.append(" deltaCcaBusyTime: ").append(Integer.toString(this.deltaCcaBusyTime));
        sbuf.append(" deltaRadioOnTime: ").append(Integer.toString(this.deltaRadioOnTime));
        sbuf.append(" ccaLevel: ").append(Integer.toString(this.ccaLevel));
        sbuf.append(" deltaTxSuccess: ").append(Long.toString(this.deltaTxSuccess));
        sbuf.append(" deltaTxRetries: ").append(Long.toString(this.deltaTxRetries));
        sbuf.append(" txRetranLevel: ").append(Integer.toString(this.txRetranLevel));
        sbuf.append(" deltaTxBad: ").append(Long.toString(this.deltaTxBad));
        sbuf.append(" txBadLevel: ").append(Integer.toString(this.txBadLevel));
        sbuf.append(" deltaRxSuccess: ").append(Integer.toString(this.deltaRxSuccess));
        sbuf.append(" deltaRxFcsErr: ").append(Integer.toString(this.deltaRxFcsErr));
        sbuf.append(" rxFcsErrLevel: ").append(Integer.toString(this.rxFcsErrLevel));
        sbuf.append(" deltaTxBytes: ").append(Integer.toString(this.deltaTxBytes));
        sbuf.append(" txRealSpeed: ").append(Integer.toString(this.txRealSpeed));
        sbuf.append(" deltaRxBytes: ").append(Integer.toString(this.deltaRxBytes));
        sbuf.append(" rxRealSpeed: ").append(Integer.toString(this.rxRealSpeed));
        sbuf.append(" txMode: ").append(Integer.toString(this.txMode));
        sbuf.append(" txMcs: ").append(Integer.toString(this.txMcs));
        sbuf.append(" txBw: ").append(Integer.toString(this.txBw));
        sbuf.append(" rxMode: ").append(Integer.toString(this.rxMode));
        sbuf.append(" rxMcs: ").append(Integer.toString(this.rxMcs));
        sbuf.append(" rxBw: ").append(Integer.toString(this.rxBw));
        sbuf.append(" currentChannelAPCount: ").append(Integer.toString(this.currentChannelAPCount));
        sbuf.append(" arpNeighbourCount: ").append(Integer.toString(this.arpNeighbourCount));
        sbuf.append(" contention_atime_be: ").append(Integer.toString(this.contention_atime_be));
        sbuf.append(" contention_atime_vi: ").append(Integer.toString(this.contention_atime_vi));
        sbuf.append(" contention_atime_vo: ").append(Integer.toString(this.contention_atime_vo));
        sbuf.append(" totalTxMpduBe: ").append(Long.toString(this.totalTxMpduBe));
        sbuf.append(" totalTxRetranLevelBe: ").append(Integer.toString(this.totalTxRetranLevelBe));
        sbuf.append(" totalTxMpduVi: ").append(Long.toString(this.totalTxMpduVi));
        sbuf.append(" totalTxRetranLevelVi: ").append(Integer.toString(this.totalTxRetranLevelVi));
        sbuf.append(" totalTxMpduVo: ").append(Long.toString(this.totalTxMpduVo));
        sbuf.append(" totalTxRetranLevelVo: ").append(Integer.toString(this.totalTxRetranLevelVo));
        sbuf.append(" linklayerLevel: ").append(Integer.toString(this.linklayerLevel));
        sbuf.append(" snr: ").append(Integer.toString(this.snr));
        return sbuf.toString();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(deltaCcaBusyTime);
        dest.writeInt(deltaRadioOnTime);
        dest.writeInt(ccaLevel);
        dest.writeLong(deltaTxSuccess);
        dest.writeLong(deltaTxRetries);
        dest.writeInt(txRetranLevel);
        dest.writeLong(deltaTxBad);
        dest.writeInt(txBadLevel);
        dest.writeInt(deltaRxSuccess);
        dest.writeInt(deltaRxFcsErr);
        dest.writeInt(rxFcsErrLevel);
        dest.writeInt(totalTxBytes);
        dest.writeInt(deltaTxBytes);
        dest.writeInt(txRealSpeed);
        dest.writeInt(totalRxBytes);
        dest.writeInt(deltaRxBytes);
        dest.writeInt(rxRealSpeed);
        dest.writeInt(txMode);
        dest.writeInt(txMcs);
        dest.writeInt(txBw);
        dest.writeInt(rxMode);
        dest.writeInt(rxMcs);
        dest.writeInt(rxBw);
        dest.writeInt(currentChannelAPCount);
        dest.writeInt(arpNeighbourCount);
        dest.writeInt(contention_atime_be);
        dest.writeInt(contention_atime_bk);
        dest.writeInt(contention_atime_vi);
        dest.writeInt(contention_atime_vo);
        dest.writeLong(totalTxMpduBe);
        dest.writeLong(totalTxRetriesBe);
        dest.writeInt(totalTxRetranLevelBe);
        dest.writeLong(totalTxMpduVi);
        dest.writeLong(totalTxRetriesVi);
        dest.writeInt(totalTxRetranLevelVi);
        dest.writeLong(totalTxMpduVo);
        dest.writeLong(totalTxRetriesVo);
        dest.writeInt(totalTxRetranLevelVo);
        dest.writeInt(linklayerLevel);
        dest.writeInt(snr);
    }

    public static final Parcelable.Creator<LinkLayerQuality> CREATOR =
            new Parcelable.Creator<LinkLayerQuality>() {
        public LinkLayerQuality createFromParcel(Parcel in) {
            LinkLayerQuality llquality = new LinkLayerQuality();
            llquality.deltaCcaBusyTime = in.readInt();
            llquality.deltaRadioOnTime = in.readInt();
            llquality.ccaLevel = in.readInt();
            llquality.deltaTxSuccess = in.readLong();
            llquality.deltaTxRetries = in.readLong();
            llquality.txRetranLevel = in.readInt();
            llquality.deltaTxBad = in.readLong();
            llquality.txBadLevel = in.readInt();
            llquality.deltaRxSuccess = in.readInt();
            llquality.deltaRxFcsErr = in.readInt();
            llquality.rxFcsErrLevel = in.readInt();
            llquality.totalTxBytes = in.readInt();
            llquality.deltaTxBytes = in.readInt();
            llquality.txRealSpeed = in.readInt();
            llquality.totalRxBytes = in.readInt();
            llquality.deltaRxBytes = in.readInt();
            llquality.rxRealSpeed = in.readInt();
            llquality.txMode = in.readInt();
            llquality.txMcs = in.readInt();
            llquality.txBw = in.readInt();
            llquality.rxMode = in.readInt();
            llquality.rxMcs = in.readInt();
            llquality.rxBw = in.readInt();
            llquality.currentChannelAPCount = in.readInt();
            llquality.arpNeighbourCount = in.readInt();
            llquality.contention_atime_be = in.readInt();
            llquality.contention_atime_bk = in.readInt();
            llquality.contention_atime_vi = in.readInt();
            llquality.contention_atime_vo = in.readInt();
            llquality.totalTxMpduBe = in.readLong();
            llquality.totalTxRetriesBe = in.readLong();
            llquality.totalTxRetranLevelBe = in.readInt();
            llquality.totalTxMpduVi = in.readLong();
            llquality.totalTxRetriesVi = in.readLong();
            llquality.totalTxRetranLevelVi = in.readInt();
            llquality.totalTxMpduVo = in.readLong();
            llquality.totalTxRetriesVo = in.readLong();
            llquality.totalTxRetranLevelVo = in.readInt();
            llquality.linklayerLevel = in.readInt();
            llquality.snr = in.readInt();
            return llquality;
        }

        public LinkLayerQuality[] newArray(int size) {
            return new LinkLayerQuality[size];
        }
    };
}
