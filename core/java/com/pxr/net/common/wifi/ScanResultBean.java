// Copyright 2026 Picomisu contributors
// SPDX-License-Identifier: Apache-2.0

package com.pxr.net.common.wifi;

import android.net.wifi.ScanResult;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.util.Log;

import com.pxr.net.common.log.LogUtil;

import java.util.List;

/**
 * Snapshot of the scan results around the currently connected access point: its channel
 * width, center frequencies and how many other access points use the same frequencies.
 *
 * @hide
 */
public class ScanResultBean {
    private static final String TAG = "ScanResultBean";

    private WifiManager mWifiManager;
    private List<ScanResult> mScanResults;
    private WifiInfo mWifiInfo;
    private String mBssid;
    private int mChannelWidth;
    private boolean mIsDoubleFrequency;
    private int mFirstFrequency;
    private int mSecondFrequency;
    private int mUsedFirstFrequencyApNum = Integer.MIN_VALUE;
    private int mUsedSecondFrequencyApNum = Integer.MIN_VALUE;

    public ScanResultBean(WifiManager wifiManager) {
        mWifiManager = wifiManager;
        if (mWifiManager.isWifiEnabled()) {
            mWifiManager.startScan();
        }

        update();
    }

    public ScanResultBean(List<ScanResult> wifiScans, WifiInfo wifiInfo) {
        mScanResults = wifiScans;
        mWifiInfo = wifiInfo;
    }

    private void constructFrequencyInfo(ScanResult scanResult) {
        if (mChannelWidth == ScanResult.CHANNEL_WIDTH_80MHZ_PLUS_MHZ) {
            mIsDoubleFrequency = true;
            mFirstFrequency = scanResult.centerFreq0;
            mSecondFrequency = scanResult.centerFreq1;
        }
    }

    private void countUsedFrequencyAp() {
        mUsedFirstFrequencyApNum = 0;
        if (mIsDoubleFrequency) {
            mUsedSecondFrequencyApNum = 0;
        }
        for (ScanResult scanResult : mScanResults) {
            // Only access points centered on exactly the same frequency are counted.
            if (scanResult.frequency == mFirstFrequency) {
                mUsedFirstFrequencyApNum++;
            }
            if (mIsDoubleFrequency && scanResult.frequency == mSecondFrequency) {
                mUsedSecondFrequencyApNum++;
            }
        }
    }

    public boolean startScan() {
        if (mWifiManager != null) {
            return mWifiManager.startScan();
        }
        return false;
    }

    public List<ScanResult> getLastScanResults() {
        return mScanResults;
    }

    public void update() {
        mScanResults = mWifiManager.getScanResults();
        mWifiInfo = mWifiManager.getConnectionInfo();
        if (mScanResults == null || mWifiInfo == null) {
            return;
        }

        mBssid = mWifiInfo.getBSSID();
        mFirstFrequency = mWifiInfo.getFrequency();
        if (LogUtil.isLoggable(Log.DEBUG)) {
            LogUtil.d(TAG, "mBssid=" + mBssid + " mFirstFrequency=" + mFirstFrequency);
        }
        for (ScanResult scanResult : mScanResults) {
            if (scanResult.BSSID.equals(mBssid)) {
                mChannelWidth = scanResult.channelWidth;
                if (LogUtil.isLoggable(Log.DEBUG)) {
                    LogUtil.d(TAG, "match bssid!, channelWidth:" + mChannelWidth);
                }
                constructFrequencyInfo(scanResult);
                break;
            }
        }
        countUsedFrequencyAp();
    }

    /** Returns the channel bandwidth in MHz (20 * 2^channelWidth). */
    public int getBandwidth() {
        return (int) (Math.pow(2, mChannelWidth) * 20);
    }

    public int getCurrentChannelUsedCount() {
        if (!mIsDoubleFrequency) {
            return mUsedFirstFrequencyApNum;
        }
        return mUsedFirstFrequencyApNum < mUsedSecondFrequencyApNum
                ? mUsedFirstFrequencyApNum : mUsedSecondFrequencyApNum;
    }
}
