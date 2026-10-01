// Copyright 2026 Picomisu contributors
// SPDX-License-Identifier: Apache-2.0
package com.android.server;

import android.app.ActivityManagerInternal;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;
import android.os.Handler;
import android.os.PowerManager;
import android.os.ServiceManager;
import android.os.SystemProperties;
import android.util.Log;
import android.util.Slog;

import com.android.server.lights.Light;
import com.android.server.power.ShutdownThread;
import com.pvr.pxrnotification.aidl.IPxrNotificationService;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

/**
 * PICO battery service extension (factory PICO OS 5.13.7 com.android.server
 * .ExtBatteryServiceImpl), ported method by method:
 *
 * - the charging LED (pvr.system.led.type; red blink on a battery temperature warning). It
 *   always returns true, so the AOSP BatteryService LED logic never runs; the LED while
 *   discharging belongs to ExtPowerManagerServiceImpl;
 * - sys.pxr.lowbatterymode and the pxr_notification "lowbatterymode_changed" message;
 * - battery temperature warnings shown by com.pvr.vrdisplay (action_type 70, type 1..4);
 * - low battery shutdown: below 1 %, or with the screen off, not charging, at most 4 % and
 *   below the level at screen-off (also checked 60 s after the screen goes off);
 * - the OTG current limit on phoenix ({@link OTGLimitController}).
 *
 * Not ported: the Smartisan quick boot LED check (getQBStateMachine().isInQBLightOn(), "battery
 * light is setting by quick boot"); the quick boot layer is not in Source.
 * @hide
 */
public class ExtBatteryServiceImpl implements IExtBatteryService {
    public static final String TAG = "BatteryServiceExt";
    private ActivityManagerInternal mActivityManagerInternal;
    private BatteryService mBase;
    private Context mContext;
    private Handler mHandler;
    private OTGLimitController mOtgLimitController;
    PowerManager mPowerManager;
    File mReadFile;
    BufferedReader reader;
    public static boolean OTG_DISABLED =
            SystemProperties.getBoolean("persist.pvr.otg_limit_disabled", false);
    /** The factory compares the build project with "phoenix" as a compile-time constant. */
    private static final String BUILD_PROJECT = android.pico.utils.Features.PROJECT_PHOENIX;
    private static boolean sIsPhx =
            android.pico.utils.Features.PROJECT_PHOENIX.equals(BUILD_PROJECT);
    private boolean hasShowBatteryShut = false;
    private boolean hasShowBatteryLimit = false;
    private int batteryMode = -1;
    private int lowbatteryresotre = SystemProperties.getInt("persist.pxr.lowbattery.test2", 5);
    private IPxrNotificationService mIPxrNotificationService = null;
    private boolean isBatteryTempWarning = false;
    private boolean mRealBootCompleted = false;
    String volutagePath = "/sys/kernel/debug/qpnp-smbcharger/dc_input";
    String DPStatePath = "/sys/class/android_usb/android1/state";
    String readString = null;
    String dpChargeVolutage = "";
    String dpState = "";
    private int MIN_BATTERY_LEVEL_SHUTDOWN = 4;
    BroadcastReceiver mScreenReceiver = null;
    private int mOffBatteryLevel = -1;
    boolean mIsScreenOn = true;
    private Runnable mShutdownCallback = new Runnable() {
        @Override
        public void run() {
            Slog.w(TAG, "checkLowPowerByScreenState shutdownDirectly");
            mBase.shutdownDirectly();
        }
    };

    public ExtBatteryServiceImpl(BatteryService base, Context context, ActivityManagerInternal am,
            Handler handler) {
        mBase = base;
        mContext = context;
        mActivityManagerInternal = am;
        mHandler = handler;
        mPowerManager = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        if (isOTGLimitFeatureEnable()) {
            mOtgLimitController = new OTGLimitController(context, mBase);
        }
        checkLowPowerByScreenState();
    }

    private boolean isOTGLimitFeatureEnable() {
        return sIsPhx && !OTG_DISABLED;
    }

    @Override
    public void onStart() {
        new IntentFilter();
        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_BOOT_COMPLETED);
        filter.addAction(Intent.ACTION_USER_SWITCHED);
        mContext.registerReceiver(new BootReceiver(), filter, null, mHandler);
    }

    @Override
    public void onBatteryChanged() {
        if (isOTGLimitFeatureEnable()) {
            mContext.getMainThreadHandler().post(() -> mOtgLimitController.onBatteryChanged());
        }
    }

    /** Boot completed / user switched: the LED may now be set. */
    private final class BootReceiver extends BroadcastReceiver {
        private BootReceiver() {
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            mRealBootCompleted = true;
            synchronized (mBase.mLock) {
                mBase.mLed.updateLightsLocked();
            }
        }
    }

    @Override
    public void onProcessValuesLocked() {
        checkBatteryStatus();
    }

    private void notifyLowBatteryModeChange(int mode) {
        try {
            if (mIPxrNotificationService == null) {
                mIPxrNotificationService = IPxrNotificationService.Stub.asInterface(
                        ServiceManager.getService("pxr_notification"));
            }
            if (mIPxrNotificationService == null) {
                Log.e(TAG, "pxr_notification is null ,do nothing ...");
                return;
            }
            Log.w(TAG, "notifyLowBatteryModeChange mode " + mode);
            mIPxrNotificationService.sendPxrMessage("lowbatterymode_changed", 0, "lowbattery",
                    mode, "changed");
        } catch (Exception e) {
            Log.e(TAG, "invoke pxrnotification notifyLowBatteryModeChange Exception:" + e);
        }
    }

    private void checkBatteryStatus() {
        int testlowbattery = SystemProperties.getInt("persist.pxr.lowbattery.test", 20);
        if (!mRealBootCompleted) {
            if (mBase.mHealthInfo.batteryLevel < testlowbattery) {
                SystemProperties.set("sys.pxr.lowbatterymode", "1");
            }
        } else if (batteryMode != 1 && mBase.mHealthInfo.batteryLevel < testlowbattery) {
            batteryMode = 1;
            SystemProperties.set("sys.pxr.lowbatterymode", "1");
            notifyLowBatteryModeChange(1);
        } else if (batteryMode != 0
                && mBase.mHealthInfo.batteryLevel > lowbatteryresotre + testlowbattery) {
            batteryMode = 0;
            SystemProperties.set("sys.pxr.lowbatterymode", "0");
            notifyLowBatteryModeChange(0);
        }
        if (mBase.mHealthInfo.batteryTemperature >= 580) {
            if (hasShowBatteryShut) {
                return;
            }
            hasShowBatteryShut = true;
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    if (mActivityManagerInternal.isSystemReady()) {
                        Slog.d(TAG, "battery checkBatteryStatus for battery over temp shutdown ui");
                        Intent intent = new Intent();
                        intent.setAction("pvr.intent.action.vrdisplay");
                        intent.setPackage("com.pvr.vrdisplay");
                        intent.putExtra("action_type", 70);
                        intent.putExtra("type", 3);
                        mContext.startService(intent);
                    }
                }
            });
        } else {
            hasShowBatteryShut = false;
        }
        if (mBase.mHealthInfo.batteryTemperature >= 450
                && mBase.mHealthInfo.batteryTemperature < 500) {
            isBatteryTempWarning = true;
            Slog.d(TAG, "battery checkBatteryStatus charger is "
                    + mBase.mHealthInfo.chargerAcOnline + " "
                    + mBase.mHealthInfo.chargerUsbOnline + " "
                    + mBase.mHealthInfo.chargerWirelessOnline);
            if (hasShowBatteryLimit) {
                return;
            }
            if (!mBase.mHealthInfo.chargerAcOnline && !mBase.mHealthInfo.chargerUsbOnline
                    && !mBase.mHealthInfo.chargerWirelessOnline) {
                return;
            }
            hasShowBatteryLimit = true;
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    if (mActivityManagerInternal.isSystemReady()) {
                        Slog.d(TAG, "battery checkBatteryStatus for battery high temp limit charge");
                        Intent intent = new Intent();
                        intent.setAction("pvr.intent.action.vrdisplay");
                        intent.setPackage("com.pvr.vrdisplay");
                        intent.putExtra("action_type", 70);
                        intent.putExtra("type", 4);
                        mContext.startService(intent);
                    }
                }
            });
            return;
        }
        if (mBase.mHealthInfo.batteryTemperature >= 500
                && mBase.mHealthInfo.batteryTemperature < 580) {
            isBatteryTempWarning = true;
            hasShowBatteryLimit = false;
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    if (mActivityManagerInternal.isSystemReady()) {
                        Slog.d(TAG, "battery checkBatteryStatus for battery high temp no charge");
                        Intent intent = new Intent();
                        intent.setAction("pvr.intent.action.vrdisplay");
                        intent.setPackage("com.pvr.vrdisplay");
                        intent.putExtra("action_type", 70);
                        intent.putExtra("type", 1);
                        mContext.startService(intent);
                    }
                }
            });
        } else if (mBase.mHealthInfo.batteryTemperature <= 0) {
            isBatteryTempWarning = true;
            hasShowBatteryLimit = false;
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    if (mActivityManagerInternal.isSystemReady()) {
                        Slog.d(TAG, "battery checkBatteryStatus for battery low temp no charge");
                        Intent intent = new Intent();
                        intent.setAction("pvr.intent.action.vrdisplay");
                        intent.setPackage("com.pvr.vrdisplay");
                        intent.putExtra("action_type", 70);
                        intent.putExtra("type", 2);
                        mContext.startService(intent);
                    }
                }
            });
        } else {
            isBatteryTempWarning = false;
            hasShowBatteryLimit = false;
        }
    }

    /**
     * The PICO charging LED; returns true so that the AOSP battery LED logic never runs. The LED
     * is only set while charging or full, after boot, outside CIT and shutdown and without DP 5V.
     */
    @Override
    public boolean updateLightsLocked(Light batteryLight) {
        int level = mBase.mHealthInfo.batteryLevel;
        int status = mBase.mHealthInfo.batteryStatus;
        boolean isShutdowning =
                SystemProperties.getInt(ShutdownThread.SHUTDOWN_ACTION_PROPERTY, -1) > -1;
        boolean isCit = SystemProperties.getInt("picovr.cit.status", 0) == 1;
        int ledType = SystemProperties.getInt("pvr.system.led.type", 1);
        if (isBatteryTempWarning) {
            batteryLight.setFlashing(0xF1500000, 0xF00F, 500, 2000);
        } else if (mRealBootCompleted && !isCit && !isShutdowning
                && (status == BatteryManager.BATTERY_STATUS_CHARGING
                        || status == BatteryManager.BATTERY_STATUS_FULL)) {
            // Smartisan (factory)
            if (SysOptBridge.getFactory().getQBStateMachine().isInQBLightOn()) {
                Slog.d(TAG, "battery light is setting by quick boot. return!");
                return true;
            }
            if (isDP5VPlug()) {
                return true;
            }
            int picoextRGB = 0;
            int picoextRGBBlinkMode = 0;
            int blinkOnMS = 500;
            int blinkOffMs = 1000;
            if (ledType == 1) {
                blinkOnMS = 0;
                blinkOffMs = 0;
                if (level <= 20) {
                    picoextRGB = 0xF1500000;
                    picoextRGBBlinkMode = 0xF;
                } else if (level > 20 && level <= 98) {
                    picoextRGB = 0xF1505000;
                    picoextRGBBlinkMode = 0xF;
                } else {
                    picoextRGB = 0xF1005000;
                    picoextRGBBlinkMode = 0xF;
                }
            } else if (ledType != 4) {
                if (ledType == 99) {
                    blinkOnMS = 0;
                    blinkOffMs = 0;
                    if (level <= 50) {
                        picoextRGB = 0xF1500000;
                        picoextRGBBlinkMode = 0xF;
                    } else if (level > 50 && level <= 90) {
                        picoextRGB = 0xF1000050;
                        picoextRGBBlinkMode = 0xF;
                    } else {
                        picoextRGB = 0xF1005000;
                        picoextRGBBlinkMode = 0xF;
                    }
                }
            } else if (level <= 25) {
                picoextRGB = 0xFF;
                picoextRGBBlinkMode = 0xFF;
            } else if (level > 25 && level <= 50) {
                picoextRGB = 0xFFFF;
                picoextRGBBlinkMode = 0xF0F;
            } else if (level > 50 && level <= 75) {
                picoextRGB = 0xFFFFFF;
                picoextRGBBlinkMode = 0xF00F;
            } else if (level > 75 && level < 100) {
                picoextRGB = -1;
                picoextRGBBlinkMode = 0xF000F;
            } else {
                picoextRGB = -1;
                picoextRGBBlinkMode = 0xF;
                blinkOnMS = 0;
                blinkOffMs = 0;
            }
            batteryLight.setFlashing(picoextRGB, picoextRGBBlinkMode, blinkOnMS, blinkOffMs);
        }
        return true;
    }

    /** The DP cable powers the headset (dc_input 0x00, USB configured). */
    private boolean isDP5VPlug() {
        dpChargeVolutage = readFileByLines(volutagePath);
        dpState = readFileByLines(DPStatePath);
        String str = dpChargeVolutage;
        return str != null && dpState != null && str.equalsIgnoreCase("0x00")
                && dpState.equalsIgnoreCase("configured");
    }

    private String readFileByLines(String fileName) {
        mReadFile = new File(fileName);
        if (!mReadFile.exists()) {
            return null;
        }
        reader = null;
        try {
            reader = new BufferedReader(new FileReader(mReadFile));
            readString = reader.readLine();
            reader.close();
        } catch (IOException e) {
            readString = null;
            e.printStackTrace();
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                }
            }
        }
        return readString;
    }

    /**
     * Screen off at low battery while not charging: shut down after 60 s unless the screen comes
     * back on.
     */
    private void checkLowPowerByScreenState() {
        mScreenReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                if (Intent.ACTION_SCREEN_ON.equals(action)) {
                    mIsScreenOn = true;
                    mOffBatteryLevel = -1;
                    Slog.i(TAG, "checkLowPowerByScreenState remove shutdown callback");
                    mHandler.removeCallbacks(mShutdownCallback);
                    return;
                }
                if (Intent.ACTION_SCREEN_OFF.equals(action)) {
                    mIsScreenOn = false;
                    int batteryLevel = mBase.mHealthInfo.batteryLevel;
                    mOffBatteryLevel = batteryLevel;
                    boolean isCharging = mBase.mHealthInfo.batteryStatus
                            == BatteryManager.BATTERY_STATUS_CHARGING;
                    Slog.i(TAG, "checkLowPowerByScreenState batteryLevel=" + batteryLevel
                            + ",batteryStatus=" + mBase.mHealthInfo.batteryStatus
                            + ",isCharging=" + isCharging);
                    if (!isCharging && batteryLevel <= MIN_BATTERY_LEVEL_SHUTDOWN) {
                        Slog.i(TAG, "checkLowPowerByScreenState add shutdown callback");
                        mHandler.postDelayed(mShutdownCallback, 60000L);
                    }
                }
            }
        };
        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_SCREEN_ON);
        filter.addAction(Intent.ACTION_SCREEN_OFF);
        mContext.registerReceiver(mScreenReceiver, filter);
    }

    @Override
    public boolean shouldShutdownLocked() {
        if (mBase.mHealthInfo.batteryLevel < 1
                && SystemProperties.getInt("persist.pxr.shutdown_lowpower", 1) == 1) {
            Slog.i(TAG, "shouldShutdownLocked batteryLevel=" + mBase.mHealthInfo.batteryLevel
                    + " should shutdown");
            return true;
        }
        if (mIsScreenOn) {
            Slog.i(TAG, "shouldShutdownLocked mIsScreenOn=" + mIsScreenOn + " no need shutdown");
            return false;
        }
        if (mBase.mHealthInfo.batteryLevel > MIN_BATTERY_LEVEL_SHUTDOWN
                || mBase.mHealthInfo.batteryLevel >= mOffBatteryLevel) {
            Slog.i(TAG, "shouldShutdownLocked batteryLevel=" + mBase.mHealthInfo.batteryLevel
                    + ",mOffBatteryLevel=" + mOffBatteryLevel + " no need shutdown");
            return false;
        }
        boolean isCharging =
                mBase.mHealthInfo.batteryStatus == BatteryManager.BATTERY_STATUS_CHARGING;
        if (isCharging) {
            Slog.i(TAG, "shouldShutdownLocked isCharging=" + isCharging + " no need shutdown");
            return false;
        }
        Slog.i(TAG, "shouldShutdownLocked mIsScreenOn=" + mIsScreenOn + ",batteryLevel="
                + mBase.mHealthInfo.batteryLevel + ",batteryStatus="
                + mBase.mHealthInfo.batteryStatus + ",isChanging=" + isCharging);
        return true;
    }
}
