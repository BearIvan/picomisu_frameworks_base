// Copyright 2026 Picomisu contributors
// SPDX-License-Identifier: Apache-2.0

package com.pxr.net;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.DnsResolver;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.IBinder;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemClock;
import android.text.TextUtils;
import android.util.Log;

import com.pxr.net.common.log.LogUtil;
import com.pxr.net.common.score.WifiScoreCard;
import com.pxr.net.common.utils.WifiUtils;
import com.pxr.net.common.wifi.ChannelsEnvironment;
import com.pxr.net.common.wifi.ScanResultBean;
import com.pxr.net.common.wifi.WifiChangeListener;
import com.pxr.net.common.wifi.WifiInfoBean;
import com.pxr.net.common.wifi.WifiState;
import com.pxr.net.util.DnsUtils;
import com.pxr.net.util.Stopwatch;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Client side adapter of the PICO network service ("pxr_net"): exposes Wi-Fi state and link
 * information, network/link layer quality listeners and HTTPS/DNS latency probes.
 *
 * @hide
 */
public class NetworkPxrAdapter {
    private static final String TAG = "NetworkPxrAdapter";
    private static final String SERVER_NAME = "pxr_net";
    private static final int RETRY_GET_SERVER_COUNT = 50;
    private static final int GET_SERVER_INTERVAL_MS = 100;

    public static final int INT_ERROR = -1;

    public static final int STATE_TURNING_OFF = 0;
    public static final int STATE_OFF = 1;
    public static final int STATE_TURNING_ON = 2;
    public static final int STATE_ON = 3;
    public static final int STATE_CONNECTING = 4;
    public static final int STATE_CONNECTED = 5;
    public static final int STATE_DISCONNECTING = 6;
    public static final int STATE_DISCONNECTED = 7;

    public static final int WIFI_BAND_24_GHZ = 1;
    public static final int WIFI_BAND_5_GHZ = 2;
    public static final int WIFI_BAND_6_GHZ = 8;

    public static final int PROBE_TYPE_HTTPS = 1;
    public static final int PROBE_TYPE_DNS = 2;

    private static final int PROBE_FAILURE_DELAY_TIME = -1;
    private static final int PROBE_TIMEOUT_MS = 3000;
    private static final int SOCKET_TIMEOUT_MS = 10000;
    private static final boolean OVERSEA_VERSION = true;
    private static final String PROBE_URL = "https://connectivitycheck.picovr.com/wifi.html";
    private static final String PROBE_URL_OVERSEA =
            "https://connectivitycheck-global.picovr.com/wifi.html";
    private static final String DEFAULT_USER_AGENT = "Mozilla/5.0 (X11; Linux x86_64) "
            + "AppleWebKit/537.36 (KHTML, like Gecko) "
            + "Chrome/60.0.3112.32 Safari/537.36";

    private static NetworkPxrAdapter mNetworkPxrAdapterInstance;

    private Context mContext;
    private IPxrNetworkManager mPxrNetworkManager;
    private ConnectivityManager mConnectivityManager;
    private final WifiManager mManagerService;
    private WifiState mWifiState;
    private WifiInfoBean mLastWifiInfoBean;
    private ChannelsEnvironment mChannelsEnvironment;
    private ScanResultBean mScanResultBean;
    private WifiScoreCard mWifiScoreCard;
    private int mLastScore;
    private final IntentFilter mIntentFilter;

    private List<WifiChangeListener> mWifiChangeListener = new ArrayList<>();

    private Map<Integer, NetworkQualityChangedListenerProxy> mNetworkQualityListenerMap =
            new HashMap<>();

    private Map<Integer, LinkLayerQualityChangedListenerProxy> mLinkLayerListenerMap =
            new HashMap<>();

    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (WifiManager.WIFI_STATE_CHANGED_ACTION.equals(action)) {
                mWifiState.handleWifiStateChanged(intent.getIntExtra(
                        WifiManager.EXTRA_WIFI_STATE, WifiManager.WIFI_STATE_UNKNOWN));
                dispatchWifiStateChange(mWifiState.getState());
                updateAndDispatchWifiInfo();
                updateAndDispatchScore();
            } else if (WifiManager.NETWORK_STATE_CHANGED_ACTION.equals(action)) {
                NetworkInfo info = intent.getParcelableExtra(WifiManager.EXTRA_NETWORK_INFO);
                mWifiState.handleConnectStateChanged(info.getDetailedState());
                dispatchWifiStateChange(mWifiState.getState());
                updateAndDispatchWifiInfo();
                updateAndDispatchScore();
            } else if (WifiManager.SCAN_RESULTS_AVAILABLE_ACTION.equals(action)) {
                mChannelsEnvironment.update();
                mScanResultBean.update();
                dispatchWifiScanResultChange(mManagerService.getScanResults());
            } else if (WifiManager.RSSI_CHANGED_ACTION.equals(action)) {
                updateAndDispatchWifiInfo();
                updateAndDispatchScore();
            }
        }
    };

    public NetworkPxrAdapter(Context context) {
        mContext = context;

        mPxrNetworkManager = IPxrNetworkManager.Stub.asInterface(
                ServiceManager.getService(SERVER_NAME));
        mConnectivityManager =
                (ConnectivityManager) mContext.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (mPxrNetworkManager != null) {
            setBinderDeath();
        } else {
            retryGetServiceThread();
        }

        mManagerService = context.getSystemService(WifiManager.class);
        mWifiState = new WifiState();
        mLastWifiInfoBean = new WifiInfoBean(mManagerService);
        mChannelsEnvironment = new ChannelsEnvironment(mManagerService);
        mScanResultBean = new ScanResultBean(mManagerService);
        mWifiScoreCard = new WifiScoreCard(this);
        mLastScore = mWifiScoreCard.calculateScoreLevel();

        mIntentFilter = new IntentFilter(WifiManager.WIFI_STATE_CHANGED_ACTION);
        mIntentFilter.addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION);
        mIntentFilter.addAction(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION);
        mIntentFilter.addAction(WifiManager.RSSI_CHANGED_ACTION);
        mContext.registerReceiver(mReceiver, mIntentFilter);

        LogUtil.d(TAG, "get mPxrNetworkManager = " + mPxrNetworkManager);
    }

    public static NetworkPxrAdapter getInstance(Context context) {
        if (context == null) {
            throw new IllegalArgumentException("context cannot be null");
        }

        if (mNetworkPxrAdapterInstance == null) {
            mNetworkPxrAdapterInstance = new NetworkPxrAdapter(context);
        }

        return mNetworkPxrAdapterInstance;
    }

    public void refreshData() {
        updateAndDispatchWifiInfo();
        updateAndDispatchScore();
        startScan();
    }

    public int getWifiState() {
        return mWifiState.getState();
    }

    public int getWifiStandard() {
        return mLastWifiInfoBean.getWifiStandard();
    }

    public String getBssid() {
        if (getWifiState() == STATE_CONNECTED) {
            return mLastWifiInfoBean.getBssid();
        }
        return null;
    }

    public String getSsid() {
        if (getWifiState() == STATE_CONNECTED) {
            return mLastWifiInfoBean.getSsid();
        }
        return null;
    }

    public int getFrequency() {
        if (getWifiState() == STATE_CONNECTED) {
            return mLastWifiInfoBean.getFrequency();
        }
        return INT_ERROR;
    }

    public int getBand() {
        int freqMhz = getFrequency();
        if (WifiUtils.is24GHz(freqMhz)) {
            return WIFI_BAND_24_GHZ;
        } else if (WifiUtils.is5GHz(freqMhz)) {
            return WIFI_BAND_5_GHZ;
        } else if (WifiUtils.is6GHz(freqMhz)) {
            return WIFI_BAND_6_GHZ;
        }
        return INT_ERROR;
    }

    public int getRssi() {
        if (getWifiState() == STATE_CONNECTED) {
            return mLastWifiInfoBean.getRssi();
        }
        return INT_ERROR;
    }

    public int getChannel() {
        if (getWifiState() == STATE_CONNECTED) {
            return mLastWifiInfoBean.getChannel();
        }
        return INT_ERROR;
    }

    public int getBandwidth() {
        if (getWifiState() != STATE_CONNECTED) {
            return INT_ERROR;
        }

        return mScanResultBean.getBandwidth();
    }

    public List<ScanResult> getLastScanResults() {
        if (getWifiState() < STATE_ON) {
            return null;
        }
        return mScanResultBean.getLastScanResults();
    }

    public boolean startScan() {
        if (getWifiState() < STATE_ON) {
            return false;
        }
        return mScanResultBean.startScan();
    }

    public int getCurrentChannelUsedCount() {
        if (getWifiState() != STATE_CONNECTED) {
            return INT_ERROR;
        }
        return mScanResultBean.getCurrentChannelUsedCount();
    }

    public int getTxLinkSpeedMbps() {
        if (getWifiState() != STATE_CONNECTED) {
            return INT_ERROR;
        }
        return mLastWifiInfoBean.getTxLinkSpeedMbps();
    }

    public int getRxLinkSpeedMbps() {
        if (getWifiState() != STATE_CONNECTED) {
            return INT_ERROR;
        }
        return mLastWifiInfoBean.getRxLinkSpeedMbps();
    }

    public int getWifiScore() {
        mWifiScoreCard.calculateScoreLevel();
        return mWifiScoreCard.getScore();
    }

    public boolean registerWifiChangeListener(WifiChangeListener listener) {
        return mWifiChangeListener.add(listener);
    }

    public boolean unregisterWifiChangeListener(WifiChangeListener listener) {
        if (mWifiChangeListener.contains(listener)) {
            return mWifiChangeListener.remove(listener);
        }
        return false;
    }

    private void updateAndDispatchScore() {
        mWifiScoreCard.calculateScoreLevel();
        if (mLastScore != mWifiScoreCard.getScore()) {
            mLastScore = mWifiScoreCard.getScore();
            dispatchWifiScoreChange(mLastScore);
        }
    }

    private void updateAndDispatchWifiInfo() {
        WifiInfoBean current = mLastWifiInfoBean.update();
        if (current == null) {
            return;
        }

        LogUtil.d(TAG, "Update WifiInfoBean:" + current.toString());
        if (current.equals(mLastWifiInfoBean)) {
            dispatchWifiInfoChange(true);
            mLastWifiInfoBean = current;
        }
    }

    private void dispatchWifiStateChange(int state) {
        for (WifiChangeListener wifiChangeListener : mWifiChangeListener) {
            wifiChangeListener.onWifiStateChanged(state);
        }
    }

    private void dispatchWifiInfoChange(boolean changed) {
        for (WifiChangeListener wifiChangeListener : mWifiChangeListener) {
            wifiChangeListener.onWifiInfoChanged(changed);
        }
    }

    private void dispatchWifiScanResultChange(List<ScanResult> scanResults) {
        for (WifiChangeListener wifiChangeListener : mWifiChangeListener) {
            wifiChangeListener.onScanInfoChanged(scanResults);
        }
    }

    private void dispatchWifiScoreChange(int score) {
        for (WifiChangeListener wifiChangeListener : mWifiChangeListener) {
            wifiChangeListener.onWifiScoredChanged(score);
        }
    }

    /** Callback for per-process network quality updates. */
    public interface NetworkQualityChangedListener {
        void onNetworkQualityChanged(NetworkQuality networkQuality);
    }

    class NetworkQualityChangedListenerProxy extends INetworkQualityListener.Stub {
        private final NetworkQualityChangedListener mListener;
        private final int mTimeout;

        NetworkQualityChangedListenerProxy(int timeout, NetworkQualityChangedListener listener) {
            mListener = listener;
            mTimeout = timeout;
        }

        public int getTimeout() {
            return mTimeout;
        }

        public NetworkQualityChangedListener getListener() {
            return mListener;
        }

        @Override
        public void onNetworkQualityChanged(NetworkQuality networkQuality) {
            mListener.onNetworkQualityChanged(networkQuality);
        }
    }

    class LinkLayerQualityChangedListenerProxy extends ILinkLayerQualityListener.Stub {
        private final LinkLayerQualityChangedListener mListener;

        LinkLayerQualityChangedListenerProxy(LinkLayerQualityChangedListener listener) {
            mListener = listener;
        }

        public LinkLayerQualityChangedListener getListener() {
            return mListener;
        }

        @Override
        public void onLinkLayerQualityChanged(LinkLayerQuality linkLayerQuality) {
            mListener.onLinkLayerQualityChanged(linkLayerQuality);
        }

        @Override
        public void onLinkLayerLevelChanged(LinkLayerQuality linkLayerQuality) {
            mListener.onLinkLayerLevelChanged(linkLayerQuality);
        }

        @Override
        public void onAverLinkLayerLevelChanged(LinkLayerQuality linkLayerQuality) {
            mListener.onAverLinkLayerLevelChanged(linkLayerQuality);
        }
    }

    public boolean registerNetworkQualityChangedListener(int timeout,
            NetworkQualityChangedListener listener) {
        if (mPxrNetworkManager == null) return false;
        if (listener == null) throw new IllegalArgumentException("callback cannot be null");
        if (mNetworkQualityListenerMap.containsKey(listener.hashCode())) return false;

        try {
            NetworkQualityChangedListenerProxy listenerProxy =
                    new NetworkQualityChangedListenerProxy(timeout, listener);
            boolean registerSuccess = mPxrNetworkManager.registerNetworkQualityChangedListener(
                    mContext.getOpPackageName(), timeout, listenerProxy);
            if (registerSuccess) {
                mNetworkQualityListenerMap.put(listener.hashCode(), listenerProxy);
            }
        } catch (RemoteException e) {
            LogUtil.e(TAG, "RemoteException !!!");
        }
        return true;
    }

    public boolean unregisterNetworkQualityChangedListener(
            NetworkQualityChangedListener listener) {
        if (mPxrNetworkManager == null) return false;

        if (listener != null) {
            try {
                int listenerHashCode = listener.hashCode();
                if (!mNetworkQualityListenerMap.containsKey(listenerHashCode)) {
                    return false;
                }
                NetworkQualityChangedListenerProxy listenerProxy =
                        mNetworkQualityListenerMap.remove(listenerHashCode);
                mPxrNetworkManager.unregisterNetworkQualityChangedListener(
                        mContext.getOpPackageName(), listenerProxy);
            } catch (RemoteException e) {
                LogUtil.e(TAG, "RemoteException !!!");
            }
        }
        return true;
    }

    public boolean isInternetConnected() {
        Network[] networks = mConnectivityManager.getAllNetworks();
        for (Network network : networks) {
            NetworkInfo networkInfo = mConnectivityManager.getNetworkInfo(network);
            if (networkInfo != null && networkInfo.getType() == ConnectivityManager.TYPE_WIFI
                    && networkInfo.isConnected()) {
                NetworkCapabilities networkCapabilities =
                        mConnectivityManager.getNetworkCapabilities(network);
                if (networkCapabilities != null && networkCapabilities.hasCapability(
                        NetworkCapabilities.NET_CAPABILITY_VALIDATED)) {
                    return true;
                }
            }
        }

        return false;
    }

    public boolean probeHttpsDelay(ProbeCallback callback) {
        ProbeThread httpsProbe = new ProbeThread(PROBE_TYPE_HTTPS, callback);

        try {
            httpsProbe.start();
        } catch (Exception e) {
            LogUtil.e(TAG, "Error: probes wait interrupted!");
            return false;
        }

        return true;
    }

    public boolean probeDnsDelay(ProbeCallback callback) {
        ProbeThread dnsProbe = new ProbeThread(PROBE_TYPE_DNS, callback);

        try {
            dnsProbe.start();
        } catch (Exception e) {
            LogUtil.e(TAG, "Error: probes wait interrupted!");
            return false;
        }

        return true;
    }

    /** Receives the result of a latency probe; delayMs is -1 on failure. */
    public static class ProbeCallback {
        public void onProbeCompleted(boolean isSuccessful, long delayMs) {
        }
    }

    private URL makeURL(String url) {
        if (url != null) {
            try {
                return new URL(url);
            } catch (MalformedURLException e) {
                LogUtil.e(TAG, "Bad URL: " + url);
            }
        }
        return null;
    }

    final class ProbeThread extends Thread {
        long mResult = PROBE_FAILURE_DELAY_TIME;
        int mIsHttpsOrDns;
        ProbeCallback mProbeCallback;

        ProbeThread(int isHttpsOrDns, ProbeCallback callback) {
            mIsHttpsOrDns = isHttpsOrDns;
            mProbeCallback = callback;
        }

        @Override
        public void run() {
            URL probeUrl = makeURL(getDefaultTestServerHttpsUrl());
            if (mIsHttpsOrDns == PROBE_TYPE_DNS) {
                mResult = sendDnsProbe(probeUrl);
            } else if (mIsHttpsOrDns == PROBE_TYPE_HTTPS) {
                mResult = sendHttpProbe(probeUrl);
            }

            if (mResult != PROBE_FAILURE_DELAY_TIME) {
                mProbeCallback.onProbeCompleted(true, mResult);
            } else {
                mProbeCallback.onProbeCompleted(false, PROBE_FAILURE_DELAY_TIME);
            }
        }
    }

    protected InetAddress[] sendDnsProbeWithTimeout(String host, int timeoutMs)
            throws UnknownHostException {
        return DnsUtils.getAllByName(DnsResolver.getInstance(), null /* network */, host,
                DnsUtils.TYPE_ADDRCONFIG, 0 /* FLAG_EMPTY */, timeoutMs);
    }

    private String getDefaultTestServerHttpsUrl() {
        return OVERSEA_VERSION ? PROBE_URL_OVERSEA : PROBE_URL;
    }

    private long sendDnsProbe(URL url) {
        if (url == null) {
            return PROBE_FAILURE_DELAY_TIME;
        }
        String host = url.getHost();
        if (TextUtils.isEmpty(host)) {
            return PROBE_FAILURE_DELAY_TIME;
        }

        final Stopwatch watch = new Stopwatch().start();
        boolean isResultSuccessful = false;
        String connectInfo;
        try {
            InetAddress[] addresses = sendDnsProbeWithTimeout(host, 5000);
            StringBuffer buffer = new StringBuffer();
            for (InetAddress address : addresses) {
                buffer.append(',').append(address.getHostAddress());
            }
            isResultSuccessful = true;
            connectInfo = "OK " + buffer.substring(1);
        } catch (UnknownHostException e) {
            isResultSuccessful = false;
            connectInfo = "FAIL";
        }
        long latency;
        if (isResultSuccessful) {
            latency = watch.stop();
        } else {
            latency = PROBE_FAILURE_DELAY_TIME;
        }

        LogUtil.d(TAG, "dns request: " + host + "; "
                + String.format("%dms %s", latency, connectInfo));
        LogUtil.d(TAG, "dns result: " + latency);
        return latency;
    }

    private long sendHttpProbe(URL url) {
        if (url == null) {
            return PROBE_FAILURE_DELAY_TIME;
        }

        HttpURLConnection testUrlConnection = null;
        Network wifiNetwork = mConnectivityManager.getNetworkForType(
                ConnectivityManager.TYPE_WIFI);
        if (wifiNetwork == null) {
            return PROBE_FAILURE_DELAY_TIME;
        }

        try {
            testUrlConnection = (HttpURLConnection) wifiNetwork.openConnection(url);
            testUrlConnection.setConnectTimeout(5000);
            testUrlConnection.setReadTimeout(5000);
            testUrlConnection.setUseCaches(false);
            testUrlConnection.setRequestProperty("User-Agent", DEFAULT_USER_AGENT);

            final Stopwatch watch = new Stopwatch().start();
            final int responseCode = testUrlConnection.getResponseCode();
            final long latency = watch.stop();
            LogUtil.d(TAG, "uir: " + url + " time=" + latency + "ms ret=" + responseCode);

            if (responseCode == 200 && testUrlConnection.getContentLength() < 1024) {
                InputStream input = testUrlConnection.getInputStream();
                if (input == null) {
                    return PROBE_FAILURE_DELAY_TIME;
                }

                String content = null;
                BufferedReader br = new BufferedReader(new InputStreamReader(input));
                try {
                    content = br.readLine();
                    LogUtil.d(TAG, "content : " + content);
                    if (!TextUtils.isEmpty(content)) {
                        String context = content.trim();
                        if ("ok".equalsIgnoreCase(context)) {
                            return latency;
                        }
                    }
                } catch (IOException e) {
                    LogUtil.e(TAG, "Error:" + e);
                } catch (OutOfMemoryError e) {
                    LogUtil.e(TAG, "Exception " + e);
                }
            }
        } catch (IOException e) {
            // Probe failed; fall through and report a failure delay.
        } finally {
            if (testUrlConnection != null) {
                testUrlConnection.disconnect();
            }
        }

        return PROBE_FAILURE_DELAY_TIME;
    }

    private void autoRegisterListenerOnRetryBindSuccess() {
        if (mNetworkQualityListenerMap != null && mNetworkQualityListenerMap.size() > 0) {
            for (Integer key : mNetworkQualityListenerMap.keySet()) {
                NetworkQualityChangedListenerProxy proxy = mNetworkQualityListenerMap.get(key);
                if (proxy == null) {
                    continue;
                }
                try {
                    mPxrNetworkManager.registerNetworkQualityChangedListener(
                            mContext.getOpPackageName(), proxy.getTimeout(), proxy);
                } catch (RemoteException e) {
                    Log.e(TAG, "RemoteException !!!");
                }
            }
        }

        if (mLinkLayerListenerMap != null && mLinkLayerListenerMap.size() > 0) {
            for (Integer key : mLinkLayerListenerMap.keySet()) {
                LinkLayerQualityChangedListenerProxy proxy = mLinkLayerListenerMap.get(key);
                if (proxy == null) {
                    continue;
                }
                try {
                    mPxrNetworkManager.registerLinkLayerQualityChangedListener(
                            mContext.getOpPackageName(), proxy);
                } catch (RemoteException e) {
                    Log.e(TAG, "RemoteException !!!");
                }
            }
        }
    }

    private void informationListenerOnRetryBindFail() {
        if (mNetworkQualityListenerMap != null && mNetworkQualityListenerMap.size() > 0) {
            for (Integer key : mNetworkQualityListenerMap.keySet()) {
                NetworkQualityChangedListenerProxy proxy = mNetworkQualityListenerMap.get(key);
                if (proxy == null || proxy.getListener() == null) {
                    continue;
                }

                proxy.getListener().onNetworkQualityChanged(null);
                mNetworkQualityListenerMap.remove(key);
            }
        }

        if (mLinkLayerListenerMap != null && mLinkLayerListenerMap.size() > 0) {
            for (Integer key : mLinkLayerListenerMap.keySet()) {
                LinkLayerQualityChangedListenerProxy proxy = mLinkLayerListenerMap.get(key);
                if (proxy == null || proxy.getListener() == null) {
                    continue;
                }

                proxy.getListener().onLinkLayerQualityChanged(null);
                proxy.getListener().onLinkLayerLevelChanged(null);
                proxy.getListener().onAverLinkLayerLevelChanged(null);
                mLinkLayerListenerMap.remove(key);
            }
        }
    }

    public void retryGetServiceThread() {
        new Thread() {
            @Override
            public void run() {
                for (int i = 0; i < RETRY_GET_SERVER_COUNT; i++) {
                    SystemClock.sleep(GET_SERVER_INTERVAL_MS);
                    mPxrNetworkManager = IPxrNetworkManager.Stub.asInterface(
                            ServiceManager.getService(SERVER_NAME));
                    if (mPxrNetworkManager != null) {
                        setBinderDeath();
                        autoRegisterListenerOnRetryBindSuccess();
                        return;
                    }
                }
                LogUtil.e(TAG, "get pxr network service error!!!");
                informationListenerOnRetryBindFail();
            }
        }.start();
    }

    private void setBinderDeath() {
        try {
            mPxrNetworkManager.asBinder().linkToDeath(new IBinder.DeathRecipient() {
                @Override
                public void binderDied() {
                    LogUtil.d(TAG, "pxr network service is dead!");
                    mPxrNetworkManager = null;
                    retryGetServiceThread();
                }
            }, 0);
        } catch (RemoteException e) {
            LogUtil.e(TAG, "mPxrNetworkManager linkToDeath! :" + e);
        }
    }

    /** Callback for link layer quality and quality level updates. */
    public interface LinkLayerQualityChangedListener {
        void onLinkLayerQualityChanged(LinkLayerQuality linkLayerQuality);

        void onLinkLayerLevelChanged(LinkLayerQuality linkLayerQuality);

        void onAverLinkLayerLevelChanged(LinkLayerQuality linkLayerQuality);
    }

    public boolean registerLinkLayerQualityChangedListener(
            LinkLayerQualityChangedListener listener) {
        if (mPxrNetworkManager == null) return false;
        if (listener == null) throw new IllegalArgumentException("callback cannot be null");
        if (mLinkLayerListenerMap.containsKey(listener.hashCode())) return false;

        try {
            LinkLayerQualityChangedListenerProxy listenerProxy =
                    new LinkLayerQualityChangedListenerProxy(listener);
            boolean registerSuccess = mPxrNetworkManager.registerLinkLayerQualityChangedListener(
                    mContext.getOpPackageName(), listenerProxy);
            if (registerSuccess) {
                mLinkLayerListenerMap.put(listener.hashCode(), listenerProxy);
            }
        } catch (RemoteException e) {
            LogUtil.e(TAG, "RemoteException !!!");
            return false;
        }
        return true;
    }

    public boolean unregisterLinkLayerQualityChangedListener(
            LinkLayerQualityChangedListener listener) {
        if (mPxrNetworkManager == null) return false;

        try {
            if (listener != null) {
                int listenerHashCode = listener.hashCode();
                if (!mLinkLayerListenerMap.containsKey(listenerHashCode)) {
                    return false;
                }
                LinkLayerQualityChangedListenerProxy listenerProxy =
                        mLinkLayerListenerMap.remove(listenerHashCode);
                mPxrNetworkManager.unregisterLinkLayerQualityChangedListener(
                        mContext.getOpPackageName(), listenerProxy);
            } else {
                return false;
            }
        } catch (RemoteException e) {
            LogUtil.e(TAG, "RemoteException !!!");
        }
        return true;
    }

    public boolean updateLinkLayerQuality(PxrWifiConnectionInfo pwci) {
        if (mPxrNetworkManager == null) {
            return false;
        }
        String test = null;
        try {
            mPxrNetworkManager.updateLinkLayerQuality(mContext.getOpPackageName(), pwci);
        } catch (Exception e) {
            LogUtil.e(TAG, "updateLinkLayerQuality fail, e=" + e);
        }
        return true;
    }

    public LinkLayerQuality getLinkLayerQuality() {
        if (mPxrNetworkManager == null) {
            return null;
        }
        String test = null;
        try {
            return mPxrNetworkManager.getLinkLayerQuality(mContext.getOpPackageName());
        } catch (Exception e) {
            LogUtil.e(TAG, "getLinkLayerQuality fail, e=" + e);
        }
        return null;
    }

    public LinkLayerQuality getAverageLinkLayerQuality() {
        if (mPxrNetworkManager == null) {
            return null;
        }
        try {
            return mPxrNetworkManager.getAverageLinkLayerQuality(mContext.getOpPackageName());
        } catch (Exception e) {
            LogUtil.e(TAG, "getAverageLinkLayerQuality fail, e=" + e);
        }
        return null;
    }
}
