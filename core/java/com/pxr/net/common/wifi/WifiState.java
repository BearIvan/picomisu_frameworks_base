// Copyright 2026 Picomisu contributors
// SPDX-License-Identifier: Apache-2.0

package com.pxr.net.common.wifi;

import android.net.NetworkInfo;
import android.net.wifi.WifiManager;

import com.pxr.net.common.log.LogUtil;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Tracks the Wi-Fi radio state (0..3, same values as WifiManager.WIFI_STATE_*) and the
 * connection state (4 connecting, 5 connected, 6 disconnecting, 7 disconnected).
 *
 * @hide
 */
public class WifiState {
    private static final String TAG = "WifiState";

    private AtomicInteger mWifiState = new AtomicInteger(-1);

    public void handleWifiStateChanged(int state) {
        switch (state) {
            case WifiManager.WIFI_STATE_ENABLING:
                mWifiState.set(WifiManager.WIFI_STATE_ENABLING);
                break;
            case WifiManager.WIFI_STATE_ENABLED:
                mWifiState.set(WifiManager.WIFI_STATE_ENABLED);
                break;
            case WifiManager.WIFI_STATE_DISABLING:
                mWifiState.set(WifiManager.WIFI_STATE_DISABLING);
                break;
            case WifiManager.WIFI_STATE_DISABLED:
                mWifiState.set(WifiManager.WIFI_STATE_DISABLED);
                break;
            default:
                LogUtil.e(TAG, "Set an invalid value for wifi state!");
        }
    }

    public void handleConnectStateChanged(NetworkInfo.DetailedState state) {
        switch (state) {
            case CONNECTING:
                mWifiState.set(4);
                break;
            case CONNECTED:
                mWifiState.set(5);
                break;
            case DISCONNECTING:
                mWifiState.set(6);
                break;
            case DISCONNECTED:
                mWifiState.set(7);
                break;
            default:
                LogUtil.e(TAG, "Set an invalid value for wifi connect state!");
        }
    }

    public int getState() {
        return mWifiState.get();
    }

    public boolean isConnected() {
        return mWifiState.get() == 5;
    }
}
