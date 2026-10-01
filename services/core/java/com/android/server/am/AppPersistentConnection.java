// Copyright 2026 Picomisu contributors
// SPDX-License-Identifier: Apache-2.0
package com.android.server.am;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Handler;
import android.os.IBinder;
import android.os.UserHandle;
import android.util.Slog;

/**
 * Keeps a service of an app bound for the PICO API layer (factory PICO OS 5.13.7
 * com.android.server.am.AppPersistentConnection, used by
 * ExtActivityTaskManagerServiceImpl.updatePersistentConnection): rebinds after the binding or
 * the service process dies, and re-runs the bind up to ten times (every 10 s after a successful
 * bind call, every 1 s after a failed one) until the service connects.
 */
public class AppPersistentConnection implements ServiceConnection {
    private static final int MAX_RETRY_COUNT = 10;
    private static final String TAG = "APC";
    private Context mContext;
    private Handler mHandler;
    private ComponentName mName;
    private IBinder mServiceBinder;
    private long bindTaskDelay = 1000;
    private int mRetryCount = 0;
    private final IBinder.DeathRecipient mDeathRecipient = new IBinder.DeathRecipient() {
        @Override
        public void binderDied() {
            clientBinderDied();
        }
    };
    private Runnable bindClientTask = new Runnable() {
        @Override
        public void run() {
            try {
                bindClient();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    };

    public AppPersistentConnection(Context context, Handler handler, ComponentName name) {
        mContext = context;
        mHandler = handler;
        mName = name;
    }

    @Override
    public void onServiceConnected(ComponentName name, IBinder service) {
        Slog.i(TAG, "onServiceConnected: " + name);
        if (service == null) {
            Slog.w(TAG, "onServiceConnected service obj is null");
            return;
        }
        if (mServiceBinder != null) {
            try {
                mServiceBinder.unlinkToDeath(mDeathRecipient, 0);
            } catch (Exception e) {
            }
        }
        mServiceBinder = service;
        try {
            mServiceBinder.linkToDeath(mDeathRecipient, 0);
            mHandler.removeCallbacks(bindClientTask);
            mRetryCount = 0;
        } catch (Exception e2) {
            e2.printStackTrace();
        }
    }

    @Override
    public void onServiceDisconnected(ComponentName name) {
        Slog.i(TAG, "onServiceDisconnected: " + name);
    }

    @Override
    public void onBindingDied(ComponentName name) {
        Slog.i(TAG, "onBindingDied: " + name);
        postBindTask(bindTaskDelay);
    }

    public void onBindFinish(boolean result) {
        if (mRetryCount >= MAX_RETRY_COUNT) {
            Slog.i(TAG, "onBindFinish abandon, retry too many times");
            return;
        }
        mRetryCount++;
        long delay = result ? bindTaskDelay * 10 : bindTaskDelay;
        Slog.i(TAG, "onBindFinish [" + result + "], mRetryCount [" + mRetryCount + "], name "
                + mName);
        postBindTask(delay);
    }

    private void postBindTask(long delay) {
        mHandler.removeCallbacks(bindClientTask);
        mHandler.postDelayed(bindClientTask, delay);
    }

    public void bindClient() {
        boolean result = true;
        try {
            Intent intent = new Intent();
            intent.setComponent(mName);
            result = mContext.bindServiceAsUser(intent, this, Context.BIND_AUTO_CREATE,
                    UserHandle.OWNER);
            Slog.i(TAG, "bindClient [" + mName + "] result : " + result);
        } catch (Exception e) {
            result = false;
            e.printStackTrace();
        } finally {
            onBindFinish(result);
        }
    }

    public void unbindClient() {
        mRetryCount = 0;
        try {
            Slog.i(TAG, "unbindClient [" + mName + "]");
            mHandler.removeCallbacks(bindClientTask);
            if (mServiceBinder != null) {
                mServiceBinder.unlinkToDeath(mDeathRecipient, 0);
                mServiceBinder = null;
            }
            mContext.unbindService(this);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void clientBinderDied() {
        Slog.i(TAG, "clientBinderDied, post task for restart, delay = " + bindTaskDelay
                + ", name " + mName);
        postBindTask(bindTaskDelay);
    }

    public ComponentName getComponentName() {
        return mName;
    }
}
