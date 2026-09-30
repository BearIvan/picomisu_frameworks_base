// Copyright 2026 Picomisu contributors
// SPDX-License-Identifier: Apache-2.0

package com.pxr.net.common.wifi;

import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;

import com.pxr.net.common.log.LogUtil;
import com.pxr.net.common.utils.WifiUtils;

/**
 * Cached view of the current Wi-Fi connection (BSSID, SSID, frequency, RSSI, link speeds).
 *
 * @hide
 */
public class WifiInfoBean {
    private static final String TAG = "WifiInfoBean";

    private final WifiManager mWifiManager;
    private String mBssid;
    private String mSsid;
    private int mFrequency;
    private int mRssi;
    private int mChannel;
    private int mRxLinkSpeed;
    private int mTxLinkSpeed;
    private Integer mStandard;

    public WifiInfoBean(WifiManager wifiManager) {
        mWifiManager = wifiManager;
        update();
    }

    public WifiInfoBean update() {
        WifiInfo wifiInfo = mWifiManager.getConnectionInfo();
        LogUtil.d(TAG, "get WifiInfo:" + wifiInfo);
        mBssid = wifiInfo.getBSSID();
        mSsid = wifiInfo.getSSID();
        mFrequency = wifiInfo.getFrequency();
        mRssi = wifiInfo.getRssi();
        mChannel = WifiUtils.convertFrequencyMhzToChannel(mFrequency);
        mRxLinkSpeed = wifiInfo.getRxLinkSpeedMbps();
        mTxLinkSpeed = wifiInfo.getTxLinkSpeedMbps();
        mStandard = wifiInfo.getWifiGeneration();

        return this;
    }

    public String getBssid() {
        return mBssid;
    }

    public String getSsid() {
        return mSsid;
    }

    public int getFrequency() {
        return mFrequency;
    }

    public int getRssi() {
        update();
        return mRssi;
    }

    public int getChannel() {
        return mChannel;
    }

    public boolean is24GHz() {
        return WifiUtils.is24GHz(mFrequency);
    }

    public boolean is5GHz() {
        return WifiUtils.is5GHz(mFrequency);
    }

    public boolean is6GHz() {
        return WifiUtils.is6GHz(mFrequency);
    }

    public int getTxLinkSpeedMbps() {
        return mTxLinkSpeed;
    }

    public int getRxLinkSpeedMbps() {
        return mRxLinkSpeed;
    }

    public int getWifiStandard() {
        return mStandard;
    }

    public boolean equals(WifiInfoBean wifiInfoBean) {
        if (wifiInfoBean == null) {
            return false;
        }
        if (mBssid == null || !mBssid.equals(wifiInfoBean.mBssid)) {
            return false;
        } else if (mSsid == null || !mSsid.equals(wifiInfoBean.mSsid)) {
            return false;
        } else if (mFrequency != wifiInfoBean.mFrequency) {
            return false;
        } else if (mRssi != wifiInfoBean.mRssi) {
            return false;
        } else if (mChannel != wifiInfoBean.mChannel) {
            return false;
        } else if (mRxLinkSpeed != wifiInfoBean.mRxLinkSpeed) {
            return false;
        } else if (mTxLinkSpeed != wifiInfoBean.mTxLinkSpeed) {
            return false;
        }

        return true;
    }

    @Override
    public String toString() {
        return "mBssid=" + mBssid + ", mSsid=" + mSsid + ", mFrequency=" + mFrequency
                + ", mRss=" + mRssi + ", mChannel=" + mChannel + ", mRxLinkSpeed=" + mRxLinkSpeed
                + ", mTxLinkSpeed=" + mTxLinkSpeed;
    }
}
