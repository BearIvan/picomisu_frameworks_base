// Copyright 2026 Picomisu contributors
// SPDX-License-Identifier: Apache-2.0

package com.pxr.net.common.score;

import com.pxr.net.NetworkPxrAdapter;

/**
 * Placeholder Wi-Fi score card. The factory implementation keeps the thresholds but does not
 * compute a score yet (both getters return -1).
 *
 * @hide
 */
public class WifiScoreCard {
    private static final int RSSI_ENTRY_THRESHOLD_5GHZ = -77;
    private static final int RSSI_LOW_THRESHOLD_5GHZ = -70;
    private static final int RSSI_GOOD_THRESHOLD_5GHZ = -57;
    private static final int LINK_SPEED_GOOD_THRESHOLD = 300;

    private NetworkPxrAdapter mWifiAdapter;
    private int mScore;

    public WifiScoreCard(NetworkPxrAdapter wifiAdapter) {
        mWifiAdapter = wifiAdapter;
    }

    public int calculateScoreLevel() {
        return -1;
    }

    public int getScore() {
        return -1;
    }
}
