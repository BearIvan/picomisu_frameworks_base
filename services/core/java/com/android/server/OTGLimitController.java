// Copyright 2026 Picomisu contributors
// SPDX-License-Identifier: Apache-2.0
package com.android.server;

import android.app.KeyguardManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbManager;
import android.os.BatteryManager;
import android.os.Build;
import android.os.HandlerThread;
import android.os.PowerManager;
import android.os.SystemProperties;
import android.os.storage.StorageManager;
import android.os.storage.VolumeInfo;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

/**
 * OTG power limit of the PICO headset (factory PICO OS 5.13.7
 * com.android.server.OTGLimitController), used by {@link ExtBatteryServiceImpl} on phoenix
 * unless persist.pvr.otg_limit_disabled. With a USB device attached, OTG current is limited to
 * 500 mA while charging or at low battery (<= 15 %) and switched off (USB storage unmounted,
 * com.pvr.vrdisplay tips) at low battery without a charger; otherwise 1.5 A. A tips dialog is
 * shown 10 s after attach when the device draws more than 900 mA while discharging, unless
 * high_power_otg_enable.
 * @hide
 */
public class OTGLimitController {
    public static boolean DEBUG_LOW_BATTERY =
            SystemProperties.getBoolean("persist.pvr.otg.debug", false);
    private static boolean DEBUG_OTG = Build.IS_DEBUGGABLE | DEBUG_LOW_BATTERY;
    private static final String NODE_PATH_OTG_CURRENT_NOW =
            "/sys/class/power_supply/usb/input_current_now";
    public static final String NODE_PATH_OTG_ENABLED =
            "/sys/kernel/debug/qpnp-smbcharger/otg_disable";
    public static final String NODE_PATH_OTG_LIMITED =
            "/sys/kernel/debug/qpnp-smbcharger/otg_current_limit";
    private static final int STATUS_CHARGING = 1;
    private static final int STATUS_LOW_BATTERY = 16;
    public static final String TAG = "OTGLimitController";
    /** Factory JobStatus.DEFAULT_TRIGGER_UPDATE_DELAY. */
    private static final long CHECK_HIGH_POWER_MODE_DELAY_MS = 10 * 1000;
    /** Factory ISmtResourceControl.LAUNCH_CPUSET_EFFECTIVE_TIME. */
    private static final long USB_ATTACHED_UPDATE_DELAY_MS = 2000;
    private BatteryService mBatteryService;
    private Context mContext;
    private boolean mIsOTGDisabled;
    private boolean mIsOTGLimited;
    private KeyguardManager mKeyguardManager;
    private PowerManager mPowerManager;
    private MonitorReceiver mReceiver;
    private int mStatus;
    private StorageManager mStorageManager;
    private int mUsbCount;
    private long usbAttachedTime;
    private CheckOTGHighPowerModeRunnable mPowerModeRunnable = new CheckOTGHighPowerModeRunnable();
    private HandlerThread mWorkThread = new HandlerThread(TAG);

    public OTGLimitController(Context context, BatteryService batteryService) {
        mContext = context;
        mBatteryService = batteryService;
        mWorkThread.start();
        IntentFilter filter = new IntentFilter();
        filter.addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED);
        filter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED);
        filter.addAction(Intent.ACTION_SCREEN_ON);
        filter.addAction(Intent.ACTION_USER_PRESENT);
        mReceiver = new MonitorReceiver();
        mContext.registerReceiver(mReceiver, filter);
        mWorkThread.getThreadHandler().post(() -> writeNode(NODE_PATH_OTG_LIMITED, "1500000"));
    }

    private int getCurrentStatus() {
        int status = 0;
        if (isCharging()) {
            status = 0 | STATUS_CHARGING;
        }
        if (isLowBatteryLevel()) {
            return status | STATUS_LOW_BATTERY;
        }
        return status;
    }

    private void showUI(boolean dialogui) {
        Log.i(TAG, "showUI: isDialogUI=" + dialogui);
        try {
            Intent intent = new Intent();
            if (dialogui) {
                intent.setPackage("com.pvr.vrdisplay");
                intent.setAction("pvrdisplay.intent.action.OTG_TIPS_DIALOG");
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                mContext.startActivity(intent);
            } else {
                intent.setAction("pvr.intent.action.vrdisplay");
                intent.setPackage("com.pvr.vrdisplay");
                intent.putExtra("action_type", 1000);
                mContext.startService(intent);
            }
        } catch (Exception e) {
            Log.e(TAG, "showUI: error=" + e);
        }
    }

    void onBatteryChanged() {
        int status = getCurrentStatus();
        if ((mStatus ^ status) > 0) {
            if (hasUsbDevice()) {
                if (isCharging() || isLowBatteryLevel()) {
                    mWorkThread.getThreadHandler().post(() -> mPowerModeRunnable.reset(false));
                } else {
                    mWorkThread.getThreadHandler().removeCallbacks(mPowerModeRunnable);
                    mWorkThread.getThreadHandler().postDelayed(mPowerModeRunnable,
                            CHECK_HIGH_POWER_MODE_DELAY_MS);
                }
            }
            postDelayedUpdateOTGStatus(false);
        }
    }

    void postDelayedUpdateOTGStatus(final boolean showDialogUI) {
        mContext.getMainThreadHandler().postDelayed(() -> updateOTGStatus(showDialogUI), 200L);
    }

    void postDelayedUpdateOTGStatus(final boolean showDialogUI, long delayTime) {
        mContext.getMainThreadHandler().postDelayed(() -> updateOTGStatus(showDialogUI),
                delayTime);
    }

    private void updateOTGStatus(boolean showDialogUI) {
        if (mStorageManager == null) {
            mStorageManager = (StorageManager) mContext.getSystemService(Context.STORAGE_SERVICE);
        }
        boolean hasUsbDevice = hasUsbDevice();
        boolean isLowBatteryLevel = isLowBatteryLevel();
        boolean isCharging = isCharging();
        int status = getCurrentStatus();
        if (DEBUG_OTG) {
            Log.i(TAG, "updateOTGStatus: hasUsbDevice=" + hasUsbDevice + ", isCharging="
                    + isCharging + ", isLowBatteryLevel=" + isLowBatteryLevel
                    + ",  showDialogUI=" + showDialogUI + ", lastStatus="
                    + Integer.toHexString(mStatus) + ", status=" + Integer.toHexString(status)
                    + ", time=" + (System.currentTimeMillis() - usbAttachedTime));
        }
        if (!hasUsbDevice) {
            limiteOTG(isLowBatteryLevel);
        } else if (!getPowerManager().isInteractive() || getKeyguardManager().isKeyguardLocked()) {
            Log.i(TAG, "updateOTGStatus: screen is not interactive or keyguard locked, so do"
                    + " nothing!");
            return;
        } else if (isCharging) {
            limiteOTG(isLowBatteryLevel);
        } else if (isLowBatteryLevel) {
            closeOTG(showDialogUI);
        }
        mStatus = status;
    }

    private PowerManager getPowerManager() {
        if (mPowerManager == null) {
            mPowerManager = (PowerManager) mContext.getSystemService(Context.POWER_SERVICE);
        }
        return mPowerManager;
    }

    private KeyguardManager getKeyguardManager() {
        if (mKeyguardManager == null) {
            mKeyguardManager = (KeyguardManager) mContext.getSystemService(
                    Context.KEYGUARD_SERVICE);
        }
        return mKeyguardManager;
    }

    private boolean hasUsbDevice() {
        return mUsbCount > 0;
    }

    private boolean isCharging() {
        return mBatteryService.mHealthInfo.batteryStatus
                == BatteryManager.BATTERY_STATUS_CHARGING;
    }

    private boolean isLowBatteryLevel() {
        return mBatteryService.mHealthInfo.batteryLevel <= 15 || DEBUG_LOW_BATTERY;
    }

    private void closeOTG(boolean showDialogUI) {
        if (mIsOTGDisabled) {
            return;
        }
        Log.i(TAG, "closeOTG");
        mIsOTGDisabled = true;
        showUI(showDialogUI);
        mWorkThread.getThreadHandler().post(() -> {
            umountUsbStorageDevices();
            writeNode(NODE_PATH_OTG_ENABLED, "0x00");
        });
        limiteOTG(true);
    }

    private void umountUsbStorageDevices() {
        if (mStorageManager == null) {
            mStorageManager = (StorageManager) mContext.getSystemService(Context.STORAGE_SERVICE);
        }
        List<VolumeInfo> volumeInfos = mStorageManager.getVolumes();
        for (VolumeInfo volume : volumeInfos) {
            boolean isUsb = volume.disk != null && volume.disk.isUsb();
            if (isUsb) {
                Log.i(TAG, "umountUsbStorageDevices: volume.id=" + volume.id);
                mStorageManager.unmount(volume.id);
            }
        }
    }

    private void limiteOTG(final boolean limite) {
        if (mIsOTGLimited == limite) {
            return;
        }
        boolean highPowerMode = Settings.Global.getInt(mContext.getContentResolver(),
                "high_power_otg_enable", 0) == 1;
        boolean hasUsbDevice = hasUsbDevice();
        Log.i(TAG, "limiteOTG: limite=" + limite + ", highPowerMode=" + highPowerMode
                + ", hasUsbDevice=" + hasUsbDevice);
        mIsOTGLimited = limite;
        mWorkThread.getThreadHandler().post(
                () -> writeNode(NODE_PATH_OTG_LIMITED, limite ? "500000" : "1500000"));
    }

    private void writeNode(String path, String value) {
        File file = new File(path);
        if (DEBUG_OTG) {
            Log.i(TAG, "writeNode: path=" + path + "value=" + value);
        }
        if (file.exists()) {
            try {
                FileWriter writer = new FileWriter(path, false);
                writer.write(value);
                writer.close();
                return;
            } catch (Exception e) {
                Log.e(TAG, "writeNode error, error=" + e);
                return;
            }
        }
        Log.e(TAG, "writeNode error, file not exists, path=" + path + ", value=" + value);
    }

    /** First line of a file, or null. */
    public static String readFile(String path) {
        File file = new File(path);
        FileReader reader = null;
        try {
            reader = new FileReader(file);
            char[] data = new char[1024];
            int charCount = reader.read(data);
            Log.i(TAG, "readFile(String path) : " + path + " ,count :" + charCount);
            String[] status = new String(data, 0, charCount).trim().split("\n");
            return status[0];
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    class MonitorReceiver extends BroadcastReceiver {
        MonitorReceiver() {
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (UsbManager.ACTION_USB_DEVICE_ATTACHED.equals(action)) {
                Log.i(TAG, "onUsbDeviceAttached");
                mUsbCount++;
                usbAttachedTime = System.currentTimeMillis();
                postDelayedUpdateOTGStatus(true, USB_ATTACHED_UPDATE_DELAY_MS);
                mWorkThread.getThreadHandler().removeCallbacks(mPowerModeRunnable);
                mWorkThread.getThreadHandler().postDelayed(mPowerModeRunnable,
                        CHECK_HIGH_POWER_MODE_DELAY_MS);
                return;
            }
            if (UsbManager.ACTION_USB_DEVICE_DETACHED.equals(action)) {
                Log.i(TAG, "onUsbDeviceDetached");
                mUsbCount--;
                if (!hasUsbDevice()) {
                    mIsOTGDisabled = false;
                    mWorkThread.getThreadHandler().removeCallbacks(mPowerModeRunnable);
                    mWorkThread.getThreadHandler().post(() -> mPowerModeRunnable.reset(true));
                }
                return;
            }
            if (Intent.ACTION_SCREEN_ON.equals(action)) {
                Log.i(TAG, "screen on");
                postDelayedUpdateOTGStatus(true);
            } else if (Intent.ACTION_USER_PRESENT.equals(action)) {
                Log.i(TAG, "keyguard dismiss");
                postDelayedUpdateOTGStatus(true);
            }
        }
    }

    class CheckOTGHighPowerModeRunnable implements Runnable {
        CheckOTGHighPowerModeRunnable() {
        }

        @Override
        public void run() {
            boolean enable = false;
            try {
                enable = Settings.Global.getInt(mContext.getContentResolver(),
                        "high_power_otg_enable") == 1;
            } catch (Settings.SettingNotFoundException e) {
            }
            if (enable) {
                Log.i(TAG, "OTGLimit CheckOTGTipsRunnable high_power_otg_enable is true, so"
                        + " return");
                return;
            }
            try {
                if (isLimitedState()) {
                    Intent intent = new Intent("pvrdisplay.intent.action.OTG_TIPS_DIALOG");
                    intent.setPackage("com.pvr.vrdisplay");
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    intent.putExtra("action", 1);
                    mContext.startActivity(intent);
                }
            } catch (Exception e2) {
                Log.e(TAG, "OTGLimit run: error=" + e2);
            }
        }

        private boolean isLimitedState() {
            String currentStr = readFile(NODE_PATH_OTG_CURRENT_NOW);
            if (TextUtils.isEmpty(currentStr)) {
                Log.i(TAG, "isLimitedState: currentStr is empty");
                return false;
            }
            float current = (float) (((double) Float.parseFloat(currentStr)) / 1000.0d);
            int batteryStatus = mBatteryService.mHealthInfo.batteryStatus;
            Log.i(TAG, "isLimitedState: current=" + current + ", sLastBatteryStatus="
                    + batteryStatus);
            return current > 900.0f && batteryStatus == BatteryManager.BATTERY_STATUS_DISCHARGING;
        }

        void reset(boolean detached) {
            Log.i(TAG, "CheckOTGHighPowerModeRunnable reset");
            mWorkThread.getThreadHandler().removeCallbacks(this);
            if (detached) {
                mWorkThread.getThreadHandler().postDelayed(
                        () -> writeNode(NODE_PATH_OTG_LIMITED, "1500000"), 200L);
            }
        }
    }
}
