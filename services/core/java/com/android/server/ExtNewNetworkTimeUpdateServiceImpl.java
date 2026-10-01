// Copyright 2026 Picomisu contributors
// SPDX-License-Identifier: Apache-2.0
package com.android.server;

import android.util.NtpTrustedTime;
import android.util.Slog;

/**
 * PICO NewNetworkTimeUpdateService extension (factory PICO OS 5.13.7
 * com.android.server.ExtNewNetworkTimeUpdateServiceImpl): a stale NTP fix is refreshed from a
 * fixed server list (asia / android / cn pool, NIST, Windows), trying the next server one
 * second after a failure.
 */
public class ExtNewNetworkTimeUpdateServiceImpl implements IExtNewNetworkTimeUpdateService {
    private static final boolean DBG = false;
    private static final String[] SERVER_LIST = {"asia.pool.ntp.org", "2.android.pool.ntp.org",
            "cn.pool.ntp.org", "time.nist.gov", "time.windows.com"};
    private static final String TAG = "NetworkTimeUpdateService";
    private NewNetworkTimeUpdateService mBase;

    public ExtNewNetworkTimeUpdateServiceImpl(NewNetworkTimeUpdateService base) {
        mBase = base;
    }

    @Override
    public void syncTimeFromServer(NtpTrustedTime time, int tryAgainCounter) {
        for (String server : SERVER_LIST) {
            if (time.forceSync(server)) {
                return;
            }
            Slog.w(TAG, "mTryAgainCounter[" + tryAgainCounter + "] try to sync time  with server:"
                    + server + " failed,try next server 1 sec later");
            try {
                Thread.sleep(1000L);
            } catch (InterruptedException e) {
                Slog.e(TAG, "InterruptedException when forcerefresh.", e);
            }
        }
    }
}
