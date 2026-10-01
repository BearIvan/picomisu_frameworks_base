// Copyright 2026 Picomisu contributors
// SPDX-License-Identifier: Apache-2.0
package com.android.server.power;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.AsyncTask;
import android.os.BatteryManager;
import android.os.Binder;
import android.os.IBinder;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.provider.Settings;
import android.util.Log;
import android.util.Slog;

import com.android.server.api.ApiLayerService;
import com.android.server.lights.Light;
import com.android.server.lights.LightsManager;
import com.pvr.IPvrManagerService;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * PICO power manager service extension (factory PICO OS 5.13.7
 * com.android.server.power.ExtPowerManagerServiceImpl), ported method by method:
 *
 * - wake-up / sleep go through {@link #proxyWakeUpInternal} / {@link #proxyGoToSleepInternal}:
 *   power LED, the power manager service wake / sleep, the API layer power state and
 *   "power_status" to pvr_manager;
 * - the power LED (pvr.system.led.type): on while awake and unplugged, low battery blinking,
 *   off (or blinking, led_flashing_when_screen_off) when asleep; the extension's battery receiver
 *   replaces the one of the power manager service;
 * - DP 5V detection (ro.pxr.dpstream.support devices only);
 * - BOOT_COMPLETED: sys.pvr.cpufre.action=1, device_provisioned=1, user_setup_complete=1;
 * - no PROXIMITY_SCREEN_OFF_WAKE_LOCK, no dim phase, no wake-up on plug / unplug;
 * - the sensor controlled auto sleep switch (pvr.factorytest.never.sleep).
 *
 * Not ported: the Smartisan quick boot check (getQBStateMachine().isInQBLightOn(), "battery
 * light is setting by quick boot") of notifyLedStatus; the quick boot layer is not in Source,
 * where that check is always false.
 * @hide
 */
public class ExtPowerManagerServiceImpl implements IExtPowerManagerService {
    /** Packages allowed to switch off the proximity-sensor controlled screen. */
    private static final List<String> CAN_NEVER_AUTO_SLEEP_LIST =
            Arrays.asList("com.pvr.lanserver");
    private static final boolean DEBUG = false;
    private static final String PROP_NEVER_AUTO_SLEEP_BY_SENSOR_DISENABLED = "0";
    private static final String PROP_NEVER_AUTO_SLEEP_BY_SENSOR_ENABLED = "1";
    private static final String PROP_NEVER_AUTO_SLEEP_MODE_BY_SENSOR =
            "pvr.factorytest.never.sleep";
    private static final String TAG = "PowerManagerService";
    /** Factory ISmtResourceControl.LAUNCH_CPUSET_EFFECTIVE_TIME. */
    private static final long READ_FILE_TIMEOUT_WARNING_MS = 2000;

    private PowerManagerService mBase;
    private Context mContext;
    /** Token of the application that switched the feature off, reset when it dies. */
    private IBinder mCurrentToken;
    private Light mLedLight;
    private IPvrManagerService mPvrManagerService;
    File mReadFile;
    BufferedReader reader;
    private Object mLock = new Object();
    private boolean mRealBootCompleted = false;
    /** Pending LED change: -1 none, 0 awake / unplugged, 1 asleep. */
    private int mLedStatus = -1;
    private boolean isPowerLedOn = false;
    private boolean isDPDevice = PROP_NEVER_AUTO_SLEEP_BY_SENSOR_ENABLED.equals(
            SystemProperties.get("ro.pxr.dpstream.support", "-1"));
    String volutagePath = "/sys/kernel/debug/qpnp-smbcharger/dc_input";
    String DPStatePath = "/sys/class/android_usb/android1/state";
    String readString = null;
    String dpChargeVolutage = "";
    String dpState = "";
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
     * Resets the sensor control screen feature, takes the battery light and registers the PICO
     * battery receiver (in place of the one of the power manager service, hence true) and the
     * boot receiver.
     */
    @Override
    public boolean systemReady(Context context, LightsManager lightsManager) {
        synchronized (mLock) {
            changeFeatureSwitch(true);
        }
        mContext = context;
        mLedLight = lightsManager.getLight(LightsManager.LIGHT_ID_BATTERY);
        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_POWER_CONNECTED);
        filter.addAction(Intent.ACTION_POWER_DISCONNECTED);
        filter.addAction(Intent.ACTION_BATTERY_CHANGED);
        filter.setPriority(IntentFilter.SYSTEM_HIGH_PRIORITY);
        context.registerReceiver(new BatteryReceiver(), filter, null, mBase.mHandler);
        IntentFilter filter2 = new IntentFilter();
        filter2.addAction(Intent.ACTION_BOOT_COMPLETED);
        context.registerReceiver(new BootReceiver(), filter2, null, mBase.mHandler);
        updateLedStatus();
        return true;
    }

    private void updateLedStatus() {
        mRealBootCompleted = true;
        notifyLedStatus();
    }

    @Override
    public boolean proxyWakeUpInternal(long eventTime, int reason, String details, int uid,
            String opPackageName, int opUid) {
        synchronized (mBase.mLock) {
            if (!mBase.mSystemReady) {
                return true;
            }
            if (!mBase.mBatteryManagerInternal.isPowered(BatteryManager.BATTERY_PLUGGED_ANY)
                    || isDP5VPlug()) {
                mLedStatus = 0;
                notifyLedStatus(0);
            }
            if (mBase.wakeUpNoUpdateLocked(eventTime, reason, details, uid, opPackageName,
                    opUid)) {
                mBase.updatePowerStateLocked();
                ApiLayerService.getInstance().updatePowerState(
                        ApiLayerService.POWER_STATE_WAKE_UP);
                if (mPvrManagerService == null || mPvrManagerService.asBinder() == null
                        || !mPvrManagerService.asBinder().isBinderAlive()) {
                    if (ServiceManager.getService("pvr_manager") != null) {
                        mPvrManagerService = IPvrManagerService.Stub.asInterface(
                                ServiceManager.getService("pvr_manager"));
                    } else {
                        Slog.w(TAG, "pvr_manager has not been added to ServiceManager,do"
                                + " nothing.");
                    }
                }
                if (mPvrManagerService != null) {
                    try {
                        mPvrManagerService.sendPvrMessages("power_status", "wakeUp");
                    } catch (Exception e) {
                        Slog.e(TAG, "mPvrManagerService sendPvrMessages error");
                    }
                }
            }
            return true;
        }
    }

    @Override
    public void notifyLedStatus() {
        notifyLedStatus(-1);
    }

    /**
     * Applies the pending LED change (mLedStatus). status is the battery status of a
     * BATTERY_CHANGED broadcast, 0 / 1 for a wake-up / sleep, or -1.
     */
    private void notifyLedStatus(int status) {
        int ledType;
        if (!mRealBootCompleted || mLedStatus == -1) {
            return;
        }
        if (mBase.mBatteryManagerInternal.isPowered(BatteryManager.BATTERY_PLUGGED_ANY)
                && (status == -1 || status == BatteryManager.BATTERY_STATUS_FULL)) {
            mLedStatus = -1;
            return;
        }
        if (SystemProperties.getInt("picovr.cit.status", 0) == 1
                || SystemProperties.getInt(ShutdownThread.SHUTDOWN_ACTION_PROPERTY, -1) > -1
                || (ledType = SystemProperties.getInt("pvr.system.led.type", 99)) == 99) {
            return;
        }
        int batteryLevel = mBase.mBatteryManagerInternal.getBatteryLevel();
        int i = mLedStatus;
        if (i == 0) {
            int picoextRGB = 0;
            int picoextRGBBlinkMode = 0;
            int blinkOnMS = 0;
            int blinkOffMs = 0;
            if (ledType == 1) {
                blinkOnMS = 500;
                blinkOffMs = 1000;
                if (batteryLevel < 20) {
                    picoextRGB = 0xF1500000;
                    picoextRGBBlinkMode = 0xF00F;
                } else if (!mBase.isInteractiveInternal() && !isDP5VPlug()) {
                    mLedLight.setFlashing(0xF1000000, 0xF, 0, 0);
                } else {
                    picoextRGB = 0xF10000FF;
                    picoextRGBBlinkMode = 0xF;
                    blinkOnMS = 500;
                    blinkOffMs = 0;
                }
            } else if (ledType == 4) {
                if (batteryLevel <= 25) {
                    picoextRGB = 0xF10000FF;
                    picoextRGBBlinkMode = 0xF;
                } else if (batteryLevel <= 50) {
                    picoextRGB = 0xF100FFFF;
                    picoextRGBBlinkMode = 0xF;
                } else if (batteryLevel > 75 && batteryLevel <= 100) {
                    picoextRGB = 0xF1FFFFFF;
                    picoextRGBBlinkMode = 0xF;
                } else {
                    picoextRGB = 0xF1FFFFFF;
                    picoextRGBBlinkMode = 0xF;
                }
            }
            mLedLight.setFlashing(picoextRGB, picoextRGBBlinkMode, blinkOnMS, blinkOffMs);
            isPowerLedOn = true;
        } else if (i == 1) {
            Context context = mContext;
            if (context == null || Settings.Global.getInt(context.getContentResolver(),
                    "led_flashing_when_screen_off", 0) != 1 || batteryLevel >= 20) {
                mLedLight.setFlashing(0xF1000000, 0xF, 0, 0);
                isPowerLedOn = false;
            } else {
                mLedLight.setFlashing(0xF1500000, 0xF00F, 500, 1000);
            }
        }
        mLedStatus = -1;
    }

    /** DP devices only: the DP cable powers the headset (dc_input 0x00, USB configured). */
    private boolean isDP5VPlug() {
        if (!isDPDevice) {
            return false;
        }
        dpChargeVolutage = readFileByLines(volutagePath);
        dpState = readFileByLines(DPStatePath);
        String str = dpChargeVolutage;
        return str != null && dpState != null && "0x00".equalsIgnoreCase(str)
                && "configured".equalsIgnoreCase(dpState);
    }

    /** First line of a file, read on an AsyncTask with a 2 s wait. */
    private String readFileByLines(final String fileName) {
        mReadFile = new File(fileName);
        if (!mReadFile.exists()) {
            return null;
        }
        final CountDownLatch latch = new CountDownLatch(1);
        AsyncTask task = new AsyncTask() {
            @Override
            protected Object doInBackground(Object[] objects) {
                reader = null;
                try {
                    reader = new BufferedReader(new FileReader(mReadFile));
                    readString = reader.readLine();
                    reader.close();
                } catch (IOException e) {
                    readString = null;
                } finally {
                    latch.countDown();
                    if (reader != null) {
                        try {
                            reader.close();
                        } catch (IOException e) {
                        }
                    }
                }
                return true;
            }
        };
        task.execute(new Object[0]);
        try {
            long latchAwaitStartTime = SystemClock.uptimeMillis();
            latch.await(2L, TimeUnit.SECONDS);
            if (SystemClock.uptimeMillis() - latchAwaitStartTime > READ_FILE_TIMEOUT_WARNING_MS) {
                Log.w(TAG, "It seems that read operation for file:" + fileName
                        + " has reached a timeout");
            }
        } catch (InterruptedException e) {
            Log.e(TAG, "InterruptedException when CountDownLatch await.", e);
        }
        return readString;
    }

    private final class BootReceiver extends BroadcastReceiver {
        private BootReceiver() {
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            SystemProperties.set("sys.pvr.cpufre.action", PROP_NEVER_AUTO_SLEEP_BY_SENSOR_ENABLED);
            Settings.Global.putInt(context.getContentResolver(),
                    Settings.Global.DEVICE_PROVISIONED, 1);
            Settings.Secure.putInt(context.getContentResolver(),
                    Settings.Secure.USER_SETUP_COMPLETE, 1);
        }
    }

    /**
     * Returns false when it handled the sleep itself; the factory always returns true, so the
     * power manager service then runs its own (by then no-op) sleep path as well.
     */
    @Override
    public boolean proxyGoToSleepInternal(long eventTime, int reason, int flags, int uid,
            PowerManagerService.NativeWrapper nativeWrapper) {
        synchronized (mBase.mLock) {
            if (!mBase.mSystemReady) {
                return true;
            }
            if (!mBase.mBatteryManagerInternal.isPowered(BatteryManager.BATTERY_PLUGGED_ANY)
                    || isDP5VPlug()) {
                mLedStatus = 1;
                notifyLedStatus(1);
            }
            if (mBase.goToSleepNoUpdateLocked(eventTime, reason, flags, uid)) {
                mBase.updatePowerStateLocked();
                ApiLayerService.getInstance().updatePowerState(
                        ApiLayerService.POWER_STATE_GO_TO_SLEEP);
                if (mPvrManagerService == null || mPvrManagerService.asBinder() == null
                        || !mPvrManagerService.asBinder().isBinderAlive()) {
                    if (ServiceManager.getService("pvr_manager") != null) {
                        mPvrManagerService = IPvrManagerService.Stub.asInterface(
                                ServiceManager.getService("pvr_manager"));
                    } else {
                        Slog.w(TAG, "pvr_manager has not been added to ServiceManager,do"
                                + " nothing.");
                    }
                }
                if (mPvrManagerService != null) {
                    try {
                        mPvrManagerService.sendPvrMessages("power_status", "goToSleep");
                    } catch (Exception e) {
                        Slog.e(TAG, "mPvrManagerService sendPvrMessages error");
                    }
                }
            }
            return true;
        }
    }

    /**
     * Replaces the battery receiver of the power manager service. With led type 1 (this
     * product) a BATTERY_CHANGED broadcast only updates the LED; only led type 4 with the LED on
     * passes it on to the power manager service, as on the factory.
     */
    final class BatteryReceiver extends BroadcastReceiver {
        BatteryReceiver() {
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            synchronized (mBase.mLock) {
                if (!mBase.mSystemReady) {
                    return;
                }
                if (Intent.ACTION_POWER_CONNECTED.equals(intent.getAction())) {
                    mLedStatus = -1;
                } else if (Intent.ACTION_POWER_DISCONNECTED.equals(intent.getAction())) {
                    mLedStatus = 0;
                    notifyLedStatus();
                } else {
                    int ledType = SystemProperties.getInt("pvr.system.led.type", 4);
                    if (ledType != 1) {
                        if (ledType == 4 && isPowerLedOn) {
                            mLedStatus = 0;
                            mBase.handleBatteryStateChangedLocked();
                        }
                    } else {
                        Log.d(TAG, "battery change is " + intent.getIntExtra(
                                BatteryManager.EXTRA_STATUS, 1));
                        if (intent.getIntExtra(BatteryManager.EXTRA_STATUS, 1)
                                != BatteryManager.BATTERY_STATUS_CHARGING) {
                            if (mBase.mBatteryManagerInternal.isPowered(
                                    BatteryManager.BATTERY_PLUGGED_ANY)
                                    || mBase.isInteractiveInternal()) {
                                mLedStatus = 0;
                                notifyLedStatus(intent.getIntExtra(
                                        BatteryManager.EXTRA_STATUS, 1));
                            } else {
                                mLedStatus = -1;
                                notifyLedStatus();
                            }
                        }
                    }
                }
            }
        }
    }

    @Override
    public boolean unplugTurnsOnScreenConfig(boolean wakeUpWhenPluggedOrUnpluggedConfig) {
        return false;
    }

    @Override
    public boolean allowTheaterModeWakeFromUnplugConfig(
            boolean wakeUpWhenPluggedOrUnpluggedInTheaterModeConfig) {
        return false;
    }

    @Override
    public boolean proximityScreenOffWakeLock() {
        return true;
    }

    @Override
    public boolean disableDimPowerState() {
        return true;
    }

    @Override
    public void setSensorControlScreenFeatureState(boolean opened, IBinder appToken,
            String packageName) {
        if (!CAN_NEVER_AUTO_SLEEP_LIST.contains(packageName)) {
            throw new SecurityException(packageName
                    + " can not change sensor controller screen state!");
        }
        mContext.enforceCallingOrSelfPermission(android.Manifest.permission.DEVICE_POWER,
                "setSensorControlScreenFeatureState");
        Log.i(TAG, "setSensorControlScreenFeatureState opened = " + opened
                + ",pid=" + Binder.getCallingPid() + ",uid=" + Binder.getCallingUid());
        synchronized (mLock) {
            changeFeatureSwitch(opened);
            linkToDeath(opened, appToken);
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
    private void linkToDeath(boolean opened, IBinder appToken) {
        if (mCurrentToken != null) {
            Log.i(TAG, "unlinkToDeath mCurrentToken = " + mCurrentToken);
            mCurrentToken.unlinkToDeath(mDeathRecipient, 0);
        }
        if (opened) {
            mCurrentToken = null;
            return;
        }
        mCurrentToken = appToken;
        IBinder iBinder = mCurrentToken;
        if (iBinder != null && iBinder.isBinderAlive()) {
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
