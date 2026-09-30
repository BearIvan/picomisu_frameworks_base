// Copyright 2026 Picomisu contributors
// SPDX-License-Identifier: Apache-2.0

package com.pxr.net.common.wifi;

import java.util.List;

/**
 * Callback for Wi-Fi state, connection, scan and score changes.
 *
 * @hide
 */
public interface WifiChangeListener {
    void onScanInfoChanged(List list);

    void onWifiInfoChanged(boolean changed);

    void onWifiScoredChanged(int score);

    void onWifiStateChanged(int state);
}
