// Copyright 2026 Picomisu contributors
// SPDX-License-Identifier: Apache-2.0

package smartisanos.api;

import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManagerSmtEx;
import android.util.Singleton;

import java.util.Timer;
import java.util.TimerTask;

/**
 * Smartisan handling of intents delivered to {@link android.app.LoadedApk} receivers
 * (factory PICO OS 5.13.7 {@code smartisanos.api.LoadedApkSmtEx}).
 *
 * @hide
 */
public class LoadedApkSmtEx {
    static final String TAG = "LoadedApkSmt";

    static final Singleton<LoadedApkSmtEx> sLoadedApkSmt = new Singleton<LoadedApkSmtEx>() {
        @Override
        protected LoadedApkSmtEx create() {
            return new LoadedApkSmtEx();
        }
    };

    public static LoadedApkSmtEx getInstance() {
        return sLoadedApkSmt.get();
    }

    private LoadedApkSmtEx() {
    }

    public static void handleSpecialIntentSmt(Intent intent, final Context context) {
        if ((intent.getSmtEx().getSmFlags() & 8) != 0) {
            ConnectivityManagerSmtEx.clearActiveNetworkInfoCache();
        }

        boolean smtisfromsystemui = intent.getSmtEx().getSmtBooleanExtra("IS_FROM_NOTIFICATION",
                false);
        intent.getSmtEx().removeSmtExtra("IS_FROM_NOTIFICATION");
        if (smtisfromsystemui) {
            if (context.getApplicationInfo() != null) {
                context.getApplicationInfo().getSmtEx().isFromSystemUI = true;
                TimerTask task = new TimerTask() {
                    @Override
                    public void run() {
                        if (context.getApplicationInfo() != null
                                && context.getApplicationInfo().getSmtEx().isFromSystemUI) {
                            context.getApplicationInfo().getSmtEx().isFromSystemUI = false;
                        }
                    }
                };
                Timer timer = new Timer();
                timer.schedule(task, 15000);
            }
        }
    }
}
