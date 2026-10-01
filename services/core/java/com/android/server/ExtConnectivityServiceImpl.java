// Copyright 2026 Picomisu contributors
// SPDX-License-Identifier: Apache-2.0
package com.android.server;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.WifiManager;
import android.os.Handler;
import android.os.Message;
import android.os.UserHandle;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Slog;

import com.android.server.connectivity.MockableSystemProperties;

/**
 * PICO ConnectivityService extension (factory PICO OS 5.13.7
 * com.android.server.ExtConnectivityServiceImpl).
 *
 * <p>When Wi-Fi is switched off, the network transition wakelock is released 5 s later
 * (EVENT_CLEAR_NET_TRANSITION_WAKELOCK) instead of waiting for its expiry. An empty
 * net.hostname is set to "&lt;pxr.vendorhw.product.model without blanks&gt;_&lt;android_id&gt;"
 * (or "PicoNeo2_&lt;android_id&gt;" without a model).
 */
public class ExtConnectivityServiceImpl implements IExtConnectivityService {
    private static final String TAG = ConnectivityService.class.getSimpleName();
    private static final int WIFI_DISABLED_RELEASE_LOCK_DELAY_MS = 5000;
    private ConnectivityService mBase;
    private Handler mHandler;
    private BroadcastReceiver mWifiStateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            int wifiState = intent.getIntExtra(WifiManager.EXTRA_WIFI_STATE,
                    WifiManager.WIFI_STATE_UNKNOWN);
            if (wifiState == WifiManager.WIFI_STATE_DISABLED) {
                mHandler.removeMessages(ConnectivityService.EVENT_EXPIRE_NET_TRANSITION_WAKELOCK);
                mHandler.removeMessages(ConnectivityService.EVENT_CLEAR_NET_TRANSITION_WAKELOCK);
                Message msg = mHandler.obtainMessage(
                        ConnectivityService.EVENT_CLEAR_NET_TRANSITION_WAKELOCK);
                mHandler.sendMessageDelayed(msg, WIFI_DISABLED_RELEASE_LOCK_DELAY_MS);
                Slog.w(TAG, "wifi disabled, send msg to release lock after "
                        + WIFI_DISABLED_RELEASE_LOCK_DELAY_MS + "ms");
            }
        }
    };

    public ExtConnectivityServiceImpl(ConnectivityService base) {
        mBase = base;
    }

    @Override
    public void init(Context context, Handler handler) {
        mHandler = handler;
        IntentFilter wifiStateIntentFilter = new IntentFilter();
        wifiStateIntentFilter.addAction(WifiManager.WIFI_STATE_CHANGED_ACTION);
        context.registerReceiverAsUser(mWifiStateReceiver, UserHandle.ALL,
                wifiStateIntentFilter, null, mHandler);
    }

    @Override
    public void setupUniqueDeviceName(Context context, MockableSystemProperties systemProperties) {
        String hostname = systemProperties.get("net.hostname");
        if (TextUtils.isEmpty(hostname)) {
            String id = Settings.Secure.getString(context.getContentResolver(),
                    Settings.Secure.ANDROID_ID);
            if (id != null && id.length() > 0) {
                String name = new String("PicoNeo2_").concat(id);
                if (!TextUtils.isEmpty(systemProperties.get("pxr.vendorhw.product.model"))) {
                    String pxr_model = systemProperties.get("pxr.vendorhw.product.model")
                            .replaceAll("\\s*", "") + "_";
                    Slog.w(TAG, "net.hostname pxr_model is " + pxr_model);
                    if (pxr_model != null) {
                        name = pxr_model.concat(id);
                    }
                }
                systemProperties.set("net.hostname", name);
            }
        }
    }
}
