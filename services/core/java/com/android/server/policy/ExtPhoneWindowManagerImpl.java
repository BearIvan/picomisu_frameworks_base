/*
 * Copyright 2026 Picomisu contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package com.android.server.policy;

import android.app.ActivityManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.media.AudioManager;
import android.media.IAudioService;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.pico.utils.Features;
import android.provider.Settings;
import android.security.KeyStore;
import android.text.TextUtils;
import android.util.Log;
import android.util.Slog;
import android.view.KeyEvent;
import android.view.WindowManager;

import com.android.server.api.ApiLayerService;
import com.android.server.policy.WindowManagerPolicy.WindowState;
import com.android.server.wm.SystemExt;
import com.pvr.IPvrManagerService;
import com.pvr.pxrnotification.aidl.IPxrNotificationService;

import java.io.File;
import java.io.FileInputStream;
import java.util.Properties;

/**
 * PICO VR key, home and power handling of the window policy (factory PICO OS 5.13.7
 * com.android.server.policy.ExtPhoneWindowManagerImpl).
 *
 * Every key first goes through {@link #processKey}: keys are reported to pvr_manager
 * ("vr2d_key_event") and, when handled or BACK, to the API layer clients; the headset HOME key
 * and the controller HOME keys (901/902) run their configured tap / double-tap / long-press
 * actions (default: tap = global navigation, i.e. SystemExt HOME; long press = recenter), the
 * camera key (27) takes screenshots / recordings, and BACK from a non head-control device is left
 * to the API layer. HOME from anything but the gesture hands is consumed.
 *
 * Intentionally NOT ported: the factory checkADBPwd key-sequence password and the "open adb"
 * HOME + volume key combinations of checkMulKeyAction, which switch on ADB (adb_enabled) from
 * keys. The device owner has not approved enabling USB/ADB debugging from key presses.
 * Also not ported: the ToB composite key (volume down + confirm, dead code on phoenix) and
 * the Smartisan quick boot on power long press.
 */
public class ExtPhoneWindowManagerImpl {
    static final String TAG = "WindowManagerExt";
    private static final boolean DEBUG = Build.IS_DEBUGGABLE;

    private static final String HOME_DISALBE_FOR_PVR = "android.intent.disablehome_pvr";
    private static final String POWER_DISALBE_FOR_HCIT = "android.intent.disalbepower_hcit";
    private static final String USER_KEY_CONFIG_CHANGE = "android.intent.user_keyconfig_change";
    private static final String PXR_NOTIFICATION_NAME_MULTI_KEY_PRESSED =
            "pxr.notification.key.multi_key_pressed";
    private static final String NOTIFICATION_MSG_BACK_LONG_PRESS = "long.tap.back";
    private static final String NOTIFICATION_MSG_HOME_LONG_PRESS = "long.tap.home";
    private static final String SHORTCT_SHOW_ON_3D = "shortct_show_on_3d";
    private static final String SCREEN_RECORD_CAP_PACKAGE = "com.bytedance.pico.screencapture";

    // ToB key configuration files (ro.pxr.externalfunc=1 only), in lookup order.
    private static final String MDM_KEY_CONFIG_PATH = "/data/local/tmp/PxrSystemKeyConfig.prop";
    private static final String USER_KEY_CONFIG_PATH = "/data/local/tmp/SystemKeyConfig.prop";
    private static final String OEM_KEY_CONFIG_PATH = "/data/misc/pxr/PxrSystemKeyConfig.prop";
    private static final String USER_KEY_CONFIG_PATH_DEFAULT = "/system/etc/SystemKeyConfig.prop";

    // Settings.Global values kept by the factory SettingsObserverExt.
    private static final String PVR_SETUP_WIZARD_COMPLETE = "pvr.config.provision2.complete";
    private static final String SETTINGS_DISABLE_CAMERA_KEY = "pvr.app.data.disable_camera_key";
    private static final String SETTINGS_DOCK_SHOWING = "pvr.app.data.dock_visible_state";
    private static final String SETTINGS_SCREENSHOT_TOAST_SHOWING =
            "pvr.settings.screenshot_toast_showing";

    private static final int KEYCODE_LEFT_CONTROLLER_HOME = 901;
    private static final int KEYCODE_RIGHT_CONTROLLER_HOME = 902;
    private static final int KEYCODE_CONFIRM = 1001;
    private static final int KEYCODE_RECENTER = 1004;
    private static final int DEVICE_GESTURE_LEFT_HAND = 20001;
    private static final int DEVICE_GESTURE_RIGHT_HAND = 20002;
    private static final int DEVICE_HEAD_CONTROL_HANDLE_MIN = 100000;

    private static final int MSG_HMD_HOME_TAP = 101;
    private static final int MSG_LCONTROLLER_HOME_TAP = 102;
    private static final int MSG_RCONTROLLER_HOME_TAP = 103;
    private static final int MSG_RCAPTURE_TAP = 104;

    private static final int ACTION_PXR_UNDEFINED = -1;
    private static final int ACTION_PXR_GOHOME = 2;
    private static final int ACTION_PXR_DASHBOARD = 3;
    private static final int ACTION_PXR_RECENTER = 6;
    private static final int ACTION_PXR_GLOBAL_NAVIGATION = 11;
    private static final int ACTION_PXR_SCREENCAP = 104;
    private static final int ACTION_PXR_SCREENRECORD = 105;

    private static boolean sHomeDisabledForPvr = false;
    private static boolean sPowerDisabledForHcit = false;

    private final PhoneWindowManager mBase;
    private final Handler mKeyActionHandler = new SystemKeyHandler(Looper.getMainLooper());
    private final SystemKeyAction mHmdHomeKeyAction = new SystemKeyAction();
    private final SystemKeyAction mLctlHomeKeyAction = new SystemKeyAction();
    private final SystemKeyAction mRctlHomeKeyAction = new SystemKeyAction();
    private final SystemKeyAction mHmdConfirmKeyAction = new SystemKeyAction();
    private final SystemKeyAction mHmdVolumeUpKeyAction = new SystemKeyAction();
    private final SystemKeyAction mHmdVolumeDownKeyAction = new SystemKeyAction();
    private final SystemKeyAction mHmdBackKeyAction = new SystemKeyAction();
    private final SystemKeyAction mHmdPowerKeyAction = new SystemKeyAction();
    private final SystemKeyAction mRctlCaptureKeyAction = new SystemKeyAction();
    private final boolean mIsDefectiveDevice;
    private IAudioService mAudioService;
    private IPvrManagerService mPvrManagerService;
    private boolean mIsToBDevice;
    private boolean mWillSendKeyBC;
    private boolean mShortctShowOn3d;
    private long mHomeDownTime = -1;
    private long mBackDownTime = -1;

    private final BroadcastReceiver mPxrKeyActionReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            Slog.d(TAG, "mPxrKeyActionReceiver get action " + action);
            if (POWER_DISALBE_FOR_HCIT.equals(action)) {
                sPowerDisabledForHcit = intent.getBooleanExtra("state", false);
            } else if (HOME_DISALBE_FOR_PVR.equals(action)) {
                sHomeDisabledForPvr = intent.getBooleanExtra("state", false);
            } else if (USER_KEY_CONFIG_CHANGE.equals(action)) {
                updateSystemKeyConfig();
            }
        }
    };

    ExtPhoneWindowManagerImpl(PhoneWindowManager base) {
        mBase = base;
        final String product = SystemProperties.get("pxr.vendorhw.product.name");
        mIsDefectiveDevice = "A7L10".equals(product) || "A7J10".equals(product);
        mWillSendKeyBC = SystemProperties.getInt("persit.pxr.keybc.enable", 0) == 1;
        mAudioService = IAudioService.Stub.asInterface(ServiceManager.checkService("audio"));
    }

    /** End of PhoneWindowManager.init. */
    void init(Context context) {
        final IntentFilter filter = new IntentFilter();
        filter.addAction(POWER_DISALBE_FOR_HCIT);
        filter.addAction(HOME_DISALBE_FOR_PVR);
        filter.addAction(USER_KEY_CONFIG_CHANGE);
        mBase.mContext.registerReceiver(mPxrKeyActionReceiver, filter);
        updateSystemKeyConfig();
        mIsToBDevice = SystemProperties.getInt("ro.pxr.externalfunc", 0) != 0
                || Settings.Global.getInt(mBase.mContext.getContentResolver(),
                        "ro.pxr.externalfunc.test", 0) != 0;
        if (mIsDefectiveDevice) {
            mBase.mVeryLongPressTimeout = 3000;
        }
        mBase.mScreenshotChordEnabled = mBase.mScreenshotChordEnabled
                && SystemProperties.getInt("persist.pvr.screenshot.enable", 1) == 1;
    }

    void updateSystemKeyConfig() {
        Slog.d(TAG, "updateSystemKeyConfig call");
        mHmdHomeKeyAction.reset();
        mLctlHomeKeyAction.reset();
        mRctlHomeKeyAction.reset();
        mHmdConfirmKeyAction.reset();
        mHmdVolumeUpKeyAction.reset();
        mHmdVolumeDownKeyAction.reset();
        mHmdBackKeyAction.reset();
        mHmdPowerKeyAction.reset();
        mHmdHomeKeyAction.defined = 1;
        mLctlHomeKeyAction.defined = 1;
        mRctlHomeKeyAction.defined = 1;
        mRctlCaptureKeyAction.defined = 1;
        mRctlCaptureKeyAction.enableDoubleTap = false;
        mLctlHomeKeyAction.tapAction = SystemProperties.getInt("persist.pxr.lcontroller.tap",
                ACTION_PXR_GLOBAL_NAVIGATION);
        mRctlHomeKeyAction.tapAction = SystemProperties.getInt("persist.pxr.rcontroller.tap",
                ACTION_PXR_GLOBAL_NAVIGATION);
        mHmdHomeKeyAction.tapAction = SystemProperties.getInt("persist.pxr.hmd.tap",
                ACTION_PXR_GLOBAL_NAVIGATION);
        // Not phoenix only: double tap = screen record, no capture key.
        if (!Features.PROJECT_PHOENIX.equals(Features.getProjectName())) {
            mLctlHomeKeyAction.doubleTapAction = SystemProperties.getInt(
                    "persist.pxr.lcontroller.doubletap", ACTION_PXR_SCREENRECORD);
            mRctlHomeKeyAction.doubleTapAction = SystemProperties.getInt(
                    "persist.pxr.rcontroller.doubletap", ACTION_PXR_SCREENRECORD);
            mHmdHomeKeyAction.doubleTapAction = SystemProperties.getInt(
                    "persist.pxr.hmd.doubletap", ACTION_PXR_SCREENRECORD);
            mRctlCaptureKeyAction.enable = 0;
            mRctlCaptureKeyAction.defined = 0;
        }
        mLctlHomeKeyAction.longPressAction = SystemProperties.getInt(
                "persist.pxr.lcontroller.longpress", ACTION_PXR_RECENTER);
        mRctlHomeKeyAction.longPressAction = SystemProperties.getInt(
                "persist.pxr.rcontroller.longpress", ACTION_PXR_RECENTER);
        mHmdHomeKeyAction.longPressAction = SystemProperties.getInt(
                "persist.pxr.hmd.longpress", ACTION_PXR_RECENTER);
        mRctlCaptureKeyAction.longPressActionTime = SystemProperties.getInt(
                "persist.pxr.capture.longpresstime", 300);
        mRctlCaptureKeyAction.longPressAction = SystemProperties.getInt(
                "persist.pxr.capture.longpress", ACTION_PXR_SCREENRECORD);
        mWillSendKeyBC = SystemProperties.getInt("persit.pxr.keybc.enable", 0) == 1;
        updateUserKeyConfig();
    }

    private static String getPropValue(Properties prop, String key) {
        return prop.containsKey(key) ? prop.get(key).toString() : null;
    }

    /** ToB devices: key actions from the first existing key configuration file. */
    private void updateUserKeyConfig() {
        try {
            if (SystemProperties.getInt("ro.pxr.externalfunc", -1) != 1) {
                return;
            }
            File keyConfigFile = null;
            for (String path : new String[] {MDM_KEY_CONFIG_PATH, USER_KEY_CONFIG_PATH,
                    OEM_KEY_CONFIG_PATH, USER_KEY_CONFIG_PATH_DEFAULT}) {
                final File file = new File(path);
                if (file.exists()) {
                    keyConfigFile = file;
                    break;
                }
            }
            if (keyConfigFile == null) {
                Slog.i(TAG, "key config file do not exists, so we do not do any change to key"
                        + " action");
                return;
            }
            Slog.i(TAG, "we get key config file " + keyConfigFile + " so change key action");
            final Properties config = new Properties();
            try (FileInputStream in = new FileInputStream(keyConfigFile)) {
                config.load(in);
            }
            final String powerLongPressTime = getPropValue(config, "time_power_long_press");
            if (powerLongPressTime != null) {
                mHmdPowerKeyAction.defined = 1;
                mHmdPowerKeyAction.longPressActionTime = Integer.parseInt(powerLongPressTime);
            }
            if (getPropValue(config, "action_power_single_tap") != null) {
                mHmdPowerKeyAction.defined = 1;
                mHmdPowerKeyAction.tapAction = 0;
            }
            if (getPropValue(config, "action_power_long_press") != null) {
                mHmdPowerKeyAction.defined = 1;
                mHmdPowerKeyAction.longPressAction = 0;
            }
            mHmdConfirmKeyAction.defined = getPropValue(config, "action_key_enter") != null ? 1 : 0;
            mHmdVolumeUpKeyAction.defined =
                    getPropValue(config, "action_key_volumeup") != null ? 1 : 0;
            mHmdVolumeDownKeyAction.defined =
                    getPropValue(config, "action_key_volumedown") != null ? 1 : 0;
            mHmdBackKeyAction.defined = getPropValue(config, "action_key_back") != null ? 1 : 0;
            // Generic home value first, then the per-device (hmd_/lctl_/rctl_) override.
            applyHomeConfig(config, "action_home_single_tap", 0);
            applyHomeConfig(config, "action_home_double_tap", 1);
            applyHomeConfig(config, "action_home_long_press", 2);
            applyHomeConfig(config, "time_home_double_tap", 3);
            applyHomeConfig(config, "time_home_long_press", 4);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void applyHomeConfig(Properties config, String key, int field) {
        final String all = getPropValue(config, key);
        final SystemKeyAction[] actions = {mHmdHomeKeyAction, mLctlHomeKeyAction,
                mRctlHomeKeyAction};
        final String[] prefixes = {"hmd_", "lctl_", "rctl_"};
        for (int i = 0; i < actions.length; i++) {
            final String own = getPropValue(config, prefixes[i] + key);
            final String value = own != null ? own : all;
            if (value == null) {
                continue;
            }
            final int v = Integer.parseInt(value);
            switch (field) {
                case 0: actions[i].tapAction = v; break;
                case 1: actions[i].doubleTapAction = v; break;
                case 2: actions[i].longPressAction = v; break;
                case 3: actions[i].doubleTapActionTime = v; break;
                default: actions[i].longPressActionTime = v; break;
            }
        }
    }

    private boolean isMulKeyEnable() {
        final boolean inDPMode = SystemProperties.getInt("sys.pxr.vxr7200.status", 0) == 1;
        return !inDPMode && SystemProperties.getInt("persist.pvr.mulkey.enable", 1) == 1;
    }

    private void launchSettings() {
        final Intent extraIntent = new Intent();
        extraIntent.setComponent(new ComponentName("com.android.settings",
                "com.android.settings.Settings"));
        final Intent intent = new Intent("pvr.intent.action.VRSHELL");
        intent.putExtra("intent", extraIntent);
        mBase.startActivityAsUser(intent, UserHandle.CURRENT_OR_SELF);
    }

    private void sendPvrBroadCast(String action) {
        mBase.mContext.sendOrderedBroadcastAsUser(new Intent(action), UserHandle.ALL, null, null,
                null, 0, null, null);
    }

    private static IPxrNotificationService getPxrNotificationService() {
        return IPxrNotificationService.Stub.asInterface(
                ServiceManager.checkService("pxr_notification"));
    }

    /**
     * HOME long press + confirm held: BACK opens Android settings, volume down disconnects the
     * controllers. The factory's ADB password sequence and its HOME + volume "open adb"
     * combinations are intentionally not ported (see the class comment).
     */
    private void checkMulKeyAction(KeyEvent event) {
        if (!isMulKeyEnable() || !mHmdHomeKeyAction.isLongPressed
                || !mHmdConfirmKeyAction.isPressed) {
            return;
        }
        if (event.getAction() != KeyEvent.ACTION_DOWN || event.getRepeatCount() != 0) {
            return;
        }
        if (event.getKeyCode() == KeyEvent.KEYCODE_BACK) {
            Slog.d(TAG, "checkMulKeyAction do launchSettings");
            launchSettings();
        } else if (event.getKeyCode() == KeyEvent.KEYCODE_VOLUME_DOWN) {
            Slog.w(TAG, "checkMulKeyAction do disconnect controller");
            sendPvrBroadCast("android.intent.pvrcon.disconnect");
            final IPxrNotificationService notification = getPxrNotificationService();
            if (notification != null) {
                Log.w(TAG, "send pxr notification when multi key pressed");
                try {
                    notification.sendPxrMessage(PXR_NOTIFICATION_NAME_MULTI_KEY_PRESSED, -1,
                            "3;25;1001", -1, "");
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private void sendKeyBroadCast(int keycode, int action, int status) {
        final Intent keyIntent = new Intent("android.intent.keybroadcast");
        keyIntent.putExtra("keycode", keycode);
        keyIntent.putExtra("action", action);
        keyIntent.putExtra("status", status);
        mBase.mContext.sendOrderedBroadcastAsUser(keyIntent, UserHandle.ALL, null, null, null, 0,
                null, null);
    }

    private boolean checkSomeDefinedHome(String winPackage) {
        if (sHomeDisabledForPvr) {
            Slog.d(TAG, "checkSomeDefinedHome retun true by home disabled for pvr");
            return true;
        }
        if (SystemProperties.getInt("sys.pxr.vxr7200.status", 0) == 1) {
            Slog.d(TAG, "checkSomeDefinedHome retun true by vxr7200 status");
            return true;
        }
        if ("com.pvr.seethrough.setting".equals(winPackage)) {
            Slog.d(TAG, "checkSomeDefinedHome retun true by seethrough");
            return true;
        }
        if (Features.PROJECT_PHOENIX.equals(Features.getProjectName())
                && Features.isKeyguardEnabled() && mBase.isKeyguardShowingAndNotOccluded()) {
            Slog.d(TAG, "checkSomeDefinedHome retun true by Keyguard");
            return true;
        }
        return false;
    }

    private boolean getPowerDefined() {
        return mHmdPowerKeyAction.defined == 1;
    }

    /**
     * interceptKeyBeforeDispatchingInner, before anything else: 1 = continue the normal policy,
     * 0 = pass the key to the app, -1 = consume it.
     */
    int processKey(KeyEvent event, WindowState win) {
        final int keyCode = event.getKeyCode();
        final WindowManager.LayoutParams attrs = win != null ? win.getAttrs() : null;
        final boolean result = handleKey(event, attrs != null ? attrs.packageName : "unknown");
        Slog.d(TAG, "handleKey result=" + result + ",event=" + event);
        if ((result || keyCode == KeyEvent.KEYCODE_BACK) && !injectBackKeyDirectly()) {
            ApiLayerService.getInstance().onKeyEvent(event);
        }
        if (result) {
            return 1;
        }
        if (keyCode == KeyEvent.KEYCODE_BACK || keyCode == KEYCODE_CONFIRM
                || keyCode == KEYCODE_RECENTER || keyCode == KeyEvent.KEYCODE_VOLUME_UP
                || keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) {
            return -1;
        }
        return 0;
    }

    private boolean injectBackKeyDirectly() {
        return mIsToBDevice && SystemProperties.getInt("pvr.active.input_device", 0) == -1
                && SystemProperties.getBoolean("pvr.tob.inject.backkey.direct", false);
    }

    /** True: the normal policy handles the key; false: PICO handled it. */
    private boolean handleKey(KeyEvent event, String winPackage) {
        if (event.getRepeatCount() == 0 && DEBUG) {
            Slog.d(TAG, "we get the handleKey " + event.getKeyCode() + ", action "
                    + event.getAction() + ", deviceId " + event.getDeviceId());
        }
        final int keyCode = event.getKeyCode();
        sendKeyToPvrManager(event);
        checkMulKeyAction(event);
        switch (keyCode) {
            case KeyEvent.KEYCODE_HOME:
                return handleHomeKey(mHmdHomeKeyAction, event, winPackage, null);
            case KeyEvent.KEYCODE_BACK:
                if (event.getDeviceId() < DEVICE_HEAD_CONTROL_HANDLE_MIN
                        && !injectBackKeyDirectly()) {
                    return false;
                }
                return handleConfigurableKey(mHmdBackKeyAction, event);
            case KeyEvent.KEYCODE_VOLUME_UP:
                return handleConfigurableKey(mHmdVolumeUpKeyAction, event);
            case KeyEvent.KEYCODE_VOLUME_DOWN:
                return handleConfigurableKey(mHmdVolumeDownKeyAction, event);
            case KeyEvent.KEYCODE_CAMERA:
                if (mRctlCaptureKeyAction.defined != 1) {
                    return mRctlCaptureKeyAction.enable != 0;
                }
                mRctlCaptureKeyAction.handleEvent(event);
                if (mRctlHomeKeyAction.down) {
                    Slog.d(TAG, "camera key conflict with right controller key");
                    mRctlHomeKeyAction.setConflictWithOtherKeys();
                    mRctlCaptureKeyAction.setConflictWithOtherKeys();
                }
                return false;
            case KEYCODE_CONFIRM:
                if (SystemProperties.getInt("sys.pxr.vxr7200.status", 0) == 1) {
                    return false;
                }
                return handleConfigurableKey(mHmdConfirmKeyAction, event);
            case KEYCODE_RECENTER:
                if (event.getAction() == KeyEvent.ACTION_UP) {
                    Slog.w(TAG, "launchRecenter by keyevent KEYCODE_RECENTER");
                    mBase.sendCloseSystemWindows("recenter");
                }
                return false;
            case KEYCODE_LEFT_CONTROLLER_HOME:
                return handleHomeKey(mLctlHomeKeyAction, event, winPackage, null);
            case KEYCODE_RIGHT_CONTROLLER_HOME:
                return handleHomeKey(mRctlHomeKeyAction, event, winPackage,
                        mRctlCaptureKeyAction);
            default:
                return true;
        }
    }

    private boolean handleHomeKey(SystemKeyAction action, KeyEvent event, String winPackage,
            SystemKeyAction conflicting) {
        action.down = event.getAction() == KeyEvent.ACTION_DOWN;
        if (checkSomeDefinedHome(winPackage)) {
            return false;
        }
        if (action.defined != 1) {
            return action.enable != 0;
        }
        action.handleEvent(event);
        if (conflicting != null && conflicting.down) {
            Slog.d(TAG, "right controller key conflict with camera key");
            action.setConflictWithOtherKeys();
            conflicting.setConflictWithOtherKeys();
        }
        return false;
    }

    private static boolean handleConfigurableKey(SystemKeyAction action, KeyEvent event) {
        action.handleEvent(event);
        return action.defined != 1 && action.enable != 0;
    }

    /** Reports each key press / release to pvr_manager ("vr2d_key_event"). */
    private void sendKeyToPvrManager(KeyEvent event) {
        final int keyCode = event.getKeyCode();
        if (keyCode == KeyEvent.KEYCODE_BACK && event.getScanCode() == 10001) {
            return;
        }
        try {
            if (mPvrManagerService == null || mPvrManagerService.asBinder() == null
                    || !mPvrManagerService.asBinder().isBinderAlive()) {
                final IBinder binder = ServiceManager.checkService("pvr_manager");
                if (binder != null) {
                    mPvrManagerService = IPvrManagerService.Stub.asInterface(binder);
                } else {
                    Slog.w(TAG, "pvr_manager has not been added to ServiceManager,do nothing.");
                }
            }
            if (mPvrManagerService != null && event.getRepeatCount() == 0) {
                if (keyCode == KEYCODE_CONFIRM && mHmdConfirmKeyAction.defined == 1) {
                    Slog.w(TAG, "confirm key is defined so do nothing");
                } else {
                    mPvrManagerService.sendPvrMessages("vr2d_key_event",
                            event.getAction() + ":" + keyCode + ":" + event.getDeviceId());
                }
            }
            if (mWillSendKeyBC) {
                sendKeyBroadCast(keyCode, -1, event.getAction());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private boolean isSetupWizardComplete() {
        return Settings.Global.getInt(mBase.mContext.getContentResolver(),
                PVR_SETUP_WIZARD_COMPLETE, 0) != 0;
    }

    private boolean getGlobalFlag(String name) {
        return Settings.Global.getInt(mBase.mContext.getContentResolver(), name, 0) != 0;
    }

    private static boolean topIsVrPermissionActivity() {
        try {
            final ActivityManager.StackInfo stackInfo =
                    ActivityManager.getService().getFocusedStackInfo();
            return stackInfo != null && stackInfo.topActivity != null
                    && TextUtils.equals(stackInfo.topActivity.getClassName(),
                            "com.android.packageinstaller.permission.ui.pico"
                                    + ".VrGrantPermissionsActivity");
        } catch (RemoteException e) {
            e.printStackTrace();
            return false;
        }
    }

    /** Tells SystemExt that HOME was pressed (it shows the dock / home panel). */
    void dispatchHomeToNS() {
        Slog.w(TAG, "dispatchHomeToNS");
        final Intent intent = new Intent(SystemExt.sAction);
        intent.setPackage(SystemExt.sCurrentPkg);
        mBase.mContext.startService(intent);
    }

    boolean isShortctShowOn3dApp(Context context, int repeatCount) {
        if (repeatCount == 0) {
            mShortctShowOn3d = Settings.Global.getInt(context.getContentResolver(),
                    SHORTCT_SHOW_ON_3D, 0) == 1;
        }
        return mShortctShowOn3d;
    }

    /** Volume keys while the shortcut panel shows over a 3D app: adjust the volume directly. */
    void interceptVolumeEventAndApply(Context context, KeyEvent event) {
        if (event.getAction() != KeyEvent.ACTION_DOWN) {
            return;
        }
        if (mAudioService == null) {
            mAudioService = IAudioService.Stub.asInterface(ServiceManager.checkService("audio"));
            if (mAudioService == null) {
                return;
            }
        }
        final int flags = AudioManager.FLAG_SHOW_UI | AudioManager.FLAG_PLAY_SOUND
                | AudioManager.FLAG_FROM_KEY;
        final String pkgName = context.getOpPackageName();
        try {
            switch (event.getKeyCode()) {
                case KeyEvent.KEYCODE_VOLUME_UP:
                    mAudioService.adjustSuggestedStreamVolume(AudioManager.ADJUST_RAISE,
                            AudioManager.USE_DEFAULT_STREAM_TYPE, flags, pkgName, TAG);
                    break;
                case KeyEvent.KEYCODE_VOLUME_DOWN:
                    mAudioService.adjustSuggestedStreamVolume(AudioManager.ADJUST_LOWER,
                            AudioManager.USE_DEFAULT_STREAM_TYPE, flags, pkgName, TAG);
                    break;
                case KeyEvent.KEYCODE_VOLUME_MUTE:
                    if (event.getRepeatCount() == 0) {
                        mAudioService.adjustSuggestedStreamVolume(
                                AudioManager.ADJUST_TOGGLE_MUTE,
                                AudioManager.USE_DEFAULT_STREAM_TYPE, flags, pkgName, TAG);
                    }
                    break;
                default:
                    break;
            }
        } catch (Exception e) {
            Slog.e(TAG, "Error dispatching volume key in interceptVolumeEventAndApply.", e);
        }
    }

    /**
     * interceptKeyBeforeDispatching: HOME is consumed unless it comes from a gesture hand (then
     * it goes to the app). -1 = no change.
     */
    long adjustResultFromInterceptKeyBeforeDispatchingInner(WindowState win, KeyEvent event,
            int policyFlags, long result) {
        if (event == null || event.getKeyCode() != KeyEvent.KEYCODE_HOME) {
            return -1;
        }
        final int deviceId = event.getDeviceId();
        if (deviceId == DEVICE_GESTURE_LEFT_HAND || deviceId == DEVICE_GESTURE_RIGHT_HAND) {
            Slog.w(TAG, "adjustResultFromInterceptKeyBeforeDispatchingInner");
            return 0;
        }
        return -1;
    }

    /** powerPress: the short press does nothing (key config, psensor near, DP mode). */
    boolean interruptPowerPress() {
        if (getPowerDefined() && mHmdPowerKeyAction.tapAction == 0) {
            Log.w(TAG, "interruptPowerPress by power defined");
            return true;
        }
        if (SystemProperties.getInt("sys.pxr.psensor.status", 1) == 0
                && SystemProperties.getInt("persist.pxr.psensor.powermode", 1) == 1
                && mIsDefectiveDevice && mBase.isScreenOn()) {
            Log.w(TAG, "KeyEvent.KEYCODE_POWER press but no effect for psensor is near status");
            return true;
        }
        if (SystemProperties.getInt("sys.pxr.vxr7200.status", 0) == 1) {
            Log.w(TAG, "interruptPowerPress by vxr7200");
            return true;
        }
        return false;
    }

    boolean interruptPowerLongPress() {
        if (getPowerDefined() && mHmdPowerKeyAction.longPressAction == 0) {
            Log.w(TAG, "interruptPowerLongPress by power defined");
            return true;
        }
        return false;
    }

    /** The power key always waits for a possible double press. */
    int getDefaultMaxMultiPressPowerCount() {
        return 2;
    }

    boolean isPowerDisabledForHcit() {
        return sPowerDisabledForHcit;
    }

    boolean denyBackKeyIn2DApp() {
        return Features.isDenyBackKeyIn2dApp();
    }

    /** DisplayHomeButtonHandler: HOME held for over 5 s is reported as pxr notification. */
    void sendTapHomeMsgIfNeeded(KeyEvent event) {
        if (event.getRepeatCount() == 0) {
            mHomeDownTime = System.currentTimeMillis();
            return;
        }
        if ((event.getFlags() & KeyEvent.FLAG_LONG_PRESS) != 0) {
            return;
        }
        final long repeatHomeTime = System.currentTimeMillis() - mHomeDownTime;
        if (mHomeDownTime != -1 && repeatHomeTime > 5000) {
            sendLongPressNotification(NOTIFICATION_MSG_HOME_LONG_PRESS, repeatHomeTime);
            mHomeDownTime = -1;
        }
    }

    /** interceptKeyBeforeDispatchingInner: BACK held for over 8 s is reported. */
    void dispatchBackKeyTapMsg(KeyEvent event) {
        if (event.getKeyCode() != KeyEvent.KEYCODE_BACK) {
            return;
        }
        if (event.getRepeatCount() == 0) {
            mBackDownTime = System.currentTimeMillis();
            return;
        }
        final long repeatBackTime = System.currentTimeMillis() - mBackDownTime;
        if (mBackDownTime != -1 && repeatBackTime > 8000) {
            sendLongPressNotification(NOTIFICATION_MSG_BACK_LONG_PRESS, repeatBackTime);
            mBackDownTime = -1;
        }
    }

    private static void sendLongPressNotification(String msg, long duration) {
        try {
            final IPxrNotificationService notification = getPxrNotificationService();
            if (notification != null) {
                Log.w(TAG, "long tap, send notification msg " + msg);
                notification.sendPxrMessage(msg, 1, "", (int) duration, "");
            } else {
                Log.w(TAG, "pxr_notification is null ,do nothing ...");
            }
        } catch (RemoteException e) {
            Log.e(TAG, "invoke pxrnotification send " + msg + " Exception:" + e);
        }
    }

    /**
     * checkAddPermission: apps marked as VR apps in their metadata need SYSTEM_ALERT_WINDOW for
     * system windows.
     */
    boolean needCheckSystemAlertWindowPermission(Context context,
            WindowManager.LayoutParams attrs, int callingUid) {
        ApplicationInfo applicationInfo;
        try {
            applicationInfo = context.getPackageManager().getApplicationInfoAsUser(
                    attrs.packageName, PackageManager.GET_META_DATA,
                    UserHandle.getUserId(callingUid));
        } catch (PackageManager.NameNotFoundException e) {
            applicationInfo = null;
        }
        if (applicationInfo == null || applicationInfo.metaData == null) {
            return false;
        }
        return applicationInfo.metaData.getString("com.picovr.type") != null
                || applicationInfo.metaData.getString("pvr.app.type") != null;
    }

    /** startDockOrHome: test mode goes to Launcher3 when it is installed. */
    boolean startLauncher3IfNeeded() {
        if (!SystemProperties.getBoolean("persist.pxr.testmode", false)) {
            return false;
        }
        try {
            mBase.mContext.getPackageManager().getApplicationInfo("com.android.launcher3", 0);
        } catch (Exception e) {
            return false;
        }
        final Intent intent = new Intent();
        intent.setComponent(new ComponentName("com.android.launcher3",
                "com.android.launcher3.Launcher"));
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
        mBase.startActivityAsUser(intent, UserHandle.CURRENT_OR_SELF);
        return true;
    }

    /** startDockOrHome: HOME goes to SystemExt instead of the Android home activity. */
    boolean launchDockIfNeeded() {
        dispatchHomeToNS();
        return true;
    }

    /** Tap / double-tap / long-press state and actions of one PICO key. */
    private final class SystemKeyAction {
        int enable = -1;
        int defined = 0;
        int tapAction = ACTION_PXR_UNDEFINED;
        int doubleTapAction = ACTION_PXR_UNDEFINED;
        int longPressAction = ACTION_PXR_UNDEFINED;
        int doubleTapActionTime = 300;
        int longPressActionTime = 500;
        boolean enableDoubleTap = true;
        boolean down = false;
        int keycode = -1;
        long downTime = -1;
        boolean doubleTapPending = false;
        boolean isPressed = false;
        boolean isLongPressed = false;
        boolean isConsumed = false;
        boolean tapMessageHold = true;
        private boolean conflictWithOtherKeys = false;

        void setConflictWithOtherKeys() {
            conflictWithOtherKeys = true;
            cancelKeyTapActionDelay(keycode);
            doubleTapPending = false;
            isPressed = false;
            isLongPressed = false;
            isConsumed = false;
        }

        void handleEvent(KeyEvent event) {
            doKeyAction(event);
            down = event.getAction() == KeyEvent.ACTION_DOWN;
            keycode = event.getKeyCode();
            if (!down || event.getRepeatCount() == 0) {
                conflictWithOtherKeys = false;
            }
        }

        private void doKeyAction(KeyEvent event) {
            if (conflictWithOtherKeys) {
                return;
            }
            Slog.d(TAG, "doKeyAction event: " + event + " mDoubleTapPending is "
                    + doubleTapPending + ",longpress_action_time=" + longPressActionTime
                    + ",doubletap_action_time=" + doubleTapActionTime + ",isConsumed="
                    + isConsumed);
            final boolean isDown = event.getAction() == KeyEvent.ACTION_DOWN;
            if (doubleTapPending) {
                downTime = -1;
                isLongPressed = false;
                if (isDown) {
                    isPressed = true;
                    cancelKeyTapActionDelay(event.getKeyCode());
                    return;
                }
                isPressed = false;
                doubleTapPending = false;
                doKeyDoubleTapAction();
                return;
            }
            if (isDown) {
                isPressed = true;
                if (event.getRepeatCount() == 0) {
                    isConsumed = false;
                    downTime = System.currentTimeMillis();
                    isLongPressed = false;
                } else if (!isLongPressed
                        && System.currentTimeMillis() - downTime > longPressActionTime) {
                    isLongPressed = true;
                    doKeyLongPressAction();
                }
                return;
            }
            isPressed = false;
            isLongPressed = false;
            if (!isConsumed) {
                if (!enableDoubleTap) {
                    downTime = -1;
                    doKeyTapAction();
                } else if (System.currentTimeMillis() - downTime < doubleTapActionTime) {
                    doubleTapPending = true;
                    downTime = -1;
                    doKeyTapActionDelay(event.getKeyCode());
                }
            }
            downTime = -1;
        }

        private int tapMessageFor(int code) {
            switch (code) {
                case KeyEvent.KEYCODE_HOME: return MSG_HMD_HOME_TAP;
                case KeyEvent.KEYCODE_CAMERA: return MSG_RCAPTURE_TAP;
                case KEYCODE_LEFT_CONTROLLER_HOME: return MSG_LCONTROLLER_HOME_TAP;
                case KEYCODE_RIGHT_CONTROLLER_HOME: return MSG_RCONTROLLER_HOME_TAP;
                default: return -1;
            }
        }

        private void cancelKeyTapActionDelay(int code) {
            if (DEBUG) {
                Slog.d(TAG, "we cancelKeyTapActionDelay " + code);
            }
            final int what = tapMessageFor(code);
            if (what != -1 && tapMessageHold) {
                mKeyActionHandler.removeMessages(what);
                tapMessageHold = false;
            }
        }

        private void doKeyTapActionDelay(int code) {
            final int what = tapMessageFor(code);
            if (what != -1) {
                tapMessageHold = true;
                mKeyActionHandler.sendEmptyMessageDelayed(what, doubleTapActionTime);
            }
        }

        void doKeyTapAction() {
            Slog.i(TAG, "we doKeyTapAction " + keycode + " , resume " + isConsumed);
            notifyHomeKeyActionIfNeeded(keycode, 1);
            tapMessageHold = false;
            doubleTapPending = false;
            if (keycode == KeyEvent.KEYCODE_HOME) {
                if (SystemProperties.getInt("pvr.screenshot.preview", 0) == 1
                        && !Features.PROJECT_PHOENIX.equals(Features.getProjectName())) {
                    Slog.i(TAG, "doKeyTapAction shortcut preview");
                    launchScreenAction("pvr.intent.action.SCREEN_SHOT", "system_key");
                    isConsumed = true;
                    return;
                }
            } else if (keycode == KeyEvent.KEYCODE_CAMERA) {
                if (isSetupWizardComplete()) {
                    launchScreenAction("pvr.intent.action.SCREEN_SHOT", "capture_key");
                    isConsumed = true;
                    return;
                }
            } else if (keycode == KEYCODE_LEFT_CONTROLLER_HOME
                    || keycode == KEYCODE_RIGHT_CONTROLLER_HOME) {
                if (SystemProperties.getInt("pvr.screenshot.preview", 0) == 1) {
                    Slog.i(TAG, "doKeyTapAction shortcut preview");
                    launchScreenAction("pvr.intent.action.SCREEN_SHOT", "system_key");
                    isConsumed = true;
                    return;
                }
            }
            if (!isConsumed) {
                isConsumed = true;
                doKeyActionReal(tapAction);
            }
        }

        private void doKeyDoubleTapAction() {
            if (DEBUG) {
                Slog.i(TAG, "we doKeyDoubleTapAction " + keycode);
            }
            notifyHomeKeyActionIfNeeded(keycode, 2);
            if (!isConsumed) {
                isConsumed = true;
                doKeyActionReal(doubleTapAction);
            }
        }

        private void doKeyLongPressAction() {
            notifyHomeKeyActionIfNeeded(keycode, 3);
            if (!isConsumed) {
                isConsumed = true;
                doKeyActionReal(longPressAction);
            }
        }

        /** ToB: HOME key actions (1 tap, 2 double tap, 3 long press) are broadcast. */
        private void notifyHomeKeyActionIfNeeded(final int code, final int action) {
            if (!mIsToBDevice || isConsumed || (code != KeyEvent.KEYCODE_HOME
                    && code != KEYCODE_LEFT_CONTROLLER_HOME
                    && code != KEYCODE_RIGHT_CONTROLLER_HOME)) {
                return;
            }
            mBase.mHandler.post(() -> {
                final Intent intent = new Intent("pxr.intent.action.home_key");
                intent.putExtra("keycode", code);
                intent.putExtra("action", action);
                intent.addFlags(Intent.FLAG_RECEIVER_REGISTERED_ONLY);
                mBase.mContext.sendBroadcastAsUser(intent, UserHandle.CURRENT);
                Log.d(TAG, "notifyHomeKeyAction keycode : " + code + ", action : " + action);
            });
        }

        private void launchScreenAction(String screenAction, String from) {
            Slog.d(TAG, "launchScreenAction " + screenAction + " from " + from);
            if (Features.isKeyguardEnabled()
                    && KeyStore.getInstance().state() == KeyStore.State.LOCKED) {
                final Intent i = new Intent("pvr.intent.action.vrdisplay");
                i.setPackage("com.pvr.vrdisplay");
                i.putExtra("action_type", 93);
                i.putExtra("remind_type", 3);
                mBase.mContext.startService(i);
                return;
            }
            if ("capture_key".equals(from) && getGlobalFlag(SETTINGS_DISABLE_CAMERA_KEY)
                    && !getGlobalFlag(SETTINGS_DOCK_SHOWING)) {
                if ("pvr.intent.action.SCREEN_SHOT".equals(screenAction)
                        && !getGlobalFlag(SETTINGS_SCREENSHOT_TOAST_SHOWING)) {
                    Slog.d(TAG, "ignore screenshot, camera key disabled by the top app");
                    return;
                }
                if ("pvr.intent.action.SCREEN_RECORD".equals(screenAction)) {
                    Slog.d(TAG, "ignore screen record, camera key disabled by the top app");
                    return;
                }
            }
            final Intent intent = new Intent(screenAction);
            intent.setPackage(SCREEN_RECORD_CAP_PACKAGE);
            intent.putExtra("from", from);
            Slog.d(TAG, "launchScreenAction intent : " + intent + " from : " + from);
            mBase.mContext.startService(intent);
        }

        private void doKeyActionReal(int realAction) {
            if (DEBUG) {
                Slog.w(TAG, "doKeyActionReal " + realAction);
            }
            switch (realAction) {
                case ACTION_PXR_RECENTER:
                    Slog.i(TAG, "launchRecenter");
                    mBase.sendCloseSystemWindows("recenter");
                    break;
                case ACTION_PXR_GLOBAL_NAVIGATION: {
                    final ActivityInfo top = mBase.mActivityTaskManagerInternal
                            .getPicoTopResumedActivityInfo(android.view.Display.DEFAULT_DISPLAY);
                    Slog.d(TAG, "launchShortcutCheck topActivity : " + top);
                    if (top != null && !topIsVrPermissionActivity()) {
                        dispatchHomeToNS();
                    }
                    break;
                }
                case ACTION_PXR_GOHOME: {
                    final Intent intent = new Intent("pvr.intent.action.LAUNCHER_MAIN");
                    intent.putExtra("func", ACTION_PXR_GOHOME);
                    intent.setPackage(SystemExt.sCurrentPkg);
                    mBase.mContext.startService(intent);
                    break;
                }
                case ACTION_PXR_DASHBOARD: {
                    Slog.w(TAG, "launchQuickSettings !");
                    final Intent intent = new Intent("pui.settings.action.QUICK_SETINGS");
                    intent.setPackage("com.picovr.settings");
                    mBase.startActivityAsUser(intent, UserHandle.CURRENT_OR_SELF);
                    break;
                }
                case ACTION_PXR_SCREENCAP:
                case ACTION_PXR_SCREENRECORD:
                    if (keycode == KeyEvent.KEYCODE_CAMERA && !isSetupWizardComplete()) {
                        break;
                    }
                    launchScreenAction(realAction == ACTION_PXR_SCREENCAP
                                    ? "pvr.intent.action.SCREEN_SHOT"
                                    : "pvr.intent.action.SCREEN_RECORD",
                            keycode != KeyEvent.KEYCODE_CAMERA ? "system_key" : "capture_key");
                    break;
                default:
                    break;
            }
        }

        void reset() {
            enable = -1;
            defined = -1;
            tapAction = ACTION_PXR_UNDEFINED;
            doubleTapAction = ACTION_PXR_UNDEFINED;
            longPressAction = ACTION_PXR_UNDEFINED;
            doubleTapActionTime = 300;
            longPressActionTime = 500;
            down = false;
            downTime = -1;
            doubleTapPending = false;
            isLongPressed = false;
            isConsumed = false;
        }
    }

    private final class SystemKeyHandler extends Handler {
        SystemKeyHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_HMD_HOME_TAP:
                    mHmdHomeKeyAction.doKeyTapAction();
                    break;
                case MSG_LCONTROLLER_HOME_TAP:
                    mLctlHomeKeyAction.doKeyTapAction();
                    break;
                case MSG_RCONTROLLER_HOME_TAP:
                    mRctlHomeKeyAction.doKeyTapAction();
                    break;
                case MSG_RCAPTURE_TAP:
                    mRctlCaptureKeyAction.doKeyTapAction();
                    break;
                default:
                    break;
            }
        }
    }
}
