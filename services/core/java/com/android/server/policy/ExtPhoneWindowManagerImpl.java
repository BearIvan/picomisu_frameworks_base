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
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.pico.utils.Features;
import android.provider.Settings;
import android.security.KeyStore;
import android.text.TextUtils;
import android.util.Log;
import android.util.Slog;
import android.view.Display;
import android.view.KeyEvent;
import android.view.WindowManager;

import com.android.server.api.ApiLayerService;
import com.android.server.wm.IExtActivityTaskManagerInternal;
import com.android.server.wm.SettingsObserverExt;
import com.android.server.wm.SystemExt;
import com.pvr.IPvrManagerService;
import com.pvr.pxrnotification.PxrNotificationService;

import java.io.File;
import java.io.FileInputStream;
import java.util.Properties;

/**
 * PICO VR key, home and power handling of the window policy (factory PICO OS 5.13.7
 * com.android.server.policy.ExtPhoneWindowManagerImpl), ported method by method.
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
 * Also not ported: the Smartisan quick boot on power long press (PhoneWindowManager side).
 *
 * pxr_notification is reached through the factory PxrNotificationService.getInstance (the
 * binder is looked up once, when that class is initialized).
 *
 * @hide
 */
public class ExtPhoneWindowManagerImpl implements IExtPhoneWindowManager {
    private static final String HMD_USER_KEY_CONFIG_ACTION_HOMEDOUBLETAP =
            "hmd_action_home_double_tap";
    private static final String HMD_USER_KEY_CONFIG_ACTION_HOMELONGPRESS =
            "hmd_action_home_long_press";
    private static final String HMD_USER_KEY_CONFIG_ACTION_HOMESINGLETAP =
            "hmd_action_home_single_tap";
    private static final String HMD_USER_KEY_CONFIG_TIME_HOMEDOUBLETAP = "hmd_time_home_double_tap";
    private static final String HMD_USER_KEY_CONFIG_TIME_HOMELONGPRESS = "hmd_time_home_long_press";
    private static final String HOME_DISALBE_FOR_PVR = "android.intent.disablehome_pvr";
    private static final String LCTL_USER_KEY_CONFIG_ACTION_HOMEDOUBLETAP =
            "lctl_action_home_double_tap";
    private static final String LCTL_USER_KEY_CONFIG_ACTION_HOMELONGPRESS =
            "lctl_action_home_long_press";
    private static final String LCTL_USER_KEY_CONFIG_ACTION_HOMESINGLETAP =
            "lctl_action_home_single_tap";
    private static final String LCTL_USER_KEY_CONFIG_TIME_HOMEDOUBLETAP =
            "lctl_time_home_double_tap";
    private static final String LCTL_USER_KEY_CONFIG_TIME_HOMELONGPRESS =
            "lctl_time_home_long_press";
    private static final String MDM_KEY_CONFIG_PATH = "/data/local/tmp/PxrSystemKeyConfig.prop";
    private static final String OEM_KEY_CONFIG_PATH = "/data/misc/pxr/PxrSystemKeyConfig.prop";
    private static final String POWER_DISALBE_FOR_HCIT = "android.intent.disalbepower_hcit";
    private static final String PXR_NOTIFICATION_NAME_MULTI_KEY_PRESSED =
            "pxr.notification.key.multi_key_pressed";
    private static final String RCTL_USER_KEY_CONFIG_ACTION_HOMEDOUBLETAP =
            "rctl_action_home_double_tap";
    private static final String RCTL_USER_KEY_CONFIG_ACTION_HOMELONGPRESS =
            "rctl_action_home_long_press";
    private static final String RCTL_USER_KEY_CONFIG_ACTION_HOMESINGLETAP =
            "rctl_action_home_single_tap";
    private static final String RCTL_USER_KEY_CONFIG_TIME_HOMEDOUBLETAP =
            "rctl_time_home_double_tap";
    private static final String RCTL_USER_KEY_CONFIG_TIME_HOMELONGPRESS =
            "rctl_time_home_long_press";
    private static final String SHORTCT_SHOW_ON_3D = "shortct_show_on_3d";
    static final String TAG = "WindowManagerExt";
    private static final String USER_KEY_CONFIG_ACTION_BACK = "action_key_back";
    private static final String USER_KEY_CONFIG_ACTION_CLASS = "_class";
    private static final String USER_KEY_CONFIG_ACTION_CONFIRM = "action_key_enter";
    private static final String USER_KEY_CONFIG_ACTION_HOMEDOUBLETAP = "action_home_double_tap";
    private static final String USER_KEY_CONFIG_ACTION_HOMELONGPRESS = "action_home_long_press";
    private static final String USER_KEY_CONFIG_ACTION_HOMESINGLETAP = "action_home_single_tap";
    private static final String USER_KEY_CONFIG_ACTION_PACKAGE = "_package";
    private static final String USER_KEY_CONFIG_ACTION_POWERLONGPRESS = "action_power_long_press";
    private static final String USER_KEY_CONFIG_ACTION_POWERSINGLETAP = "action_power_single_tap";
    private static final String USER_KEY_CONFIG_ACTION_VOLUMEDOWN = "action_key_volumedown";
    private static final String USER_KEY_CONFIG_ACTION_VOLUMEUP = "action_key_volumeup";
    private static final String USER_KEY_CONFIG_CHANGE = "android.intent.user_keyconfig_change";
    private static final String USER_KEY_CONFIG_PATH = "/data/local/tmp/SystemKeyConfig.prop";
    private static final String USER_KEY_CONFIG_PATH_DEFAULT = "/system/etc/SystemKeyConfig.prop";
    private static final String USER_KEY_CONFIG_TIME_HOMEDOUBLETAP = "time_home_double_tap";
    private static final String USER_KEY_CONFIG_TIME_HOMELONGPRESS = "time_home_long_press";
    private static final String USER_KEY_CONFIG_TIME_POWERLONGPRESS = "time_power_long_press";
    private static final String notification_msg_back_long_press = "long.tap.back";
    private static final String notification_msg_home_long_press = "long.tap.home";

    /**
     * The build project. The factory compares it with "phoenix" as a compile-time constant, so
     * every "not phoenix" branch below is dead code on this product, as on the factory.
     */
    private static final String BUILD_PROJECT = Features.PROJECT_PHOENIX;

    private String backKeyConfig;
    private boolean backKeyConfigDefined;
    private String doubleTapOnHomeTimeConfig;
    private String doubleTapOnHomeUserConfig;
    private String enterKeyConfig;
    private boolean enterKeyConfigDefined;
    private String hmd_doubleTapOnHomeTimeConfig;
    private String hmd_doubleTapOnHomeUserConfig;
    private String hmd_longPressOnHomeTimeConfig;
    private String hmd_longPressOnHomeUserConfig;
    private boolean hmd_mDoubleTapOnHomeTimeDefined;
    private boolean hmd_mDoubleTapOnHomeUserDefined;
    private boolean hmd_mLongPressOnHomeTimeDefined;
    private boolean hmd_mLongPressOnHomeUserDefined;
    private boolean hmd_mSingleTapOnHomeUserDefined;
    private String hmd_singleTapOnHomeUserConfig;
    private SystemKeyAction hmdback_KeyAction;
    private SystemKeyAction hmdconfirm_KeyAction;
    private SystemKeyAction hmdhome_KeyAction;
    private SystemKeyAction hmdpower_KeyAction;
    private SystemKeyAction hmdvolumedown_KeyAction;
    private SystemKeyAction hmdvolumeup_KeyAction;
    private boolean isDefectiveDevice;
    private String lctl_doubleTapOnHomeTimeConfig;
    private String lctl_doubleTapOnHomeUserConfig;
    private String lctl_longPressOnHomeTimeConfig;
    private String lctl_longPressOnHomeUserConfig;
    private boolean lctl_mDoubleTapOnHomeTimeDefined;
    private boolean lctl_mDoubleTapOnHomeUserDefined;
    private boolean lctl_mLongPressOnHomeTimeDefined;
    private boolean lctl_mLongPressOnHomeUserDefined;
    private boolean lctl_mSingleTapOnHomeUserDefined;
    private String lctl_singleTapOnHomeUserConfig;
    private SystemKeyAction lctlhome_KeyAction;
    private String longPressOnHomeTimeConfig;
    private String longPressOnHomeUserConfig;
    private String longPressOnPowerTimeConfig;
    private String longPressOnPowerUserConfig;
    ActivityManager mActivityManager;
    private IAudioService mAudioService;
    private long mBackDownTime;
    private PhoneWindowManager mBase;
    private boolean mCompositeKeyTriggered;
    private long mConFirmKeyDownTime;
    private boolean mConFirmKeyTriggered;
    private boolean mDoubleTapOnHomeTimeDefined;
    private boolean mDoubleTapOnHomeUserDefined;
    private long mHomeDownTime;
    private boolean mIsToBDevice;
    private Handler mKeyActionHandler;
    private boolean mLongPressOnHomeTimeDefined;
    private boolean mLongPressOnHomeUserDefined;
    private boolean mLongPressOnMenuUserDefined;
    private boolean mLongPressOnPowerTimeDefined;
    private boolean mLongPressOnPowerUserDefined;
    private IPvrManagerService mPvrManagerService;
    BroadcastReceiver mPxrKeyActionReceiver;
    private long mRepeatBackTime;
    private long mRepeatHomeTime;
    private boolean mShortctShowOn3d;
    private boolean mSingleTapOnHomeUserDefined;
    private boolean mSingleTapOnPowerUserDefined;
    private boolean mUsbModeInit;
    private long mVolumeDownKeyDownTime;
    private boolean mVolumeDownKeyTriggered;
    private String rctl_doubleTapOnHomeTimeConfig;
    private String rctl_doubleTapOnHomeUserConfig;
    private String rctl_longPressOnHomeTimeConfig;
    private String rctl_longPressOnHomeUserConfig;
    private boolean rctl_mDoubleTapOnHomeTimeDefined;
    private boolean rctl_mDoubleTapOnHomeUserDefined;
    private boolean rctl_mLongPressOnHomeTimeDefined;
    private boolean rctl_mLongPressOnHomeUserDefined;
    private boolean rctl_mSingleTapOnHomeUserDefined;
    private String rctl_singleTapOnHomeUserConfig;
    private SystemKeyAction rctlcapture_KeyAction;
    private SystemKeyAction rctlhome_KeyAction;
    private String singleTapOnHomeUserConfig;
    private String singleTapOnPowerUserConfig;
    private String volumedownKeyConfig;
    private boolean volumedownKeyConfigDefined;
    private String volumeupKeyConfig;
    private boolean volumeupKeyConfigDefined;
    private boolean willSendKeyBC;
    private static final boolean DEBUG = Build.IS_DEBUGGABLE;
    private static boolean mHomeDisabledForPvr = false;
    private static boolean mPowerDisabledForHcit = false;
    private static boolean mFactoryTestRunning = false;
    private static boolean mCitUnregisterListener = false;
    private final int LEFT_CONTROLLER_KEYCODE = 901;
    private final int RIGHT_CONTROLLER_KEYCODE = 902;
    private final int MSG_HMD_HOME_TAP = 101;
    private final int MSG_LCONTROLLER_HOME_TAP = 102;
    private final int MSG_RCONTROLLER_HOME_TAP = 103;
    private final int MSG_RCAPTURE_TAP = 104;
    private final int ACTION_PXR_UNDEFINED = -1;
    private final int ACTION_PXR_GOHOME = 2;
    private final int ACTION_PXR_DASHBOARD = 3;
    private final int ACTION_PXR_RECENTER = 6;
    private final int ACTION_PXR_GLOBAL_NAVIGATION = 11;
    private final int ACTION_PXR_SCREENCAP = 104;
    private final int ACTION_PXR_SCREENRECOARD = 105;
    private String SCREEN_RECOARD_CAP_PACKAGE_DEFAULT = "com.bytedance.pico.screencapture";

    /** Factory WindowManagerServiceSmtBase.MSG_HIDE_STATUS_BAR_TIMEOUT, sent as a virtual key. */
    private static final int MSG_HIDE_STATUS_BAR_TIMEOUT = 1005;

    protected ExtPhoneWindowManagerImpl(PhoneWindowManager base) {
        hmdhome_KeyAction = new SystemKeyAction();
        lctlhome_KeyAction = new SystemKeyAction();
        rctlhome_KeyAction = new SystemKeyAction();
        hmdconfirm_KeyAction = new SystemKeyAction();
        hmdvolumeup_KeyAction = new SystemKeyAction();
        hmdvolumedown_KeyAction = new SystemKeyAction();
        hmdback_KeyAction = new SystemKeyAction();
        hmdpower_KeyAction = new SystemKeyAction();
        rctlcapture_KeyAction = new SystemKeyAction();
        mKeyActionHandler = new SystemKeyHandler();
        willSendKeyBC = SystemProperties.getInt("persit.pxr.keybc.enable", 0) == 1;
        singleTapOnHomeUserConfig = null;
        doubleTapOnHomeUserConfig = null;
        longPressOnHomeUserConfig = null;
        doubleTapOnHomeTimeConfig = null;
        longPressOnHomeTimeConfig = null;
        longPressOnPowerTimeConfig = null;
        singleTapOnPowerUserConfig = null;
        longPressOnPowerUserConfig = null;
        enterKeyConfig = null;
        volumeupKeyConfig = null;
        volumedownKeyConfig = null;
        backKeyConfig = null;
        hmd_singleTapOnHomeUserConfig = null;
        hmd_doubleTapOnHomeUserConfig = null;
        hmd_longPressOnHomeUserConfig = null;
        hmd_doubleTapOnHomeTimeConfig = null;
        hmd_longPressOnHomeTimeConfig = null;
        lctl_singleTapOnHomeUserConfig = null;
        lctl_doubleTapOnHomeUserConfig = null;
        lctl_longPressOnHomeUserConfig = null;
        lctl_doubleTapOnHomeTimeConfig = null;
        lctl_longPressOnHomeTimeConfig = null;
        rctl_singleTapOnHomeUserConfig = null;
        rctl_doubleTapOnHomeUserConfig = null;
        rctl_longPressOnHomeUserConfig = null;
        rctl_doubleTapOnHomeTimeConfig = null;
        rctl_longPressOnHomeTimeConfig = null;
        mSingleTapOnHomeUserDefined = false;
        mDoubleTapOnHomeUserDefined = false;
        mLongPressOnHomeUserDefined = false;
        mDoubleTapOnHomeTimeDefined = false;
        mLongPressOnHomeTimeDefined = false;
        mLongPressOnPowerTimeDefined = false;
        mSingleTapOnPowerUserDefined = false;
        mLongPressOnPowerUserDefined = false;
        mLongPressOnMenuUserDefined = false;
        enterKeyConfigDefined = false;
        volumeupKeyConfigDefined = false;
        volumedownKeyConfigDefined = false;
        backKeyConfigDefined = false;
        hmd_mSingleTapOnHomeUserDefined = false;
        hmd_mDoubleTapOnHomeUserDefined = false;
        hmd_mLongPressOnHomeUserDefined = false;
        hmd_mDoubleTapOnHomeTimeDefined = false;
        hmd_mLongPressOnHomeTimeDefined = false;
        lctl_mSingleTapOnHomeUserDefined = false;
        lctl_mDoubleTapOnHomeUserDefined = false;
        lctl_mLongPressOnHomeUserDefined = false;
        lctl_mDoubleTapOnHomeTimeDefined = false;
        lctl_mLongPressOnHomeTimeDefined = false;
        rctl_mSingleTapOnHomeUserDefined = false;
        rctl_mDoubleTapOnHomeUserDefined = false;
        rctl_mLongPressOnHomeUserDefined = false;
        rctl_mDoubleTapOnHomeTimeDefined = false;
        rctl_mLongPressOnHomeTimeDefined = false;
        isDefectiveDevice = "A7L10".equals(SystemProperties.get("pxr.vendorhw.product.name"))
                || "A7J10".equals(SystemProperties.get("pxr.vendorhw.product.name"));
        mHomeDownTime = -1L;
        mBackDownTime = -1L;
        mUsbModeInit = true;
        mPxrKeyActionReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String intentaction = intent.getAction();
                Slog.d(TAG, "mPxrKeyActionReceiver get action " + intentaction);
                if (POWER_DISALBE_FOR_HCIT.equals(intentaction)) {
                    if (intent.getBooleanExtra("state", false)) {
                        mPowerDisabledForHcit = true;
                    } else {
                        mPowerDisabledForHcit = false;
                    }
                    return;
                }
                if (HOME_DISALBE_FOR_PVR.equals(intentaction)) {
                    if (intent.getBooleanExtra("state", false)) {
                        mHomeDisabledForPvr = true;
                    } else {
                        mHomeDisabledForPvr = false;
                    }
                    return;
                }
                if (USER_KEY_CONFIG_CHANGE.equals(intentaction)) {
                    updateSystemKeyConfig();
                }
            }
        };
        mBase = base;
        mAudioService = IAudioService.Stub.asInterface(ServiceManager.checkService("audio"));
    }

    /** End of PhoneWindowManager.init. */
    @Override
    public void init(Context context) {
        initSystemKey();
        updateSystemKeyConfig();
        mIsToBDevice = SystemProperties.getInt("ro.pxr.externalfunc", 0) != 0
                || Settings.Global.getInt(mBase.mContext.getContentResolver(),
                        "ro.pxr.externalfunc.test", 0) != 0;
        mActivityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        if (isDefectiveDevice) {
            mBase.mVeryLongPressTimeout = 3000;
        }
        mBase.mScreenshotChordEnabled = mBase.mScreenshotChordEnabled
                && SystemProperties.getInt("persist.pvr.screenshot.enable", 1) == 1;
    }

    @Override
    public void initSystemKey() {
        IntentFilter filter = new IntentFilter();
        filter.addAction(POWER_DISALBE_FOR_HCIT);
        filter.addAction(HOME_DISALBE_FOR_PVR);
        filter.addAction(USER_KEY_CONFIG_CHANGE);
        mBase.mContext.registerReceiver(mPxrKeyActionReceiver, filter);
    }

    @Override
    public void updateSystemKeyConfig() {
        Slog.d(TAG, "updateSystemKeyConfig call");
        hmdhome_KeyAction.reset();
        lctlhome_KeyAction.reset();
        rctlhome_KeyAction.reset();
        hmdconfirm_KeyAction.reset();
        hmdvolumeup_KeyAction.reset();
        hmdvolumedown_KeyAction.reset();
        hmdback_KeyAction.reset();
        hmdpower_KeyAction.reset();
        hmdhome_KeyAction.defined = 1;
        lctlhome_KeyAction.defined = 1;
        rctlhome_KeyAction.defined = 1;
        rctlcapture_KeyAction.defined = 1;
        rctlcapture_KeyAction.enableDoubleTap = false;
        lctlhome_KeyAction.tap_action = SystemProperties.getInt("persist.pxr.lcontroller.tap",
                ACTION_PXR_GLOBAL_NAVIGATION);
        rctlhome_KeyAction.tap_action = SystemProperties.getInt("persist.pxr.rcontroller.tap",
                ACTION_PXR_GLOBAL_NAVIGATION);
        hmdhome_KeyAction.tap_action = SystemProperties.getInt("persist.pxr.hmd.tap",
                ACTION_PXR_GLOBAL_NAVIGATION);
        if (!Features.PROJECT_PHOENIX.equals(BUILD_PROJECT)) {
            lctlhome_KeyAction.doubletap_action = SystemProperties.getInt(
                    "persist.pxr.lcontroller.doubletap", ACTION_PXR_SCREENRECOARD);
            rctlhome_KeyAction.doubletap_action = SystemProperties.getInt(
                    "persist.pxr.rcontroller.doubletap", ACTION_PXR_SCREENRECOARD);
            hmdhome_KeyAction.doubletap_action = SystemProperties.getInt(
                    "persist.pxr.hmd.doubletap", ACTION_PXR_SCREENRECOARD);
            rctlcapture_KeyAction.enable = 0;
            rctlcapture_KeyAction.defined = 0;
        }
        lctlhome_KeyAction.longpress_action = SystemProperties.getInt(
                "persist.pxr.lcontroller.longpress", ACTION_PXR_RECENTER);
        rctlhome_KeyAction.longpress_action = SystemProperties.getInt(
                "persist.pxr.rcontroller.longpress", ACTION_PXR_RECENTER);
        hmdhome_KeyAction.longpress_action = SystemProperties.getInt(
                "persist.pxr.hmd.longpress", ACTION_PXR_RECENTER);
        rctlcapture_KeyAction.longpress_action_time = SystemProperties.getInt(
                "persist.pxr.capture.longpresstime", 300);
        rctlcapture_KeyAction.longpress_action = SystemProperties.getInt(
                "persist.pxr.capture.longpress", ACTION_PXR_SCREENRECOARD);
        willSendKeyBC = SystemProperties.getInt("persit.pxr.keybc.enable", 0) == 1;
        updateUserKeyConfig();
    }

    private boolean isMulKeyEnable() {
        boolean inDPMode = SystemProperties.getInt("sys.pxr.vxr7200.status", 0) == 1;
        return !inDPMode && SystemProperties.getInt("persist.pvr.mulkey.enable", 1) == 1;
    }

    private void launchSettings() {
        Intent extraIntent = new Intent();
        ComponentName settingcomp = new ComponentName("com.android.settings",
                "com.android.settings.Settings");
        extraIntent.setComponent(settingcomp);
        Intent intent = new Intent("pvr.intent.action.VRSHELL");
        intent.putExtra("intent", extraIntent);
        mBase.startActivityAsUser(intent, UserHandle.CURRENT_OR_SELF);
    }

    /** Opens PICO VR settings, directly over VRShell or wrapped in a VRSHELL intent. */
    private void launchVRSettings() {
        IExtActivityTaskManagerInternal atmService = mBase.mActivityTaskManagerInternal.getExt();
        ActivityInfo topActivityInfo = atmService.getTopAppExt(Display.DEFAULT_DISPLAY);
        if (topActivityInfo == null) {
            return;
        }
        Intent extraIntent = new Intent();
        ComponentName settingcomp = new ComponentName("com.picovr.settings",
                "com.picovr.vrsettingslib.UnityActivity");
        extraIntent.setComponent(settingcomp);
        if ("com.pvr.vrshell".equals(topActivityInfo.packageName)) {
            extraIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            mBase.startActivityAsUser(extraIntent, UserHandle.CURRENT_OR_SELF);
        } else {
            Intent intent = new Intent("pvr.intent.action.VRSHELL");
            intent.putExtra("intent", extraIntent);
            mBase.startActivityAsUser(intent, UserHandle.CURRENT_OR_SELF);
        }
    }

    private void sendPvrBroadCast(String action) {
        Intent pvrIntent = new Intent(action);
        mBase.mContext.sendOrderedBroadcastAsUser(pvrIntent, UserHandle.ALL, null, null, null, 0,
                null, null);
    }

    /**
     * HOME long press + confirm held: BACK opens Android settings, volume down disconnects the
     * controllers. The factory ADB password key sequence at the start of this method and its
     * HOME + volume "open adb" branches are intentionally not ported (see the class comment).
     */
    private boolean checkMulKeyAction(KeyEvent event) {
        if (isMulKeyEnable() && hmdhome_KeyAction.isLongPressed) {
            if (hmdconfirm_KeyAction.isPressed) {
                if (event.getKeyCode() == KeyEvent.KEYCODE_BACK
                        && event.getAction() == KeyEvent.ACTION_DOWN
                        && event.getRepeatCount() == 0) {
                    Slog.d(TAG, "checkMulKeyAction do launchSettings");
                    launchSettings();
                    return true;
                }
                if (event.getKeyCode() == KeyEvent.KEYCODE_VOLUME_DOWN
                        && event.getAction() == KeyEvent.ACTION_DOWN
                        && event.getRepeatCount() == 0) {
                    Slog.w(TAG, "checkMulKeyAction do disconnect controller");
                    sendPvrBroadCast("android.intent.pvrcon.disconnect");
                    Context context = mBase.mContext;
                    if (PxrNotificationService.getInstance(context) != null) {
                        Log.w(TAG, "send pxr notification when multi key pressed");
                        try {
                            PxrNotificationService.getInstance(context).sendPxrMessage(
                                    PXR_NOTIFICATION_NAME_MULTI_KEY_PRESSED, -1, "3;25;1001", -1,
                                    "");
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                    return true;
                }
                return false;
            }
            return false;
        }
        return false;
    }

    private void sendKeyBroadCast(int keycode, int action, int status) {
        Intent keyIntent = new Intent("android.intent.keybroadcast");
        keyIntent.putExtra("keycode", keycode);
        keyIntent.putExtra("action", action);
        keyIntent.putExtra("status", status);
        mBase.mContext.sendOrderedBroadcastAsUser(keyIntent, UserHandle.ALL, null, null, null, 0,
                null, null);
    }

    private boolean checkSomeDefinedCase(String winpackage) {
        return SystemProperties.getInt("sys.pxr.vxr7200.status", 0) == 1;
    }

    private boolean checkSomeDefinedHome(String winpackage) {
        if (mHomeDisabledForPvr) {
            Slog.d(TAG, "checkSomeDefinedHome retun true by home disabled for pvr");
            return true;
        }
        if (SystemProperties.getInt("sys.pxr.vxr7200.status", 0) == 1) {
            Slog.d(TAG, "checkSomeDefinedHome retun true by vxr7200 status");
            return true;
        }
        if ("com.pvr.seethrough.setting".equals(winpackage)) {
            Slog.d(TAG, "checkSomeDefinedHome retun true by seethrough");
            return true;
        }
        if (!Features.PROJECT_PHOENIX.equals(BUILD_PROJECT) || !Features.isKeyguardEnabled()
                || !mBase.isKeyguardShowingAndNotOccluded()) {
            return false;
        }
        Slog.d(TAG, "checkSomeDefinedHome retun true by Keyguard");
        return true;
    }

    @Override
    public int getPowerTapStauts() {
        return hmdpower_KeyAction.tap_action;
    }

    @Override
    public int getPowerLongPressStauts() {
        return hmdpower_KeyAction.longpress_action;
    }

    @Override
    public int getPowerLongPressTime() {
        return hmdpower_KeyAction.longpress_action_time;
    }

    @Override
    public boolean getPowerDefined() {
        return hmdpower_KeyAction.defined == 1;
    }

    /**
     * interceptKeyBeforeDispatchingInner, before anything else: 1 = continue the normal policy,
     * 0 = pass the key to the app, -1 = consume it.
     */
    @Override
    public int processKey(KeyEvent event, WindowManagerPolicy.WindowState win) {
        int keyCode = event.getKeyCode();
        WindowManager.LayoutParams attrsPres = win != null ? win.getAttrs() : null;
        boolean result = handleKey(event, attrsPres != null ? attrsPres.packageName : "unknown");
        Slog.d(TAG, "handleKey result=" + result + ",event=" + event);
        if ((result || KeyEvent.KEYCODE_BACK == keyCode) && !injectBackKeyDirectly()) {
            ApiLayerService.getInstance().onKeyEvent(event);
        }
        if (!result) {
            if (keyCode == KeyEvent.KEYCODE_BACK || keyCode == 1001 || keyCode == 1004
                    || keyCode == KeyEvent.KEYCODE_VOLUME_UP
                    || keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) {
                return -1;
            }
            return 0;
        }
        return 1;
    }

    private boolean injectBackKeyDirectly() {
        boolean ret = mIsToBDevice && SystemProperties.getInt("pvr.active.input_device", 0) == -1
                && SystemProperties.getBoolean("pvr.tob.inject.backkey.direct", false);
        return ret;
    }

    /** True: the normal policy handles the key; false: PICO handled it. */
    @Override
    public boolean handleKey(KeyEvent event, String winpackage) {
        if (event.getRepeatCount() == 0 && DEBUG) {
            Slog.d(TAG, "we get the handleKey " + event.getKeyCode() + ", action "
                    + event.getAction() + ", deviceId " + event.getDeviceId());
        }
        int handkeycode = event.getKeyCode();
        if (mIsToBDevice && !Features.PROJECT_PHOENIX.equals(BUILD_PROJECT) && isMulKeyEnable()) {
            handleCompositeKey(event);
        }
        sendKeyToPvrManager(event);
        checkMulKeyAction(event);
        if (handkeycode == KeyEvent.KEYCODE_HOME) {
            hmdhome_KeyAction.down = event.getAction() == KeyEvent.ACTION_DOWN;
            if (checkSomeDefinedHome(winpackage)) {
                return false;
            }
            if (hmdhome_KeyAction.defined != 1) {
                return hmdhome_KeyAction.enable != 0;
            }
            hmdhome_KeyAction.handleEvent(event);
            return false;
        }
        if (handkeycode == KeyEvent.KEYCODE_BACK) {
            if (event.getDeviceId() < 100000 && !injectBackKeyDirectly()) {
                return false;
            }
            hmdback_KeyAction.handleEvent(event);
            return hmdback_KeyAction.defined != 1 && hmdback_KeyAction.enable != 0;
        }
        if (handkeycode == KeyEvent.KEYCODE_VOLUME_UP) {
            hmdvolumeup_KeyAction.handleEvent(event);
            return hmdvolumeup_KeyAction.defined != 1 && hmdvolumeup_KeyAction.enable != 0;
        }
        if (handkeycode == KeyEvent.KEYCODE_VOLUME_DOWN) {
            hmdvolumedown_KeyAction.handleEvent(event);
            return hmdvolumedown_KeyAction.defined != 1 && hmdvolumedown_KeyAction.enable != 0;
        }
        if (handkeycode == KeyEvent.KEYCODE_CAMERA) {
            if (rctlcapture_KeyAction.defined != 1) {
                return rctlcapture_KeyAction.enable != 0;
            }
            rctlcapture_KeyAction.handleEvent(event);
            if (rctlhome_KeyAction.down) {
                Slog.d(TAG, "camera key conflict with right controller key");
                rctlhome_KeyAction.setConflictWithOtherKeys();
                rctlcapture_KeyAction.setConflictWithOtherKeys();
            }
            return false;
        }
        if (handkeycode == 1001) {
            if (checkSomeDefinedCase(winpackage)) {
                return false;
            }
            hmdconfirm_KeyAction.handleEvent(event);
            return hmdconfirm_KeyAction.defined != 1 && hmdconfirm_KeyAction.enable != 0;
        }
        if (handkeycode == 1004) {
            if (event.getAction() == KeyEvent.ACTION_UP) {
                Slog.w(TAG, "launchRecenter by keyevent KEYCODE_RECENTER");
                mBase.sendCloseSystemWindows("recenter");
            }
            return false;
        }
        if (handkeycode == LEFT_CONTROLLER_KEYCODE) {
            lctlhome_KeyAction.down = event.getAction() == KeyEvent.ACTION_DOWN;
            if (checkSomeDefinedHome(winpackage)) {
                return false;
            }
            if (lctlhome_KeyAction.defined != 1) {
                return lctlhome_KeyAction.enable != 0;
            }
            lctlhome_KeyAction.handleEvent(event);
            return false;
        }
        if (handkeycode != RIGHT_CONTROLLER_KEYCODE) {
            return true;
        }
        rctlhome_KeyAction.down = event.getAction() == KeyEvent.ACTION_DOWN;
        if (checkSomeDefinedHome(winpackage)) {
            return false;
        }
        if (rctlhome_KeyAction.defined != 1) {
            return rctlhome_KeyAction.enable != 0;
        }
        rctlhome_KeyAction.handleEvent(event);
        if (rctlcapture_KeyAction.down) {
            Slog.d(TAG, "right controller key conflict with camera key");
            rctlhome_KeyAction.setConflictWithOtherKeys();
            rctlcapture_KeyAction.setConflictWithOtherKeys();
        }
        return false;
    }

    /** Reports each key press / release to pvr_manager ("vr2d_key_event"). */
    private void sendKeyToPvrManager(KeyEvent event) {
        int handkeycode = event.getKeyCode();
        if (handkeycode == KeyEvent.KEYCODE_BACK && event.getScanCode() == 10001) {
            return;
        }
        try {
            if (mPvrManagerService == null || mPvrManagerService.asBinder() == null
                    || !mPvrManagerService.asBinder().isBinderAlive()) {
                if (ServiceManager.getService("pvr_manager") != null) {
                    mPvrManagerService = IPvrManagerService.Stub.asInterface(
                            ServiceManager.getService("pvr_manager"));
                } else {
                    Slog.w(TAG, "pvr_manager has not been added to ServiceManager,do nothing.");
                }
            }
            if (mPvrManagerService != null && event.getRepeatCount() == 0) {
                if (handkeycode == 1001 && hmdconfirm_KeyAction.defined == 1) {
                    Slog.w(TAG, "confirm key is defined so do nothing");
                } else {
                    mPvrManagerService.sendPvrMessages("vr2d_key_event",
                            event.getAction() + ":" + handkeycode + ":" + event.getDeviceId());
                }
            }
            if (willSendKeyBC) {
                sendKeyBroadCast(handkeycode, -1, event.getAction());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /** Tap / double-tap / long-press state and actions of one PICO key. */
    private class SystemKeyAction {
        public String action;
        public boolean canceled;
        private boolean conflictWithOtherKeys;
        public int defined;
        public int displayId;
        public int doubletap_action;
        public int doubletap_action_time;
        public boolean down;
        public int enable;
        public boolean enableDoubleTap;
        public boolean isConsumed;
        public boolean isLongPressed;
        public boolean isPressed;
        public int keycode;
        public int longpress_action;
        public int longpress_action_time;
        public boolean mDoubleTapPending;
        public long mHomeDownTime;
        public int repeatCount;
        public boolean tapMessageHold;
        public int tap_action;

        private SystemKeyAction() {
            enable = -1;
            defined = 0;
            tap_action = -1;
            doubletap_action = -1;
            longpress_action = -1;
            doubletap_action_time = 300;
            longpress_action_time = 500;
            action = "";
            enableDoubleTap = true;
            down = false;
            canceled = false;
            displayId = -1;
            repeatCount = -1;
            keycode = -1;
            mHomeDownTime = -1L;
            mDoubleTapPending = false;
            isPressed = false;
            isLongPressed = false;
            isConsumed = false;
            tapMessageHold = true;
            conflictWithOtherKeys = false;
        }

        public void init() {
        }

        public void setConflictWithOtherKeys() {
            conflictWithOtherKeys = true;
            cancelKeyTapActionDelay(keycode);
            mDoubleTapPending = false;
            isPressed = false;
            isLongPressed = false;
            isConsumed = false;
        }

        public void handleEvent(KeyEvent event) {
            doKeyAction(event);
            down = event.getAction() == KeyEvent.ACTION_DOWN;
            canceled = event.isCanceled();
            displayId = event.getDisplayId();
            repeatCount = event.getRepeatCount();
            keycode = event.getKeyCode();
            if ((down && repeatCount == 0) || !down) {
                conflictWithOtherKeys = false;
            }
        }

        public void doKeyAction(KeyEvent event) {
            if (conflictWithOtherKeys) {
                return;
            }
            Slog.d(TAG, "doKeyAction event: " + event + " mDoubleTapPending is "
                    + mDoubleTapPending + ",longpress_action_time=" + longpress_action_time
                    + ",doubletap_action_time=" + doubletap_action_time + ",isConsumed="
                    + isConsumed);
            if (mDoubleTapPending) {
                if (event.getAction() == KeyEvent.ACTION_DOWN) {
                    isPressed = true;
                    mHomeDownTime = -1L;
                    mDoubleTapPending = true;
                    isLongPressed = false;
                    cancelKeyTapActionDelay(event.getKeyCode());
                    return;
                }
                isPressed = false;
                mHomeDownTime = -1L;
                mDoubleTapPending = false;
                isLongPressed = false;
                doKeyDoubleTapAction();
                return;
            }
            if (event.getAction() == KeyEvent.ACTION_DOWN) {
                isPressed = true;
                mDoubleTapPending = false;
                if (event.getRepeatCount() == 0) {
                    isConsumed = false;
                    mHomeDownTime = System.currentTimeMillis();
                    isLongPressed = false;
                    return;
                }
                if (!isLongPressed
                        && System.currentTimeMillis() - mHomeDownTime > longpress_action_time) {
                    isLongPressed = true;
                    doKeyLongPressAction();
                }
                return;
            }
            isPressed = false;
            isLongPressed = false;
            if (!isConsumed) {
                if (!enableDoubleTap) {
                    mHomeDownTime = -1L;
                    doKeyTapAction();
                } else if (System.currentTimeMillis() - mHomeDownTime < doubletap_action_time) {
                    mDoubleTapPending = true;
                    mHomeDownTime = -1L;
                    doKeyTapActionDelay(event.getKeyCode());
                }
            }
            mHomeDownTime = -1L;
        }

        public void cancelKeyTapActionDelay(int keycode) {
            if (DEBUG) {
                Slog.d(TAG, "we cancelKeyTapActionDelay " + keycode);
            }
            if (keycode == KeyEvent.KEYCODE_HOME) {
                if (tapMessageHold) {
                    mKeyActionHandler.removeMessages(MSG_HMD_HOME_TAP);
                    tapMessageHold = false;
                }
                return;
            }
            if (keycode == KeyEvent.KEYCODE_CAMERA) {
                if (tapMessageHold) {
                    mKeyActionHandler.removeMessages(MSG_RCAPTURE_TAP);
                    tapMessageHold = false;
                }
                return;
            }
            if (keycode == LEFT_CONTROLLER_KEYCODE) {
                if (tapMessageHold) {
                    mKeyActionHandler.removeMessages(MSG_LCONTROLLER_HOME_TAP);
                    tapMessageHold = false;
                }
                return;
            }
            if (keycode == RIGHT_CONTROLLER_KEYCODE && tapMessageHold) {
                mKeyActionHandler.removeMessages(MSG_RCONTROLLER_HOME_TAP);
                tapMessageHold = false;
            }
        }

        public void doKeyTapActionDelay(int keycode) {
            Message msg = new Message();
            if (keycode == KeyEvent.KEYCODE_HOME) {
                msg.what = MSG_HMD_HOME_TAP;
                tapMessageHold = true;
                mKeyActionHandler.sendMessageDelayed(msg, doubletap_action_time);
                return;
            }
            if (keycode == KeyEvent.KEYCODE_CAMERA) {
                msg.what = MSG_RCAPTURE_TAP;
                tapMessageHold = true;
                mKeyActionHandler.sendMessageDelayed(msg, doubletap_action_time);
            } else if (keycode == LEFT_CONTROLLER_KEYCODE) {
                msg.what = MSG_LCONTROLLER_HOME_TAP;
                tapMessageHold = true;
                mKeyActionHandler.sendMessageDelayed(msg, doubletap_action_time);
            } else if (keycode == RIGHT_CONTROLLER_KEYCODE) {
                msg.what = MSG_RCONTROLLER_HOME_TAP;
                tapMessageHold = true;
                mKeyActionHandler.sendMessageDelayed(msg, doubletap_action_time);
            }
        }

        public void doKeyTapAction() {
            Slog.i(TAG, "we doKeyTapAction " + keycode + " , resume " + isConsumed);
            notifyHomeKeyActionIfNeeded(keycode, 1);
            tapMessageHold = false;
            mDoubleTapPending = false;
            // Factory PICO OS 5.13.7: HOME and both controller HOME keys share the preview check,
            // including the build project test ("phoenix".equals("phoenix"), always true), so the
            // screenshot preview shortcut never runs for them.
            switch (keycode) {
                case KeyEvent.KEYCODE_HOME:
                case LEFT_CONTROLLER_KEYCODE:
                case RIGHT_CONTROLLER_KEYCODE:
                    if (SystemProperties.getInt("pvr.screenshot.preview", 0) == 1
                            && !Features.PROJECT_PHOENIX.equals(BUILD_PROJECT)) {
                        Slog.i(TAG, "doKeyTapAction shortcut preview");
                        launchScreenAction("pvr.intent.action.SCREEN_SHOT", "system_key");
                        isConsumed = true;
                        return;
                    }
                    break;
                case KeyEvent.KEYCODE_CAMERA:
                    if (SettingsObserverExt.getInstance().isSetupWizardComplete()) {
                        launchScreenAction("pvr.intent.action.SCREEN_SHOT", "capture_key");
                        isConsumed = true;
                        return;
                    }
                    break;
            }
            if (!isConsumed) {
                isConsumed = true;
                doKeyActionReal(tap_action);
            }
        }

        public void doKeyDoubleTapAction() {
            if (DEBUG) {
                Slog.i(TAG, "we doKeyDoubleTapAction " + keycode);
            }
            notifyHomeKeyActionIfNeeded(keycode, 2);
            if (!isConsumed) {
                isConsumed = true;
                doKeyActionReal(doubletap_action);
            }
        }

        public void doKeyLongPressAction() {
            notifyHomeKeyActionIfNeeded(keycode, 3);
            if (!isConsumed) {
                isConsumed = true;
                doKeyActionReal(longpress_action);
            }
        }

        /** ToB: HOME key actions (1 tap, 2 double tap, 3 long press) are broadcast. */
        private void notifyHomeKeyActionIfNeeded(final int keycode, final int action) {
            if (mIsToBDevice && !isConsumed) {
                if (keycode == KeyEvent.KEYCODE_HOME || keycode == LEFT_CONTROLLER_KEYCODE
                        || keycode == RIGHT_CONTROLLER_KEYCODE) {
                    mBase.mHandler.post(() -> {
                        Intent intent = new Intent("pxr.intent.action.home_key");
                        intent.putExtra("keycode", keycode);
                        intent.putExtra("action", action);
                        intent.addFlags(Intent.FLAG_RECEIVER_REGISTERED_ONLY);
                        mBase.mContext.sendBroadcastAsUser(intent, UserHandle.CURRENT);
                        Log.d(TAG, "notifyHomeKeyAction keycode : " + keycode + ", action : "
                                + action);
                    });
                }
            }
        }

        private void launchPUItobLauncherFunc(int value) {
            Intent intent = new Intent("pvr.intent.action.LAUNCHER_MAIN");
            intent.putExtra("func", value);
            intent.setPackage(SystemExt.sCurrentPkg);
            mBase.mContext.startService(intent);
        }

        private void launchShortcutCheck() {
            IExtActivityTaskManagerInternal atmService =
                    mBase.mActivityTaskManagerInternal.getExt();
            ActivityInfo topActivityInfo = atmService.getTopAppExt(Display.DEFAULT_DISPLAY);
            Slog.d(TAG, "launchShortcutCheck topActivity : " + topActivityInfo);
            if (topActivityInfo != null && !topIsVrPermissionActivity()) {
                dispatchHomeToNS();
            }
        }

        /** Factory method without a caller (the shortcut panel service of older products). */
        private void launchShortcut() {
            Slog.d(TAG, "launchShortcut");
            Intent intent = new Intent();
            intent.setPackage("com.pvr.shortcut");
            intent.setClassName("com.pvr.shortcut", "com.pvr.shortcut.service.ShortcutService");
            intent.putExtra("show_shortcut", true);
            mBase.mContext.startService(intent);
        }

        private void launchRecenter() {
            Slog.i(TAG, "launchRecenter");
            mBase.sendCloseSystemWindows("recenter");
        }

        private void launchScreenAction(String screenaction, String from) {
            Slog.d(TAG, "launchScreenAction " + screenaction + " from " + from);
            if (Features.isKeyguardEnabled()
                    && KeyStore.getInstance().state() == KeyStore.State.LOCKED) {
                Intent i = new Intent("pvr.intent.action.vrdisplay");
                i.setPackage("com.pvr.vrdisplay");
                i.putExtra("action_type", 93);
                i.putExtra("remind_type", 3);
                mBase.mContext.startService(i);
                return;
            }
            if ("capture_key".equals(from)
                    && SettingsObserverExt.getInstance().mDisableCaptureKeyAppShowing
                    && !SettingsObserverExt.getInstance().mIsDockShowing) {
                if ("pvr.intent.action.SCREEN_SHOT".equals(screenaction)
                        && !SettingsObserverExt.getInstance().mIsScreenshotToastShowing) {
                    Slog.d(TAG, "ignore screenshot of "
                            + SettingsObserverExt.getInstance().mCurrentDefaultDisplayApp);
                    return;
                }
                if ("pvr.intent.action.SCREEN_RECORD".equals(screenaction)) {
                    Slog.d(TAG, "ignore screen record of "
                            + SettingsObserverExt.getInstance().mCurrentDefaultDisplayApp);
                    return;
                }
            }
            Intent intent = new Intent(screenaction);
            intent.setPackage(SCREEN_RECOARD_CAP_PACKAGE_DEFAULT);
            intent.putExtra("from", from);
            Slog.d(TAG, "launchScreenAction intent : " + intent + " from : " + from);
            mBase.mContext.startService(intent);
        }

        private boolean doKeyActionReal(int realaction) {
            if (DEBUG) {
                Slog.w(TAG, "doKeyActionReal " + realaction);
            }
            if (realaction == ACTION_PXR_UNDEFINED) {
                return true;
            }
            if (realaction == ACTION_PXR_RECENTER) {
                launchRecenter();
                return true;
            }
            if (realaction == ACTION_PXR_GLOBAL_NAVIGATION) {
                launchShortcutCheck();
                return true;
            }
            if (realaction == ACTION_PXR_GOHOME) {
                launchPUItobLauncherFunc(ACTION_PXR_GOHOME);
                return true;
            }
            if (realaction == ACTION_PXR_DASHBOARD) {
                launchQuickSettings();
                return true;
            }
            if (realaction == ACTION_PXR_SCREENCAP) {
                if (keycode == KeyEvent.KEYCODE_CAMERA
                        && !SettingsObserverExt.getInstance().isSetupWizardComplete()) {
                    return false;
                }
                launchScreenAction("pvr.intent.action.SCREEN_SHOT",
                        keycode != KeyEvent.KEYCODE_CAMERA ? "system_key" : "capture_key");
                return true;
            }
            if (realaction != ACTION_PXR_SCREENRECOARD) {
                return false;
            }
            if (keycode == KeyEvent.KEYCODE_CAMERA
                    && !SettingsObserverExt.getInstance().isSetupWizardComplete()) {
                return false;
            }
            launchScreenAction("pvr.intent.action.SCREEN_RECORD",
                    keycode != KeyEvent.KEYCODE_CAMERA ? "system_key" : "capture_key");
            return true;
        }

        public void reset() {
            enable = -1;
            defined = -1;
            tap_action = -1;
            doubletap_action = -1;
            longpress_action = -1;
            doubletap_action_time = 300;
            longpress_action_time = 500;
            down = false;
            canceled = false;
            displayId = -1;
            repeatCount = -1;
            mHomeDownTime = -1L;
            mDoubleTapPending = false;
            isLongPressed = false;
            isConsumed = false;
        }

        private void launchQuickSettings() {
            Slog.w(TAG, "launchQuickSettings !");
            Intent intent = new Intent("pui.settings.action.QUICK_SETINGS");
            intent.setPackage("com.picovr.settings");
            mBase.startActivityAsUser(intent, UserHandle.CURRENT_OR_SELF);
        }
    }

    private class SystemKeyHandler extends Handler {
        private SystemKeyHandler() {
        }

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_HMD_HOME_TAP:
                    hmdhome_KeyAction.doKeyTapAction();
                    break;
                case MSG_LCONTROLLER_HOME_TAP:
                    lctlhome_KeyAction.doKeyTapAction();
                    break;
                case MSG_RCONTROLLER_HOME_TAP:
                    rctlhome_KeyAction.doKeyTapAction();
                    break;
                case MSG_RCAPTURE_TAP:
                    rctlcapture_KeyAction.doKeyTapAction();
                    break;
                default:
                    break;
            }
        }
    }

    private String getPropValue(Properties prop, String key) {
        if (!prop.containsKey(key)) {
            return null;
        }
        return prop.get(key).toString();
    }

    /**
     * ToB devices (ro.pxr.externalfunc=1) only: key actions from the first existing key
     * configuration file (MDM, user, OEM, system default). Generic home values apply to the
     * headset and both controllers, then the hmd_/lctl_/rctl_ values override them.
     */
    private void updateUserKeyConfig() {
        Slog.d(TAG, "updateSystemKeyConfig call");
        try {
            boolean enabletob = SystemProperties.getInt("ro.pxr.externalfunc", -1) == 1;
            if (!enabletob) {
                return;
            }
            Slog.d(TAG, "updateSystemKeyConfig enabletob so check key config file");
            File oemKeyConfigFile = new File(OEM_KEY_CONFIG_PATH);
            File keyConfigFile = new File(MDM_KEY_CONFIG_PATH);
            File oldkeyConfigFile = new File(USER_KEY_CONFIG_PATH);
            File syskeyConfigFile = new File(USER_KEY_CONFIG_PATH_DEFAULT);
            if (keyConfigFile.exists()) {
                keyConfigFile = new File(MDM_KEY_CONFIG_PATH);
                Slog.i(TAG, "mdm init user key config file exists");
            } else if (oldkeyConfigFile.exists()) {
                keyConfigFile = new File(USER_KEY_CONFIG_PATH);
                Slog.i(TAG, "system init user key config file exists");
            } else if (oemKeyConfigFile.exists()) {
                keyConfigFile = new File(OEM_KEY_CONFIG_PATH);
                Slog.i(TAG, "oem init user key config file exists");
            } else if (syskeyConfigFile.exists()) {
                keyConfigFile = new File(USER_KEY_CONFIG_PATH_DEFAULT);
                Slog.i(TAG, "system default key config file exists");
            }
            if (!keyConfigFile.exists()) {
                Slog.i(TAG, "key config file do not exists, so we do not do any change to key"
                        + " action");
                return;
            }
            Slog.i(TAG, "we get key config file so change key action");
            Properties keyConfig = new Properties();
            keyConfig.load(new FileInputStream(keyConfigFile));
            singleTapOnHomeUserConfig = getPropValue(keyConfig,
                    USER_KEY_CONFIG_ACTION_HOMESINGLETAP);
            doubleTapOnHomeUserConfig = getPropValue(keyConfig,
                    USER_KEY_CONFIG_ACTION_HOMEDOUBLETAP);
            longPressOnHomeUserConfig = getPropValue(keyConfig,
                    USER_KEY_CONFIG_ACTION_HOMELONGPRESS);
            doubleTapOnHomeTimeConfig = getPropValue(keyConfig,
                    USER_KEY_CONFIG_TIME_HOMEDOUBLETAP);
            longPressOnHomeTimeConfig = getPropValue(keyConfig, USER_KEY_CONFIG_TIME_HOMELONGPRESS);
            longPressOnPowerTimeConfig = getPropValue(keyConfig,
                    USER_KEY_CONFIG_TIME_POWERLONGPRESS);
            singleTapOnPowerUserConfig = getPropValue(keyConfig,
                    USER_KEY_CONFIG_ACTION_POWERSINGLETAP);
            longPressOnPowerUserConfig = getPropValue(keyConfig,
                    USER_KEY_CONFIG_ACTION_POWERLONGPRESS);
            hmd_singleTapOnHomeUserConfig = getPropValue(keyConfig,
                    HMD_USER_KEY_CONFIG_ACTION_HOMESINGLETAP);
            hmd_doubleTapOnHomeUserConfig = getPropValue(keyConfig,
                    HMD_USER_KEY_CONFIG_ACTION_HOMEDOUBLETAP);
            hmd_longPressOnHomeUserConfig = getPropValue(keyConfig,
                    HMD_USER_KEY_CONFIG_ACTION_HOMELONGPRESS);
            hmd_doubleTapOnHomeTimeConfig = getPropValue(keyConfig,
                    HMD_USER_KEY_CONFIG_TIME_HOMEDOUBLETAP);
            hmd_longPressOnHomeTimeConfig = getPropValue(keyConfig,
                    HMD_USER_KEY_CONFIG_TIME_HOMELONGPRESS);
            lctl_singleTapOnHomeUserConfig = getPropValue(keyConfig,
                    LCTL_USER_KEY_CONFIG_ACTION_HOMESINGLETAP);
            lctl_doubleTapOnHomeUserConfig = getPropValue(keyConfig,
                    LCTL_USER_KEY_CONFIG_ACTION_HOMEDOUBLETAP);
            lctl_longPressOnHomeUserConfig = getPropValue(keyConfig,
                    LCTL_USER_KEY_CONFIG_ACTION_HOMELONGPRESS);
            lctl_doubleTapOnHomeTimeConfig = getPropValue(keyConfig,
                    LCTL_USER_KEY_CONFIG_TIME_HOMEDOUBLETAP);
            lctl_longPressOnHomeTimeConfig = getPropValue(keyConfig,
                    LCTL_USER_KEY_CONFIG_TIME_HOMELONGPRESS);
            rctl_singleTapOnHomeUserConfig = getPropValue(keyConfig,
                    RCTL_USER_KEY_CONFIG_ACTION_HOMESINGLETAP);
            rctl_doubleTapOnHomeUserConfig = getPropValue(keyConfig,
                    RCTL_USER_KEY_CONFIG_ACTION_HOMEDOUBLETAP);
            rctl_longPressOnHomeUserConfig = getPropValue(keyConfig,
                    RCTL_USER_KEY_CONFIG_ACTION_HOMELONGPRESS);
            rctl_doubleTapOnHomeTimeConfig = getPropValue(keyConfig,
                    RCTL_USER_KEY_CONFIG_TIME_HOMEDOUBLETAP);
            rctl_longPressOnHomeTimeConfig = getPropValue(keyConfig,
                    RCTL_USER_KEY_CONFIG_TIME_HOMELONGPRESS);
            enterKeyConfig = getPropValue(keyConfig, USER_KEY_CONFIG_ACTION_CONFIRM);
            volumeupKeyConfig = getPropValue(keyConfig, USER_KEY_CONFIG_ACTION_VOLUMEUP);
            volumedownKeyConfig = getPropValue(keyConfig, USER_KEY_CONFIG_ACTION_VOLUMEDOWN);
            backKeyConfig = getPropValue(keyConfig, USER_KEY_CONFIG_ACTION_BACK);
            mSingleTapOnHomeUserDefined = singleTapOnHomeUserConfig != null;
            mDoubleTapOnHomeUserDefined = doubleTapOnHomeUserConfig != null;
            mLongPressOnHomeUserDefined = longPressOnHomeUserConfig != null;
            mDoubleTapOnHomeTimeDefined = doubleTapOnHomeTimeConfig != null;
            mLongPressOnHomeTimeDefined = longPressOnHomeTimeConfig != null;
            mLongPressOnPowerTimeDefined = longPressOnPowerTimeConfig != null;
            mSingleTapOnPowerUserDefined = singleTapOnPowerUserConfig != null;
            mLongPressOnPowerUserDefined = longPressOnPowerUserConfig != null;
            hmd_mSingleTapOnHomeUserDefined = hmd_singleTapOnHomeUserConfig != null;
            hmd_mDoubleTapOnHomeUserDefined = hmd_doubleTapOnHomeUserConfig != null;
            hmd_mLongPressOnHomeUserDefined = hmd_longPressOnHomeUserConfig != null;
            hmd_mDoubleTapOnHomeTimeDefined = hmd_doubleTapOnHomeTimeConfig != null;
            hmd_mLongPressOnHomeTimeDefined = hmd_longPressOnHomeTimeConfig != null;
            lctl_mSingleTapOnHomeUserDefined = lctl_singleTapOnHomeUserConfig != null;
            lctl_mDoubleTapOnHomeUserDefined = lctl_doubleTapOnHomeUserConfig != null;
            lctl_mLongPressOnHomeUserDefined = lctl_longPressOnHomeUserConfig != null;
            lctl_mDoubleTapOnHomeTimeDefined = lctl_doubleTapOnHomeTimeConfig != null;
            lctl_mLongPressOnHomeTimeDefined = lctl_longPressOnHomeTimeConfig != null;
            rctl_mSingleTapOnHomeUserDefined = rctl_singleTapOnHomeUserConfig != null;
            rctl_mDoubleTapOnHomeUserDefined = rctl_doubleTapOnHomeUserConfig != null;
            rctl_mLongPressOnHomeUserDefined = rctl_longPressOnHomeUserConfig != null;
            rctl_mDoubleTapOnHomeTimeDefined = rctl_doubleTapOnHomeTimeConfig != null;
            rctl_mLongPressOnHomeTimeDefined = rctl_longPressOnHomeTimeConfig != null;
            enterKeyConfigDefined = enterKeyConfig != null;
            volumeupKeyConfigDefined = volumeupKeyConfig != null;
            volumedownKeyConfigDefined = volumedownKeyConfig != null;
            backKeyConfigDefined = backKeyConfig != null;
            Slog.i(TAG, "system init user key config mSingleTapOnHomeUserDefined = "
                    + singleTapOnHomeUserConfig);
            Slog.i(TAG, "system init user key config mDoubleTapOnHomeUserDefined = "
                    + doubleTapOnHomeUserConfig);
            Slog.i(TAG, "system init user key config mLongPressOnHomeUserDefined = "
                    + longPressOnHomeUserConfig);
            Slog.i(TAG, "system init user key config mDoubleTapOnHomeTimeDefined = "
                    + doubleTapOnHomeTimeConfig);
            Slog.i(TAG, "system init user key config mLongPressOnHomeTimeDefined = "
                    + longPressOnHomeTimeConfig);
            Slog.i(TAG, "system init user key config mLongPressOnPowerTimeDefined = "
                    + longPressOnPowerTimeConfig);
            Slog.i(TAG, "system init user key config mSingleTapOnPowerUserDefined = "
                    + singleTapOnPowerUserConfig);
            Slog.i(TAG, "system init user key config mLongPressOnPowerUserDefined = "
                    + longPressOnPowerUserConfig);
            if (mLongPressOnPowerTimeDefined) {
                hmdpower_KeyAction.defined = 1;
                hmdpower_KeyAction.longpress_action_time =
                        Integer.valueOf(longPressOnPowerTimeConfig).intValue();
            }
            if (mSingleTapOnPowerUserDefined) {
                hmdpower_KeyAction.defined = 1;
                hmdpower_KeyAction.tap_action = 0;
            }
            if (mLongPressOnPowerUserDefined) {
                hmdpower_KeyAction.defined = 1;
                hmdpower_KeyAction.longpress_action = 0;
            }
            hmdconfirm_KeyAction.defined = 0;
            if (enterKeyConfigDefined) {
                hmdconfirm_KeyAction.defined = 1;
            }
            hmdvolumeup_KeyAction.defined = 0;
            if (volumeupKeyConfigDefined) {
                hmdvolumeup_KeyAction.defined = 1;
            }
            hmdvolumedown_KeyAction.defined = 0;
            if (volumedownKeyConfigDefined) {
                hmdvolumedown_KeyAction.defined = 1;
            }
            hmdback_KeyAction.defined = 0;
            if (backKeyConfigDefined) {
                hmdback_KeyAction.defined = 1;
            }
            if (mSingleTapOnHomeUserDefined) {
                hmdhome_KeyAction.tap_action = Integer.valueOf(singleTapOnHomeUserConfig).intValue();
                lctlhome_KeyAction.tap_action =
                        Integer.valueOf(singleTapOnHomeUserConfig).intValue();
                rctlhome_KeyAction.tap_action =
                        Integer.valueOf(singleTapOnHomeUserConfig).intValue();
            }
            if (hmd_mSingleTapOnHomeUserDefined) {
                hmdhome_KeyAction.tap_action =
                        Integer.valueOf(hmd_singleTapOnHomeUserConfig).intValue();
            }
            if (lctl_mSingleTapOnHomeUserDefined) {
                lctlhome_KeyAction.tap_action =
                        Integer.valueOf(lctl_singleTapOnHomeUserConfig).intValue();
            }
            if (rctl_mSingleTapOnHomeUserDefined) {
                rctlhome_KeyAction.tap_action =
                        Integer.valueOf(rctl_singleTapOnHomeUserConfig).intValue();
            }
            if (mDoubleTapOnHomeUserDefined) {
                hmdhome_KeyAction.doubletap_action =
                        Integer.valueOf(doubleTapOnHomeUserConfig).intValue();
                lctlhome_KeyAction.doubletap_action =
                        Integer.valueOf(doubleTapOnHomeUserConfig).intValue();
                rctlhome_KeyAction.doubletap_action =
                        Integer.valueOf(doubleTapOnHomeUserConfig).intValue();
            }
            if (hmd_mDoubleTapOnHomeUserDefined) {
                hmdhome_KeyAction.doubletap_action =
                        Integer.valueOf(hmd_doubleTapOnHomeUserConfig).intValue();
            }
            if (lctl_mDoubleTapOnHomeUserDefined) {
                lctlhome_KeyAction.doubletap_action =
                        Integer.valueOf(lctl_doubleTapOnHomeUserConfig).intValue();
            }
            if (rctl_mDoubleTapOnHomeUserDefined) {
                rctlhome_KeyAction.doubletap_action =
                        Integer.valueOf(rctl_doubleTapOnHomeUserConfig).intValue();
            }
            if (mLongPressOnHomeUserDefined) {
                hmdhome_KeyAction.longpress_action =
                        Integer.valueOf(longPressOnHomeUserConfig).intValue();
                lctlhome_KeyAction.longpress_action =
                        Integer.valueOf(longPressOnHomeUserConfig).intValue();
                rctlhome_KeyAction.longpress_action =
                        Integer.valueOf(longPressOnHomeUserConfig).intValue();
            }
            if (hmd_mLongPressOnHomeUserDefined) {
                hmdhome_KeyAction.longpress_action =
                        Integer.valueOf(hmd_longPressOnHomeUserConfig).intValue();
            }
            if (lctl_mLongPressOnHomeUserDefined) {
                lctlhome_KeyAction.longpress_action =
                        Integer.valueOf(lctl_longPressOnHomeUserConfig).intValue();
            }
            if (rctl_mLongPressOnHomeUserDefined) {
                rctlhome_KeyAction.longpress_action =
                        Integer.valueOf(rctl_longPressOnHomeUserConfig).intValue();
            }
            if (mDoubleTapOnHomeTimeDefined) {
                hmdhome_KeyAction.doubletap_action_time =
                        Integer.valueOf(doubleTapOnHomeTimeConfig).intValue();
                lctlhome_KeyAction.doubletap_action_time =
                        Integer.valueOf(doubleTapOnHomeTimeConfig).intValue();
                rctlhome_KeyAction.doubletap_action_time =
                        Integer.valueOf(doubleTapOnHomeTimeConfig).intValue();
            }
            if (hmd_mDoubleTapOnHomeTimeDefined) {
                hmdhome_KeyAction.doubletap_action_time =
                        Integer.valueOf(hmd_doubleTapOnHomeTimeConfig).intValue();
            }
            if (lctl_mDoubleTapOnHomeTimeDefined) {
                lctlhome_KeyAction.doubletap_action_time =
                        Integer.valueOf(lctl_doubleTapOnHomeTimeConfig).intValue();
            }
            if (rctl_mDoubleTapOnHomeTimeDefined) {
                rctlhome_KeyAction.doubletap_action_time =
                        Integer.valueOf(rctl_doubleTapOnHomeTimeConfig).intValue();
            }
            if (mLongPressOnHomeTimeDefined) {
                hmdhome_KeyAction.longpress_action_time =
                        Integer.valueOf(longPressOnHomeTimeConfig).intValue();
                lctlhome_KeyAction.longpress_action_time =
                        Integer.valueOf(longPressOnHomeTimeConfig).intValue();
                rctlhome_KeyAction.longpress_action_time =
                        Integer.valueOf(longPressOnHomeTimeConfig).intValue();
            }
            if (hmd_mLongPressOnHomeTimeDefined) {
                hmdhome_KeyAction.longpress_action_time =
                        Integer.valueOf(hmd_longPressOnHomeTimeConfig).intValue();
            }
            if (lctl_mLongPressOnHomeTimeDefined) {
                lctlhome_KeyAction.longpress_action_time =
                        Integer.valueOf(lctl_longPressOnHomeTimeConfig).intValue();
            }
            if (rctl_mLongPressOnHomeTimeDefined) {
                rctlhome_KeyAction.longpress_action_time =
                        Integer.valueOf(rctl_longPressOnHomeTimeConfig).intValue();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private boolean topIsVrPermissionActivity() {
        try {
            ActivityManager.StackInfo stackInfo =
                    ActivityManager.getService().getFocusedStackInfo();
            if (stackInfo != null && stackInfo.topActivity != null
                    && TextUtils.equals(stackInfo.topActivity.getClassName(),
                            "com.android.packageinstaller.permission.ui.pico"
                                    + ".VrGrantPermissionsActivity")) {
                return true;
            }
            return false;
        } catch (RemoteException e) {
            e.printStackTrace();
            return false;
        }
    }

    @Override
    public boolean isShortctShowOn3dApp(Context context, int repeatCount) {
        if (repeatCount == 0) {
            mShortctShowOn3d = Settings.Global.getInt(context.getContentResolver(),
                    SHORTCT_SHOW_ON_3D, 0) == 1;
        }
        return mShortctShowOn3d;
    }

    /** Volume keys while the shortcut panel shows over a 3D app: adjust the volume directly. */
    @Override
    public void interceptVolumeEventAndApply(Context context, KeyEvent event) {
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
        int keyCode = event.getKeyCode();
        String pkgName = context.getOpPackageName();
        if (keyCode == KeyEvent.KEYCODE_VOLUME_UP) {
            try {
                mAudioService.adjustSuggestedStreamVolume(AudioManager.ADJUST_RAISE,
                        AudioManager.USE_DEFAULT_STREAM_TYPE, flags, pkgName, TAG);
            } catch (Exception e) {
                Slog.e(TAG, "Error dispatching volume up in dispatchTvAudioEvent.", e);
            }
            return;
        }
        if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) {
            try {
                mAudioService.adjustSuggestedStreamVolume(AudioManager.ADJUST_LOWER,
                        AudioManager.USE_DEFAULT_STREAM_TYPE, flags, pkgName, TAG);
            } catch (Exception e) {
                Slog.e(TAG, "Error dispatching volume down in dispatchTvAudioEvent.", e);
            }
            return;
        }
        if (keyCode == KeyEvent.KEYCODE_VOLUME_MUTE) {
            try {
                if (event.getRepeatCount() == 0) {
                    mAudioService.adjustSuggestedStreamVolume(AudioManager.ADJUST_TOGGLE_MUTE,
                            AudioManager.USE_DEFAULT_STREAM_TYPE, flags, pkgName, TAG);
                }
            } catch (Exception e) {
                Slog.e(TAG, "Error dispatching mute in dispatchTvAudioEvent.", e);
            }
        }
    }

    /**
     * interceptKeyBeforeDispatching: HOME is consumed unless it comes from a gesture hand (then
     * it goes to the app). -1 = no change.
     */
    @Override
    public long adjustResultFromInterceptKeyBeforeDispatchingInner(
            WindowManagerPolicy.WindowState win, KeyEvent event, int policyFlags, long result) {
        if (event != null && event.getKeyCode() == KeyEvent.KEYCODE_HOME) {
            int deviceId = event.getDeviceId();
            if (deviceId == 20001 || deviceId == 20002) {
                Slog.w(TAG, "adjustResultFromInterceptKeyBeforeDispatchingInner");
                return 0L;
            }
            return -1L;
        }
        return -1L;
    }

    /**
     * ToB, not phoenix (dead code on this product, as on the factory): volume down + confirm
     * pressed within 300 ms of each other send virtual key 1005 to the API layer clients and
     * open PICO VR settings.
     */
    private boolean handleCompositeKey(KeyEvent event) {
        int keyCode = event.getKeyCode();
        long eventTime = event.getEventTime();
        boolean down = event.getAction() == KeyEvent.ACTION_DOWN;
        if (keyCode != KeyEvent.KEYCODE_VOLUME_DOWN) {
            if (keyCode == 1001) {
                if (down) {
                    if (mCompositeKeyTriggered) {
                        return true;
                    }
                    mConFirmKeyTriggered = true;
                    mConFirmKeyDownTime = event.getDownTime();
                    if (eventTime - mConFirmKeyDownTime >= 300) {
                        return false;
                    }
                    if (mVolumeDownKeyTriggered && eventTime - mVolumeDownKeyDownTime < 300) {
                        mCompositeKeyTriggered = true;
                        sendVirtualKey(MSG_HIDE_STATUS_BAR_TIMEOUT, KeyEvent.ACTION_DOWN);
                        launchVRSettings();
                    }
                    return true;
                }
                mConFirmKeyTriggered = false;
                if (mCompositeKeyTriggered) {
                    if (!mVolumeDownKeyTriggered && !mConFirmKeyTriggered) {
                        mCompositeKeyTriggered = false;
                    }
                    return true;
                }
            }
        } else {
            if (down) {
                if (mCompositeKeyTriggered) {
                    return true;
                }
                mVolumeDownKeyTriggered = true;
                mVolumeDownKeyDownTime = event.getDownTime();
                if (eventTime - mVolumeDownKeyDownTime >= 300) {
                    return false;
                }
                if (mConFirmKeyTriggered && eventTime - mConFirmKeyDownTime < 300) {
                    mCompositeKeyTriggered = true;
                    sendVirtualKey(MSG_HIDE_STATUS_BAR_TIMEOUT, KeyEvent.ACTION_DOWN);
                    launchVRSettings();
                }
                return true;
            }
            mVolumeDownKeyTriggered = false;
            if (mCompositeKeyTriggered) {
                if (!mVolumeDownKeyTriggered && !mConFirmKeyTriggered) {
                    mCompositeKeyTriggered = false;
                }
                return true;
            }
        }
        return false;
    }

    /** powerPress: the short press does nothing (key config, psensor near, DP mode). */
    @Override
    public boolean interruptPowerPress() {
        if (getPowerDefined() && getPowerTapStauts() == 0) {
            Log.w(TAG, "interruptPowerPress by power defined");
            return true;
        }
        if (SystemProperties.getInt("sys.pxr.psensor.status", 1) == 0
                && SystemProperties.getInt("persist.pxr.psensor.powermode", 1) == 1
                && isDefectiveDevice && mBase.isScreenOn()) {
            Log.w(TAG, "KeyEvent.KEYCODE_POWER press but no effect for psensor is near status");
            return true;
        }
        if (SystemProperties.getInt("sys.pxr.vxr7200.status", 0) != 1) {
            return false;
        }
        Log.w(TAG, "interruptPowerPress by vxr7200");
        return true;
    }

    @Override
    public boolean interruptPowerLongPress() {
        if (getPowerDefined() && getPowerLongPressStauts() == 0) {
            Log.w(TAG, "interruptPowerLongPress by power defined");
            return true;
        }
        return false;
    }

    /** DisplayHomeButtonHandler: HOME held for over 3 s is reported as pxr notification. */
    @Override
    public void sendTapHomeMsgIfNeeded(KeyEvent event) {
        int repeatCount = event.getRepeatCount();
        if (repeatCount == 0) {
            mHomeDownTime = System.currentTimeMillis();
            return;
        }
        if ((event.getFlags() & KeyEvent.FLAG_LONG_PRESS) != 0) {
            return;
        }
        long now = System.currentTimeMillis();
        long downTime = mHomeDownTime;
        mRepeatHomeTime = now - downTime;
        if (downTime != -1 && mRepeatHomeTime > 3000) {
            try {
                Context context = mBase.mContext;
                if (PxrNotificationService.getInstance(context) != null) {
                    Log.w(TAG, "long tap home ,send notification msg");
                    PxrNotificationService.getInstance(context).sendPxrMessage(
                            notification_msg_home_long_press, 1, "", (int) mRepeatHomeTime, "");
                } else {
                    Log.w(TAG, "pxr_notification is null ,do nothing ...");
                }
            } catch (RemoteException e) {
                Log.e(TAG, "invoke pxrnotification send home long press Exception:" + e);
            }
            mHomeDownTime = -1L;
        }
    }

    /**
     * checkAddPermission: apps marked as VR apps in their metadata need SYSTEM_ALERT_WINDOW for
     * system windows.
     */
    @Override
    public boolean needCheckSystemAlertWindowPermission(Context context,
            WindowManager.LayoutParams attrs, int callingUid) {
        ApplicationInfo applicationInfo;
        try {
            applicationInfo = context.getPackageManager().getApplicationInfoAsUser(
                    attrs.packageName, PackageManager.GET_META_DATA,
                    UserHandle.getUserId(callingUid));
        } catch (PackageManager.NameNotFoundException e) {
            applicationInfo = null;
        }
        String[] vrTagArray = {"com.picovr.type", "pvr.app.type"};
        if (applicationInfo != null && applicationInfo.metaData != null) {
            for (String tag : vrTagArray) {
                String value = applicationInfo.metaData.getString(tag);
                if (value != null) {
                    return true;
                }
            }
        }
        return false;
    }

    /** interceptKeyBeforeDispatchingInner: BACK held for over 8 s is reported. */
    @Override
    public void dispatchBackKeyTapMsg(KeyEvent event) {
        int keyCode = event.getKeyCode();
        if (keyCode != KeyEvent.KEYCODE_BACK) {
            return;
        }
        if (event.getRepeatCount() == 0) {
            mBackDownTime = System.currentTimeMillis();
            return;
        }
        long now = System.currentTimeMillis();
        long downTime = mBackDownTime;
        mRepeatBackTime = now - downTime;
        if (downTime != -1 && mRepeatBackTime > 8000) {
            try {
                if (PxrNotificationService.getInstance(mBase.mContext) != null) {
                    Log.w(TAG, "long tap back ,send notification msg");
                    PxrNotificationService.getInstance(mBase.mContext).sendPxrMessage(
                            notification_msg_back_long_press, 1, "", (int) mRepeatBackTime, "");
                } else {
                    Log.w(TAG, "pxr_notification is null ,do nothing ...");
                }
            } catch (RemoteException e) {
                Log.e(TAG, "invoke pxrnotification send back long press Exception:" + e);
            }
            mBackDownTime = -1L;
        }
    }

    @Override
    public boolean isPowerDisabledForHcit() {
        return mPowerDisabledForHcit;
    }

    /** Factory interface method without a caller in system_server. */
    @Override
    public void doMediaScan(Context context) {
        Bundle args = new Bundle();
        args.putString("volume", "external");
        Intent startScan = new Intent();
        startScan.putExtras(args);
        startScan.setComponent(new ComponentName("com.android.providers.media",
                "com.android.providers.media.MediaScannerService"));
        context.startService(startScan);
    }

    /** startDockOrHome: test mode goes to Launcher3 when it is installed. */
    @Override
    public boolean startLauncher3IfNeeded() {
        if (SystemProperties.getBoolean("persist.pxr.testmode", false)) {
            boolean isLauncher3Exists = false;
            try {
                mBase.mContext.getPackageManager().getApplicationInfo("com.android.launcher3", 0);
                isLauncher3Exists = true;
            } catch (Exception e) {
            }
            if (isLauncher3Exists) {
                Intent intent = new Intent();
                ComponentName comp = new ComponentName("com.android.launcher3",
                        "com.android.launcher3.Launcher");
                intent.setComponent(comp);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                        | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
                mBase.startActivityAsUser(intent, UserHandle.CURRENT_OR_SELF);
                return true;
            }
        }
        return false;
    }

    /** startDockOrHome: HOME goes to SystemExt instead of the Android home activity. */
    @Override
    public boolean launchDockIfNeeded() {
        dispatchHomeToNS();
        return true;
    }

    /** Tells SystemExt that HOME was pressed (it shows the dock / home panel). */
    private void dispatchHomeToNS() {
        Slog.w(TAG, "dispatchHomeToNS");
        Intent intent = new Intent(SystemExt.sAction);
        intent.setPackage(SystemExt.sCurrentPkg);
        mBase.mContext.startService(intent);
    }

    /** The power key always waits for a possible double press. */
    @Override
    public int getDefaultMaxMultiPressPowerCount() {
        return 2;
    }

    @Override
    public boolean denyBackKeyIn2DApp() {
        return Features.isDenyBackKeyIn2dApp();
    }

    /** Sends a synthetic key (down, keyCode) to the API layer clients. */
    private void sendVirtualKey(int keyCode, int action) {
        try {
            long time = SystemClock.uptimeMillis();
            KeyEvent event = KeyEvent.obtain(time, time, action, keyCode, 0, 0, 0, 0, 0, 0, 0,
                    null);
            ApiLayerService.getInstance().onKeyEvent(event);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
