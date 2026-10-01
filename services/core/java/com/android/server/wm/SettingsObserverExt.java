// Copyright 2026 Picomisu contributors
// SPDX-License-Identifier: Apache-2.0
package com.android.server.wm;

import android.app.ActivityManager;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Handler;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Slog;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

/**
 * PICO settings shared by the window manager and the window policy (factory PICO OS 5.13.7
 * com.android.server.wm.SettingsObserverExt, created by ExtActivityStartControllerImpl): setup
 * wizard completion, the ToB custom app and home, and the dock / screenshot toast / camera key
 * flags of the PICO launcher.
 */
public class SettingsObserverExt {
    private static final String PVR_SETUP_WIZARD_COMPLETE = "pvr.config.provision2.complete";
    private static final String SETTINGS_DISABLE_CAMERA_KEY = "pvr.app.data.disable_camera_key";
    private static final String SETTINGS_DOCK_SHOWING = "pvr.app.data.dock_visible_state";
    private static final String SETTINGS_SCREENSHOT_TOAST_SHOWING =
            "pvr.settings.screenshot_toast_showing";
    public static final String SETTINGS_TOB_CUSTOM_APP = "pvr_tob_config_custom_app";
    private static final String SETTINGS_TOB_CUSTOM_HOME = "pvr_tob_config_home";
    private static final String TAG = "SettingsObserverExt";
    private static final String TO_B_FILE_PATH = "/mnt/vendor/persist/license/active.flag";
    private static SettingsObserverExt mInstance;
    private Context mContext;
    public String mCurrentDefaultDisplayApp;
    private Handler mHandler;
    private SettingsObserver mSettingsObserver;
    public String mToBCustomApp;
    public String mToBCustomAppActivity;
    public String mToBCustomAppPackage;
    public String mToBCustomHome;
    public String mToBCustomHomeActivity;
    public String mToBCustomHomePackage;
    private final Uri mPvrSetupWizardCompleteUri =
            Settings.Global.getUriFor(PVR_SETUP_WIZARD_COMPLETE);
    private final Uri mToBCustomAppUri = Settings.System.getUriFor(SETTINGS_TOB_CUSTOM_APP);
    private final Uri mToBCustomHomeUri = Settings.System.getUriFor(SETTINGS_TOB_CUSTOM_HOME);
    private final Uri mScreenshotToastShowingUri =
            Settings.Global.getUriFor(SETTINGS_SCREENSHOT_TOAST_SHOWING);
    private final Uri mDockShowingUri = Settings.Global.getUriFor(SETTINGS_DOCK_SHOWING);
    private final Uri mDisableCameraKeyUri =
            Settings.Global.getUriFor(SETTINGS_DISABLE_CAMERA_KEY);
    private boolean mPvrSetupWizardComplete = false;
    public boolean mIsToBDevice = false;
    public boolean mIsDockShowing = false;
    public boolean mIsScreenshotToastShowing = false;
    public boolean mDisableCaptureKeyAppShowing = false;

    public static SettingsObserverExt getInstance() {
        return mInstance;
    }

    public static void init(Context context, Handler handler) {
        if (mInstance == null) {
            mInstance = new SettingsObserverExt(context, handler);
        }
    }

    private SettingsObserverExt(Context context, Handler handler) {
        mContext = context;
        mHandler = handler;
    }

    public void onSystemReady() {
        mSettingsObserver = new SettingsObserver();
        ContentResolver resolver = mContext.getContentResolver();
        resolver.registerContentObserver(mPvrSetupWizardCompleteUri, false, mSettingsObserver);
        resolver.registerContentObserver(mToBCustomAppUri, false, mSettingsObserver, -1);
        resolver.registerContentObserver(mToBCustomHomeUri, false, mSettingsObserver, -1);
        resolver.registerContentObserver(mDockShowingUri, false, mSettingsObserver);
        resolver.registerContentObserver(mScreenshotToastShowingUri, false, mSettingsObserver);
        resolver.registerContentObserver(mDisableCameraKeyUri, false, mSettingsObserver);
        updateSettings();
    }

    private void updateSettings() {
        mIsToBDevice = isToBPhone() || Settings.Global.getInt(mContext.getContentResolver(),
                "ro.pxr.externalfunc.test", 0) != 0;
        updateSetupWizardComplete();
        updateToBCustomApp();
        updateToBCustomHome();
        updateDockStatus();
        updateScreenshotToastStatus();
        updateDisableCameraKeyStatus();
    }

    public boolean isSetupWizardComplete() {
        updateSetupWizardComplete();
        return mPvrSetupWizardComplete;
    }

    private void updateSetupWizardComplete() {
        if (!mPvrSetupWizardComplete) {
            ContentResolver resolver = mContext.getContentResolver();
            mPvrSetupWizardComplete =
                    Settings.Global.getInt(resolver, PVR_SETUP_WIZARD_COMPLETE, 0) != 0;
            Slog.i(TAG, "mPvrSetupWizardComplete : " + mPvrSetupWizardComplete);
        }
    }

    private void updateToBCustomApp() {
        if (!mIsToBDevice) {
            return;
        }
        ContentResolver resolver = mContext.getContentResolver();
        mToBCustomApp = Settings.System.getStringForUser(resolver, SETTINGS_TOB_CUSTOM_APP, -2);
        if (TextUtils.isEmpty(mToBCustomApp)) {
            mToBCustomApp = null;
            return;
        }
        int sep = mToBCustomApp.indexOf('/');
        if (sep < 0 || sep + 1 >= mToBCustomApp.length()) {
            mToBCustomAppPackage = mToBCustomApp;
        } else {
            mToBCustomAppPackage = mToBCustomApp.substring(0, sep);
            mToBCustomAppActivity = mToBCustomApp.substring(sep + 1);
            if (mToBCustomAppActivity.length() > 0 && mToBCustomAppActivity.charAt(0) == '.') {
                mToBCustomAppActivity = mToBCustomAppPackage + mToBCustomAppActivity;
            }
        }
        Slog.i(TAG, "mToBCustomApp : " + mToBCustomAppPackage + ", " + mToBCustomAppActivity);
    }

    private void updateToBCustomHome() {
        if (!mIsToBDevice) {
            return;
        }
        ContentResolver resolver = mContext.getContentResolver();
        mToBCustomHome = Settings.System.getStringForUser(resolver, SETTINGS_TOB_CUSTOM_HOME, -2);
        if (TextUtils.isEmpty(mToBCustomHome)) {
            mToBCustomHome = null;
            return;
        }
        int sep = mToBCustomHome.indexOf('/');
        if (sep < 0 || sep + 1 >= mToBCustomHome.length()) {
            mToBCustomHomePackage = mToBCustomHome;
        } else {
            mToBCustomHomePackage = mToBCustomHome.substring(0, sep);
            mToBCustomHomeActivity = mToBCustomHome.substring(sep + 1);
            if (mToBCustomHomeActivity.length() > 0 && mToBCustomHomeActivity.charAt(0) == '.') {
                mToBCustomHomeActivity = mToBCustomHomePackage + mToBCustomHomeActivity;
            }
        }
        Slog.i(TAG, "mToBCustomHome : " + mToBCustomHomePackage + ", "
                + mToBCustomHomeActivity);
    }

    private void updateDockStatus() {
        ContentResolver resolver = mContext.getContentResolver();
        mIsDockShowing = Settings.Global.getInt(resolver, SETTINGS_DOCK_SHOWING, 0) != 0;
    }

    private void updateScreenshotToastStatus() {
        ContentResolver resolver = mContext.getContentResolver();
        mIsScreenshotToastShowing =
                Settings.Global.getInt(resolver, SETTINGS_SCREENSHOT_TOAST_SHOWING, 0) != 0;
    }

    private void updateDisableCameraKeyStatus() {
        ContentResolver resolver = mContext.getContentResolver();
        mDisableCaptureKeyAppShowing =
                Settings.Global.getInt(resolver, SETTINGS_DISABLE_CAMERA_KEY, 0) != 0;
    }

    /** Base activity, top activity or base intent component of a task. */
    public static ComponentName getComponentName(ActivityManager.RunningTaskInfo taskInfo) {
        if (taskInfo == null) {
            return null;
        }
        if (taskInfo.baseActivity != null) {
            return taskInfo.baseActivity;
        }
        if (taskInfo.topActivity != null) {
            return taskInfo.topActivity;
        }
        if (taskInfo.baseIntent == null) {
            return null;
        }
        return taskInfo.baseIntent.getComponent();
    }

    /** A ToB (business) device has '1' as the first byte of the license flag file. */
    private static boolean isToBPhone() {
        File toBFile = new File(TO_B_FILE_PATH);
        if (!toBFile.exists()) {
            return false;
        }
        FileInputStream fis = null;
        try {
            fis = new FileInputStream(toBFile);
            int res = fis.read();
            if (res == '1') {
                return true;
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (fis != null) {
                try {
                    fis.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return false;
    }

    private final class SettingsObserver extends ContentObserver {
        SettingsObserver() {
            super(mHandler);
        }

        @Override
        public void onChange(boolean selfChange, Uri uri) {
            if (mPvrSetupWizardCompleteUri.equals(uri)) {
                updateSetupWizardComplete();
                return;
            }
            if (mToBCustomAppUri.equals(uri)) {
                updateToBCustomApp();
                return;
            }
            if (mToBCustomHomeUri.equals(uri)) {
                updateToBCustomHome();
                return;
            }
            if (mDockShowingUri.equals(uri)) {
                updateDockStatus();
            } else if (mScreenshotToastShowingUri.equals(uri)) {
                updateScreenshotToastStatus();
            } else if (mDisableCameraKeyUri.equals(uri)) {
                updateDisableCameraKeyStatus();
            }
        }
    }
}
