// Copyright 2026 Picomisu contributors
// SPDX-License-Identifier: Apache-2.0

package com.pxr.net.common.utils;

import com.pxr.net.common.wifi.ChannelsEnvironment;

/**
 * Wi-Fi channel/frequency conversion helpers.
 *
 * @hide
 */
public class WifiUtils {

    public static int convertFrequencyMhzToChannel(int freqMhz) {
        if (freqMhz == ChannelsEnvironment.BAND_24_GHZ_END_FREQ_MHZ) {
            return ChannelsEnvironment.BAND_24_GHZ_LAST_CH_NUM;
        } else if (is24GHz(freqMhz)) {
            return (freqMhz - ChannelsEnvironment.BAND_24_GHZ_START_FREQ_MHZ) / 5
                    + ChannelsEnvironment.BAND_24_GHZ_FIRST_CH_NUM;
        } else if (is5GHz(freqMhz)) {
            return (freqMhz - ChannelsEnvironment.BAND_5_GHZ_START_FREQ_MHZ) / 5
                    + ChannelsEnvironment.BAND_5_GHZ_FIRST_CH_NUM;
        } else if (is6GHz(freqMhz)) {
            return (freqMhz - ChannelsEnvironment.BAND_6_GHZ_START_FREQ_MHZ) / 5
                    + ChannelsEnvironment.BAND_6_GHZ_FIRST_CH_NUM;
        }

        return ChannelsEnvironment.UNSPECIFIED;
    }

    /**
     * Converts a channel number of the given band (1: 2.4 GHz, 2: 5 GHz, 8: 6 GHz) into its
     * center frequency in MHz, or UNSPECIFIED (-1) if it is not valid.
     */
    public static int convertChannelToFrequencyMhz(int channel, int band) {
        if (band == 1) {
            if (channel == ChannelsEnvironment.BAND_24_GHZ_LAST_CH_NUM) {
                return ChannelsEnvironment.BAND_24_GHZ_END_FREQ_MHZ;
            } else if (channel >= ChannelsEnvironment.BAND_24_GHZ_FIRST_CH_NUM
                    && channel <= ChannelsEnvironment.BAND_24_GHZ_LAST_CH_NUM) {
                return ((channel - ChannelsEnvironment.BAND_24_GHZ_FIRST_CH_NUM) * 5)
                        + ChannelsEnvironment.BAND_24_GHZ_START_FREQ_MHZ;
            } else {
                return ChannelsEnvironment.UNSPECIFIED;
            }
        }
        if (band == 2) {
            if (channel >= ChannelsEnvironment.BAND_5_GHZ_FIRST_CH_NUM
                    && channel <= ChannelsEnvironment.BAND_5_GHZ_LAST_CH_NUM) {
                return ((channel - ChannelsEnvironment.BAND_5_GHZ_FIRST_CH_NUM) * 5)
                        + ChannelsEnvironment.BAND_5_GHZ_START_FREQ_MHZ;
            } else {
                return ChannelsEnvironment.UNSPECIFIED;
            }
        }
        if (band == 8) {
            if (channel >= ChannelsEnvironment.BAND_6_GHZ_FIRST_CH_NUM
                    && channel <= ChannelsEnvironment.BAND_6_GHZ_LAST_CH_NUM) {
                return ((channel - ChannelsEnvironment.BAND_6_GHZ_FIRST_CH_NUM) * 5)
                        + ChannelsEnvironment.BAND_6_GHZ_START_FREQ_MHZ;
            } else {
                return ChannelsEnvironment.UNSPECIFIED;
            }
        }
        return ChannelsEnvironment.UNSPECIFIED;
    }

    public static boolean is24GHz(int freqMhz) {
        return freqMhz >= ChannelsEnvironment.BAND_24_GHZ_START_FREQ_MHZ
                && freqMhz <= ChannelsEnvironment.BAND_24_GHZ_END_FREQ_MHZ;
    }

    public static boolean is5GHz(int freqMhz) {
        return freqMhz >= ChannelsEnvironment.BAND_5_GHZ_START_FREQ_MHZ
                && freqMhz <= ChannelsEnvironment.BAND_5_GHZ_END_FREQ_MHZ;
    }

    public static boolean is6GHz(int freqMhz) {
        return freqMhz >= ChannelsEnvironment.BAND_6_GHZ_START_FREQ_MHZ
                && freqMhz <= ChannelsEnvironment.BAND_6_GHZ_END_FREQ_MHZ;
    }
}
