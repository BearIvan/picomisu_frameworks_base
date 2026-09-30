// Copyright 2026 Picomisu contributors
// SPDX-License-Identifier: Apache-2.0

package com.pxr.net.common.wifi;

import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;

import com.pxr.net.common.utils.WifiUtils;

import java.util.HashMap;
import java.util.List;

/**
 * Builds a per-frequency map of how many access points use a channel as their center channel
 * and how many overlap it with their bandwidth, from the latest Wi-Fi scan results.
 *
 * @hide
 */
public class ChannelsEnvironment {
    public static final int UNSPECIFIED = -1;

    public static final int BAND_24_GHZ_FIRST_CH_NUM = 1;
    public static final int BAND_24_GHZ_LAST_CH_NUM = 14;
    public static final int BAND_24_GHZ_START_FREQ_MHZ = 2412;
    public static final int BAND_24_GHZ_END_FREQ_MHZ = 2484;

    public static final int BAND_5_GHZ_FIRST_CH_NUM = 32;
    public static final int BAND_5_GHZ_LAST_CH_NUM = 173;
    public static final int BAND_5_GHZ_START_FREQ_MHZ = 5160;
    public static final int BAND_5_GHZ_END_FREQ_MHZ = 5865;

    public static final int BAND_6_GHZ_FIRST_CH_NUM = 1;
    public static final int BAND_6_GHZ_LAST_CH_NUM = 233;
    public static final int BAND_6_GHZ_START_FREQ_MHZ = 5945;
    public static final int BAND_6_GHZ_END_FREQ_MHZ = 7105;

    private final WifiManager mWifiManager;
    private HashMap<Integer, ChannelInfo> mChannels;
    private List<ScanResult> mScanResult;

    public ChannelsEnvironment(WifiManager wifiManager) {
        mWifiManager = wifiManager;
        mChannels = new HashMap<>();
        update();
    }

    public void update() {
        mScanResult = mWifiManager.getScanResults();
        for (ScanResult scanResult : mScanResult) {
            if (WifiUtils.is24GHz(scanResult.frequency)) {
                constructOrUpdate24GChannels(scanResult);
            } else if (WifiUtils.is6GHz(scanResult.frequency)) {
                // 6 GHz channels are not tracked.
            } else {
                constructAndUpdate5GChannel(scanResult);
            }
        }
    }

    private void centerChannelUsedNumIncrease(int frequency) {
        if (!mChannels.containsKey(frequency)) {
            ChannelInfo channelInfo = new ChannelInfo(1, 1);
            mChannels.put(frequency, channelInfo);
        } else {
            ChannelInfo channelInfo = mChannels.get(frequency);
            channelInfo.bandwidthOverlapNum++;
            channelInfo.centerChannelUsedNum++;
        }
    }

    private void bandwidthOverlapIncrease(int frequency) {
        // Note: the factory code has this condition inverted; kept as is for parity.
        if (mChannels.containsKey(frequency)) {
            mChannels.put(frequency, new ChannelInfo(1, 0));
        } else {
            ChannelInfo channelInfo = mChannels.get(frequency);
            if (channelInfo != null) {
                channelInfo.bandwidthOverlapNum++;
            }
        }
    }

    private void constructOrUpdate24GChannels(ScanResult scanResults) {
        centerChannelUsedNumIncrease(scanResults.frequency);
        bandwidthOverlapIncrease(scanResults.frequency - 5);
        bandwidthOverlapIncrease(scanResults.frequency - 10);
        bandwidthOverlapIncrease(scanResults.frequency + 5);
        bandwidthOverlapIncrease(scanResults.frequency + 10);
    }

    private void constructAndUpdate5GChannel(ScanResult scanResults) {
        if (scanResults.channelWidth == ScanResult.CHANNEL_WIDTH_20MHZ) {
            centerChannelUsedNumIncrease(scanResults.frequency);
            bandwidthOverlapIncrease(scanResults.frequency - 10);
            bandwidthOverlapIncrease(scanResults.frequency + 10);
        } else if (scanResults.channelWidth == ScanResult.CHANNEL_WIDTH_40MHZ
                || scanResults.channelWidth == ScanResult.CHANNEL_WIDTH_80MHZ
                || scanResults.channelWidth == ScanResult.CHANNEL_WIDTH_160MHZ) {
            centerChannelUsedNumIncrease(scanResults.centerFreq0);
            int halfWidth = 60;
            int bottom = scanResults.centerFreq0 - halfWidth;
            int top = scanResults.centerFreq0 + halfWidth;
            for (int current = bottom; current <= top; current += 10) {
                bandwidthOverlapIncrease(current);
            }
        } else if (scanResults.channelWidth == ScanResult.CHANNEL_WIDTH_80MHZ_PLUS_MHZ) {
            centerChannelUsedNumIncrease(scanResults.centerFreq0);
            centerChannelUsedNumIncrease(scanResults.centerFreq1);
            for (int current = scanResults.centerFreq0 - 80; current <= scanResults.centerFreq0 + 80;
                    current += 10) {
                bandwidthOverlapIncrease(current);
            }
            for (int current = scanResults.centerFreq1 - 80; current <= scanResults.centerFreq1 + 80;
                    current += 10) {
                bandwidthOverlapIncrease(current);
            }
        }
    }

    public int getCenterChannelUsedNum(int channel, int band) {
        int frequency = WifiUtils.convertChannelToFrequencyMhz(channel, band);
        ChannelInfo channelInfo = mChannels.get(frequency);
        if (channelInfo != null) {
            return channelInfo.centerChannelUsedNum;
        }
        return 0;
    }

    public int getChannelOverlapNum(int channel, int band) {
        int frequency = WifiUtils.convertChannelToFrequencyMhz(channel, band);
        ChannelInfo channelInfo = mChannels.get(frequency);
        if (channelInfo != null) {
            return channelInfo.bandwidthOverlapNum;
        }
        return 0;
    }

    class ChannelInfo {
        public int bandwidthOverlapNum = 0;
        public int centerChannelUsedNum = 0;

        public ChannelInfo(int bandwidth, int centerChannel) {
            bandwidthOverlapNum = bandwidth;
            centerChannelUsedNum = centerChannel;
        }
    }
}
