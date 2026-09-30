// Copyright 2026 Picomisu contributors
// SPDX-License-Identifier: Apache-2.0
package com.android.server.power;

import android.content.Context;
import android.os.Binder;
import android.os.IBinder;
import android.os.RemoteException;
import android.os.SystemProperties;
import android.util.Log;

import com.android.server.lights.LightsManager;

import java.util.Arrays;
import java.util.List;

/**
 * PICO power manager service extension.
 * @hide
 */
public class ExtPowerManagerServiceImpl implements IExtPowerManagerService {
    private static final String TAG = "PowerManagerService";
    /** Packages allowed to switch off the proximity-sensor controlled screen. */
    private static final List<String> CAN_NEVER_AUTO_SLEEP_LIST =
            Arrays.asList("com.pvr.lanserver");
    private static final String PROP_NEVER_AUTO_SLEEP_MODE_BY_SENSOR =
            "pvr.factorytest.never.sleep";
    private static final String PROP_NEVER_AUTO_SLEEP_BY_SENSOR_ENABLED = "1";
    private static final String PROP_NEVER_AUTO_SLEEP_BY_SENSOR_DISENABLED = "0";

    private Object mLock = new Object();
    private PowerManagerService mBase;
    private Context mContext;
    /** Token of the application that switched the feature off, reset when it dies. */
    private IBinder mCurrentToken;
    private IBinder.DeathRecipient mDeathRecipient = new IBinder.DeathRecipient() {
        @Override
        public void binderDied() {
            Log.i(TAG, "app die reset sensor control screen feature mCurrentToken = "
                    + mCurrentToken);
            synchronized (mLock) {
                changeFeatureSwitch(true);
                mCurrentToken.unlinkToDeath(this, 0);
                mCurrentToken = null;
            }
        }
    };

    public ExtPowerManagerServiceImpl(PowerManagerService base) {
        mBase = base;
    }

    /**
     * Resets the sensor control screen feature and records the context. Unlike the factory,
     * whose remaining part registers the PICO LED battery and boot receivers in place of the
     * battery receiver of the power manager service and returns true, this returns false so
     * that the power manager service keeps registering its own battery receiver.
     */
    @Override
    public boolean systemReady(Context context, LightsManager lightsManager) {
        synchronized (mLock) {
            changeFeatureSwitch(true);
        }
        mContext = context;
        return false;
    }

    @Override
    public void setSensorControlScreenFeatureState(boolean opened, IBinder appToken,
            String packageName) {
        if (CAN_NEVER_AUTO_SLEEP_LIST.contains(packageName)) {
            mContext.enforceCallingOrSelfPermission(android.Manifest.permission.DEVICE_POWER,
                    "setSensorControlScreenFeatureState");
            Log.i(TAG, "setSensorControlScreenFeatureState opened = " + opened
                    + ",pid=" + Binder.getCallingPid() + ",uid=" + Binder.getCallingUid());
            synchronized (mLock) {
                changeFeatureSwitch(opened);
                linkToDeath(opened, appToken);
            }
        } else {
            throw new SecurityException(packageName
                    + " can not change sensor controller screen state!");
        }
    }

    private void changeFeatureSwitch(boolean opened) {
        if (opened) {
            SystemProperties.set(PROP_NEVER_AUTO_SLEEP_MODE_BY_SENSOR,
                    PROP_NEVER_AUTO_SLEEP_BY_SENSOR_DISENABLED);
        } else {
            SystemProperties.set(PROP_NEVER_AUTO_SLEEP_MODE_BY_SENSOR,
                    PROP_NEVER_AUTO_SLEEP_BY_SENSOR_ENABLED);
        }
    }

    /**
     * Forgets the previous token and, when the feature is switched off, watches the token of
     * the caller so that the feature is switched on again when the caller dies.
     */
    private void linkToDeath(boolean opened, IBinder token) {
        if (mCurrentToken != null) {
            Log.i(TAG, "unlinkToDeath mCurrentToken = " + mCurrentToken);
            mCurrentToken.unlinkToDeath(mDeathRecipient, 0);
        }
        if (opened) {
            mCurrentToken = null;
            return;
        }
        mCurrentToken = token;
        if (mCurrentToken != null && mCurrentToken.isBinderAlive()) {
            try {
                Log.i(TAG, "linkToDeath mCurrentToken = " + mCurrentToken);
                mCurrentToken.linkToDeath(mDeathRecipient, 0);
            } catch (RemoteException e) {
                Log.e(TAG, "linkToDeath err = " + e.getMessage());
                changeFeatureSwitch(true);
                mCurrentToken = null;
            }
        }
    }
}
