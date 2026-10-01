// Copyright 2026 Picomisu contributors
// SPDX-License-Identifier: Apache-2.0

package android.net;

import android.annotation.TargetApi;
import android.os.Message;
import android.os.RemoteException;
import android.os.SystemProperties;
import android.util.Log;

import smartisanos.util.FeatLog;

/**
 * Smartisan extension of {@link ConnectivityManager} (factory PICO OS 5.13.7
 * android.net.ConnectivityManagerSmtEx): client-side caches of the active network info and of
 * the network info list.
 *
 * <p>With persist.sys.network.binder.cache = 1 (the default) getActiveNetworkInfo() answers from
 * a process-wide cache that is filled by the first binder call and cleared by
 * {@link #clearActiveNetworkInfoCache} (activity resume, Smartisan special intents, unfreeze).
 * Otherwise the per-instance caches are used only while {@link #useCache} is set (AbsListView
 * touch / fling).
 *
 * @hide
 */
public class ConnectivityManagerSmtEx {
    public static final String BUNDLE_NETWORK_BINDER_SWITCH = "bundle_network_binder_switch";
    public static final String PROPERTY_ACTIVE_NETWORK_INFO_BINDER_CACHE =
            "persist.sys.network.binder.cache";

    @TargetApi(21)
    public static final NetworkRequest STATIC_REQUEST;
    private static final String TAG = "ConnectivityManagerSmtEx";
    private static boolean bindCallCacheNetworkInfo;

    @TargetApi(21)
    public static final ConnectivityManager.NetworkCallback mNetworkCallback;
    public static boolean sBinderCache;
    public static volatile NetworkInfo sNetworkInfoCache = null;
    private ConnectivityManager mConnectivityManager;
    public boolean useCache = false;
    boolean mCachedNetworkInfos = false;
    NetworkInfo mNetworkInfo = null;
    NetworkInfo[] mNetworkInfos = null;

    static {
        sBinderCache = SystemProperties.getInt(PROPERTY_ACTIVE_NETWORK_INFO_BINDER_CACHE, 1) == 1;
        STATIC_REQUEST = new NetworkRequest.Builder().build();
        mNetworkCallback = new ConnectivityManager.NetworkCallback() {
        };
        bindCallCacheNetworkInfo = SystemProperties.getBoolean(
                "debug.bytedance.logcontrol.bindCallCacheNetworkInfo", false);
    }

    public ConnectivityManagerSmtEx(ConnectivityManager connectivityManager) {
        mConnectivityManager = connectivityManager;
    }

    public static void clearActiveNetworkInfoCache() {
        sNetworkInfoCache = null;
    }

    void registerNetworkCallbackForCache(NetworkCapabilitiesSmtEx networkCapabilitiesSmtEx) {
        sBinderCache = SystemProperties.getInt(PROPERTY_ACTIVE_NETWORK_INFO_BINDER_CACHE, 1) == 1;
        if (sBinderCache) {
            networkCapabilitiesSmtEx.mCallbackForCache = true;
            mConnectivityManager.registerNetworkCallback(STATIC_REQUEST, mNetworkCallback);
        }
    }

    void clearNetworkInfoCacheFromHandleMessage(ConnectivityManager.NetworkCallback callback,
            Message message) {
        clearActiveNetworkInfoCache();
        if (callback == mNetworkCallback) {
            int flag = message.getData().getInt(BUNDLE_NETWORK_BINDER_SWITCH);
            if (flag == 2) {
                SystemProperties.set(PROPERTY_ACTIVE_NETWORK_INFO_BINDER_CACHE, String.valueOf(2));
                sBinderCache = false;
            } else if (flag == 1) {
                SystemProperties.set(PROPERTY_ACTIVE_NETWORK_INFO_BINDER_CACHE, String.valueOf(1));
                sBinderCache = true;
            }
        }
    }

    NetworkInfo getCacheOrBinderCall(IConnectivityManager mService) throws RemoteException {
        if (sBinderCache) {
            if (sNetworkInfoCache != null) {
                if (bindCallCacheNetworkInfo) {
                    // smartisanos.config.ProductConfig.FEAT_BINDER_CALL_CACHE_TAG
                    FeatLog.d(TAG, "FEAT_BINDER_CALL_CACHE", 0,
                            "getActiveNetworkInfo binder cache works");
                }
                return sNetworkInfoCache;
            }
            sNetworkInfoCache = mService.getActiveNetworkInfo();
            return sNetworkInfoCache;
        }
        if (useCache) {
            if (mNetworkInfo != null) {
                return mNetworkInfo;
            }
            mNetworkInfo = mService.getActiveNetworkInfo();
            return mNetworkInfo;
        }
        mNetworkInfo = null;
        sNetworkInfoCache = null;
        NetworkInfo ni = mService.getActiveNetworkInfo();
        return ni;
    }

    NetworkInfo[] getAllNetworkInfoSmtEx(IConnectivityManager mService) {
        if (useCache) {
            Log.d(TAG, "getAllNetworkInfo useCache");
        }
        if (useCache && mCachedNetworkInfos) {
            return mNetworkInfos;
        }
        try {
            if (useCache) {
                NetworkInfo[] infos = mService.getAllNetworkInfo();
                if (infos != null) {
                    int len = infos.length;
                    mNetworkInfos = new NetworkInfo[len];
                    System.arraycopy(infos, 0, mNetworkInfos, 0, len);
                    mCachedNetworkInfos = true;
                }
                return mNetworkInfos;
            }
            mNetworkInfos = null;
            mCachedNetworkInfos = false;
            return mService.getAllNetworkInfo();
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }
}
