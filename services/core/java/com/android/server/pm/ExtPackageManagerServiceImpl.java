// Copyright 2026 Picomisu contributors
// SPDX-License-Identifier: Apache-2.0
package com.android.server.pm;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.content.pm.ExtPackageParserUtils;
import android.content.pm.PackageManager;
import android.content.pm.PackageParser;
import android.content.pm.Signature;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Binder;
import android.os.Bundle;
import android.os.SystemProperties;
import android.pico.utils.PicoSystemConfig;
import android.provider.PicoSettings;
import android.util.Log;
import android.util.Slog;

import com.android.internal.app.PicoWebViewActivity;

import java.io.File;
import java.io.FileInputStream;
import java.lang.reflect.Field;
import java.security.PublicKey;
import java.security.cert.CertificateException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

/**
 * PICO package manager extension (factory PICO OS 5.13.7
 * com.android.server.pm.ExtPackageManagerServiceImpl).
 *
 * <ul>
 * <li>App signature verity: packages listed in /system/etc/pvr/appverity_default.prop must be
 * signed with one of the keys listed for them, packages listed in /data/misc/pxr/appverity.prop
 * (or /system/etc/pvr/appverity.prop) with the PICO store key; otherwise the install fails with
 * "package sign verity failed". Developer mode (Settings.Global pvr_settings_showDeveloper)
 * skips both checks. The lists are reloaded on android.intent.appverity.config_change.</li>
 * <li>The HOME category is removed from the activities of non-system packages (VR activities of
 * ToB devices, ro.pxr.externalfunc=1, keep it).</li>
 * <li>{@link PicoWebViewActivity} is resolved for every installed package.</li>
 * <li>App DE data creation that failed before installd was reachable is retried once installd
 * connects.</li>
 * <li>Install start/result logging, the PICO white/black list observer and the 2D virtual
 * display app config reload after a SystemExt cloud config sync.</li>
 * </ul>
 */
public class ExtPackageManagerServiceImpl implements IExtPackageManagerService {
    private static final String APP_CONFIG_PATH_FROM_CLOUD = "app_cloud_config.txt";
    private static final String KEY_CALL_TO_TASK_RECEIVER_PATH = "Path";
    private static final String SYS_EXT_SYNC_DATA_FINISH =
            "com.picovr.systemext.ACTION_SYNC_DATA_FINISH";
    static final String TAG = "PackageManager";
    static final boolean TOB_DEVICES = SystemProperties.getInt("ro.pxr.externalfunc", -1) == 1;
    private static final String USER_VERITY_CONFIG_CHANGE =
            "android.intent.appverity.config_change";
    private static final String dataVerityKeyFile = "/data/misc/pxr/appverity.prop";
    private static final String mAuthPublicKey2 = "995f8c9530ebf7392828b461f3ceb364afd7364ff86c83029f72fc4e27e4f1e0bece6bae9eed1724394a746930f27425664d679f4e2f915a6da2f1eb15b2c69fe8ee85998f6243f5671f042e547adb0494e17cbd05247329e6322b59775e366f9e2e6083d6f63917120849699c28d7d0b0c77d67651b9fbb9db2d38660c2dccccbb7ddd30426cdc99eb59623a34003f620f4d5c5af21ab09cf2e06f6c3441514fa2b7b4e861f66da561f13a9553cf81b52fe37c14f45c8e3583e874fedde5ffba84384a4d8e1cff522f950a037f8db991765bc65ca4770330d88920daedcd1a96d4c3be09945c82d3744a29ab166918916fdbb744d2efbf20f0286e1ddd7e837";
    private static final String mAuthPublicKeytest = "test1";
    private static final String systemVerityKeyDefinedFile =
            "/system/etc/pvr/appverity_default.prop";
    private static final String systemVerityKeyFile = "/system/etc/pvr/appverity.prop";

    private PackageManagerService mBase;
    private volatile boolean mCreateAppDEDataFailed = false;
    private Properties pxrVerityKeyDefined = new Properties();
    private Properties pxrVerityKey = new Properties();
    private boolean debug_verity = SystemProperties.getInt("persist.pxr.debug.verity", 0) == 1;

    BroadcastReceiver mPxrVerityActionReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String intent_action = intent.getAction();
            Log.d(TAG, "mPxrVerityActionReceiver get action " + intent_action);
            if (USER_VERITY_CONFIG_CHANGE.equals(intent_action)) {
                initPxrVerityPackage();
            }
        }
    };

    public ExtPackageManagerServiceImpl(PackageManagerService base) {
        mBase = base;
    }

    @Override
    public void init() {
        registerIInstalldConnectListener(mBase.mInstaller);
        initPxrVerityDefaultPackage();
        initPxrVerityPackage();
    }

    private boolean initPxrVerityDefaultPackage() {
        File pxrVerityKeyFile = null;
        FileInputStream fis = null;
        try {
            pxrVerityKeyFile = new File(systemVerityKeyDefinedFile);
            if (!pxrVerityKeyFile.exists()) {
                Log.w(TAG, "systemVerityKeyDefinedFile exists");
                return false;
            }
            Log.w(TAG, "systemVerityKeyDefinedFile exists");
            fis = new FileInputStream(pxrVerityKeyFile);
            pxrVerityKeyDefined.load(fis);
            Log.w(TAG, "pxrVerityKeyDefined load ok");
            return true;
        } catch (Exception e) {
            pxrVerityKeyDefined = new Properties();
            Log.w(TAG, "pxrVerityKeyDefined init error:" + e.toString());
            return false;
        } finally {
            if (fis != null) {
                try {
                    fis.close();
                } catch (Exception e) {
                    Log.w(TAG, "initPxrVerityDefaultPackage pxrVerityKeyFile close error:"
                            + e.toString());
                }
            }
        }
    }

    private boolean initPxrVerityPackage() {
        File pxrVerityKeyFile = null;
        FileInputStream fis = null;
        try {
            pxrVerityKeyFile = new File(dataVerityKeyFile);
            if (pxrVerityKeyFile.exists()) {
                Log.w(TAG, "dataVerityKeyFile exists");
            } else {
                pxrVerityKeyFile = new File(systemVerityKeyFile);
                if (!pxrVerityKeyFile.exists()) {
                    Log.w(TAG, "noVerityKeyFile exists");
                    return false;
                }
                Log.w(TAG, "systemVerityKeyFile exists");
            }
            fis = new FileInputStream(pxrVerityKeyFile);
            pxrVerityKey.load(fis);
            Log.w(TAG, "pxrVerityKey load ok");
            return true;
        } catch (Exception e) {
            pxrVerityKey = new Properties();
            Log.w(TAG, "pxrVerityKey init error:" + e.toString());
            return false;
        } finally {
            if (fis != null) {
                try {
                    fis.close();
                } catch (Exception e) {
                    Log.w(TAG, "initPxrVerityPackage pxrVerityKeyFile close error:"
                            + e.toString());
                }
            }
        }
    }

    private String getCallingPackagesForUidExt(int uid) {
        String install_from = null;
        try {
            synchronized (mBase.mPackages) {
                String[] packageNames = mBase.getPackagesForUid(uid);
                PackageParser.Package pkg = null;
                int N = packageNames == null ? 0 : packageNames.length;
                for (int i = 0; pkg == null && i < N; i++) {
                    pkg = mBase.mPackages.get(packageNames[i]);
                    install_from = install_from + packageNames[i];
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (install_from == null) {
            install_from = "unknown";
        }
        return install_from + ",uid:" + uid;
    }

    @Override
    public void notifyPackageInstallStart(List<PackageManagerService.InstallRequest> requests) {
        try {
            for (PackageManagerService.InstallRequest request : requests) {
                String install_from = request.args.installerPackageName;
                if ((request.args.installFlags & PackageManager.INSTALL_FROM_ADB) != 0) {
                    install_from = "adb";
                }
                String content_request = "install_app:" + request.args.getCodePath()
                        + ";install_from:" + install_from;
                Slog.w(TAG, "we notifyPackageInstallResult start_install_app:"
                        + content_request);
            }
        } catch (Exception e) {
        }
    }

    @Override
    public void notifyPackageInstallEnd(List<PackageManagerService.InstallRequest> requests) {
        try {
            for (PackageManagerService.InstallRequest request : requests) {
                String install_from = request.installResult.installerPackageName;
                if ((request.args.installFlags & PackageManager.INSTALL_FROM_ADB) != 0) {
                    install_from = "adb";
                }
                String content_result = "packagename:" + request.installResult.name
                        + ";install_from:" + install_from
                        + ";result:" + request.installResult.returnCode
                        + ";fail_reason:" + request.installResult.returnMsg;
                Slog.w(TAG, "we notifyPackageInstallResult install_app_result:"
                        + content_result);
            }
        } catch (Exception e) {
        }
    }

    private boolean verityPxrPackageInternal(PackageParser.Package pkg) {
        String devMode = android.provider.Settings.Global.getString(
                mBase.mContext.getContentResolver(), "pvr_settings_showDeveloper");
        boolean isDevMode = devMode != null && "true".equals(devMode);
        int is_developer_mode = 0;
        int is_within_list = 0;
        int is_same_signature = -1;
        int is_new_applist = -1;
        if (isDevMode) {
            is_developer_mode = 1;
        }
        if (pxrVerityKeyDefined.containsKey(pkg.packageName)) {
            is_within_list = 1;
            is_new_applist = 0;
        }
        if (pxrVerityKey.containsKey(pkg.packageName)) {
            is_within_list = 1;
            is_new_applist = 1;
        }
        String content = "known";
        String appPublicKey = null;
        String keyString = null;
        if (debug_verity) {
            Slog.w(TAG, "verityPxrPackage check pkg = " + pkg.packageName
                    + " inDevMode " + isDevMode);
        }
        if (pxrVerityKeyDefined != null && pxrVerityKeyDefined.containsKey(pkg.packageName)
                && !isDevMode) {
            Slog.w(TAG, "we get default list to check package:" + pkg.packageName);
            try {
                Signature verifierSig = pkg.mSigningDetails.signatures[0];
                PublicKey publicKey = verifierSig.getPublicKey();
                appPublicKey = publicKey.toString();
            } catch (CertificateException e) {
                Slog.e(TAG, "verifierSig error = " + e.toString());
                content = "packagename:" + pkg.packageName
                        + ";install_from:" + getCallingPackagesForUidExt(Binder.getCallingUid())
                        + ";is_developer_mode:" + is_developer_mode
                        + ";is_within_list:" + is_within_list
                        + ";is_same_signature:" + is_same_signature
                        + ";is_new_applist:" + is_new_applist;
                Slog.w(TAG, "we notifyPackageInstallResult examine_install_app:" + content);
                return false;
            }
            Slog.w(TAG, "debug_verity default appPublicKey = " + appPublicKey);
            keyString = IExtPackageManagerService.getPublicKeyStr(appPublicKey);
            if (debug_verity) {
                Slog.w(TAG, "debug_verity keyString = " + keyString);
                Slog.w(TAG, "debug_verity pxrVerityKeyDefined.getProperty(pkg.packageName) = "
                        + pxrVerityKeyDefined.getProperty(pkg.packageName));
            }
            if (keyString != null && pxrVerityKeyDefined.getProperty(pkg.packageName) != null
                    && !pxrVerityKeyDefined.getProperty(pkg.packageName).contains(keyString)) {
                is_same_signature = 0;
                content = "packagename:" + pkg.packageName
                        + ";install_from:" + getCallingPackagesForUidExt(Binder.getCallingUid())
                        + ";is_developer_mode:" + is_developer_mode
                        + ";is_within_list:" + is_within_list
                        + ";is_same_signature:" + is_same_signature
                        + ";is_new_applist:" + is_new_applist;
                Slog.w(TAG, "we notifyPackageInstallResult examine_install_app:" + content);
                return false;
            }
            is_same_signature = 1;
        }
        if (pxrVerityKey != null && pxrVerityKey.containsKey(pkg.packageName) && !isDevMode) {
            Slog.w(TAG, "we get store list to check package:" + pkg.packageName);
            try {
                Signature verifierSig = pkg.mSigningDetails.signatures[0];
                PublicKey publicKey = verifierSig.getPublicKey();
                appPublicKey = publicKey.toString();
            } catch (CertificateException e) {
                Slog.e(TAG, "verifierSig error = " + e.toString());
                content = "packagename:" + pkg.packageName
                        + ";install_from:" + getCallingPackagesForUidExt(Binder.getCallingUid())
                        + ";is_developer_mode:" + is_developer_mode
                        + ";is_within_list:" + is_within_list
                        + ";is_same_signature:" + is_same_signature
                        + ";is_new_applist:" + is_new_applist;
                Slog.w(TAG, "we notifyPackageInstallResult examine_install_app:" + content);
                return false;
            }
            Slog.w(TAG, "debug_verity appPublicKey = " + appPublicKey);
            keyString = IExtPackageManagerService.getPublicKeyStr(appPublicKey);
            if (keyString != null && keyString.compareTo(mAuthPublicKey1) != 0
                    && keyString.compareTo(mAuthPublicKey2) != 0) {
                is_same_signature = 0;
                content = "packagename:" + pkg.packageName
                        + ";install_from:" + getCallingPackagesForUidExt(Binder.getCallingUid())
                        + ";is_developer_mode:" + is_developer_mode
                        + ";is_within_list:" + is_within_list
                        + ";is_same_signature:" + is_same_signature
                        + ";is_new_applist:" + is_new_applist;
                Slog.w(TAG, "we notifyPackageInstallResult examine_install_app:" + content);
                return false;
            }
            is_same_signature = 1;
        }
        Slog.w(TAG, "we do nothing for common package:" + pkg.packageName);
        content = "packagename:" + pkg.packageName
                + ";install_from:" + getCallingPackagesForUidExt(Binder.getCallingUid())
                + ";is_developer_mode:" + is_developer_mode
                + ";is_within_list:" + is_within_list
                + ";is_same_signature:" + is_same_signature
                + ";is_new_applist:" + is_new_applist;
        Slog.w(TAG, "we notifyPackageInstallResult examine_install_app:" + content);
        return true;
    }

    @Override
    public void onSystemReady() {
        ContentObserver picoCo = new ContentObserver(mBase.mHandler) {
            @Override
            public void onChange(boolean selfChange, Uri uri) {
                Slog.v(TAG, "Pico content observer....Uri:" + uri.toString());
                if (uri.equals(PicoSettings.WhiteList.CONTENT_URI)) {
                    PicoSystemConfig.getInstance().updatePicoConfig(1);
                } else if (uri.equals(PicoSettings.BlackList.CONTENT_URI)) {
                    PicoSystemConfig.getInstance().updatePicoConfig(2);
                } else {
                    Slog.v(TAG, "Unknown uri " + uri + " for pico content observer");
                }
            }
        };
        mBase.mContext.getContentResolver().registerContentObserver(
                PicoSettings.WhiteList.CONTENT_URI, false, picoCo, 0);
        mBase.mContext.getContentResolver().registerContentObserver(
                PicoSettings.BlackList.CONTENT_URI, false, picoCo, 0);
        picoCo.onChange(true);
        IntentFilter filter = new IntentFilter();
        filter.addAction(USER_VERITY_CONFIG_CHANGE);
        mBase.mContext.registerReceiver(mPxrVerityActionReceiver, filter);
        mBase.mContext.registerReceiver(new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String path = intent.getStringExtra(KEY_CALL_TO_TASK_RECEIVER_PATH);
                Slog.i(TAG, "onReceive :" + intent + ", path :" + path);
                if (path.endsWith(APP_CONFIG_PATH_FROM_CLOUD)) {
                    applyVirtualDisplayConfigToApp(true);
                }
            }
        }, new IntentFilter(SYS_EXT_SYNC_DATA_FINISH));
    }

    private void applyVirtualDisplayConfigToApp(boolean force) {
        ExtPackageParserUtils.loadVirtualDisplayConfigsFromFiles(force);
        synchronized (mBase.mPackages) {
            for (PackageParser.Package p : mBase.mPackages.values()) {
                ExtPackageParserUtils.applyVirtualDisplayConfigToApp(p.applicationInfo);
            }
        }
    }

    @Override
    public boolean isGrantPermission(boolean oldGrantPermissions) {
        return oldGrantPermissions;
    }

    @Override
    public boolean allowPersistentUpdate() {
        return false;
    }

    @Override
    public void verityPxrPackage(PackageParser.Package pkg,
            PackageManagerService.PackageInstalledInfo outRes)
            throws PackageManagerService.PrepareFailure {
        try {
            if (!verityPxrPackageInternal(pkg)) {
                outRes.setError(PackageManager.INSTALL_FAILED_INVALID_APK,
                        "package sign verity failed");
                // The factory fails with -116 (INSTALL_FAILED_INSTANT_APP_INVALID).
                throw new PackageManagerService.PrepareFailure(
                        PackageManager.INSTALL_FAILED_INSTANT_APP_INVALID,
                        "package sign verity failed");
            }
        } catch (IllegalArgumentException e) {
            Slog.e(TAG, "verityPxrPackage", e);
        }
    }

    private IInstalldConnectSuccessListener mIInstalldConnectListener =
            new IInstalldConnectSuccessListener() {
                @Override
                public void connectSuccess() {
                    tryAgainCreateAppDEData();
                }
            };

    @Override
    public void registerIInstalldConnectListener(Installer installer) {
        if (installer != null) {
            installer.getExt().registerIInstalldConnectSuccessListener(mIInstalldConnectListener);
        }
    }

    @Override
    public void setCreateAppDEDataStateIfNeed(int flags) {
        if ((flags & Installer.FLAG_STORAGE_DE) != 0) {
            Slog.e(TAG, "set create app de data failed!");
            mCreateAppDEDataFailed = true;
        }
    }

    private void tryAgainCreateAppDEData() {
        if (!mCreateAppDEDataFailed) {
            return;
        }
        mCreateAppDEDataFailed = false;
        try {
            Log.w(TAG, "tryAgainCreateAppDEData");
            mBase.reconcileAppsData(0, Installer.FLAG_STORAGE_DE, true);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void removeHomeCategory(PackageParser.Package parsed) {
        if (parsed.isSystem()) {
            return;
        }
        boolean isVrApp = parsed.applicationInfo.getExt().isVrApp();
        for (int i = 0; i < parsed.activities.size(); i++) {
            PackageParser.Activity activity = parsed.activities.get(i);
            ArrayList<PackageParser.ActivityIntentInfo> activityIntentInfos = activity.intents;
            if (activityIntentInfos == null) {
                continue;
            }
            for (int j = 0; j < activityIntentInfos.size(); j++) {
                PackageParser.ActivityIntentInfo activityIntentInfo = activityIntentInfos.get(j);
                if (!activityIntentInfo.hasCategory(Intent.CATEGORY_HOME)) {
                    continue;
                }
                if (activity.info.getExt().isVrActivity() || isVrApp) {
                    if (!TOB_DEVICES) {
                        removeCategoryHomeForIntentInfo(activityIntentInfo);
                    }
                } else {
                    removeCategoryHomeForIntentInfo(activityIntentInfo);
                }
            }
        }
    }

    private void removeCategoryHomeForIntentInfo(PackageParser.ActivityIntentInfo info) {
        try {
            Field f = info.getClass().getSuperclass().getSuperclass()
                    .getDeclaredField("mCategories");
            f.setAccessible(true);
            ArrayList<String> categories = (ArrayList<String>) f.get(info);
            boolean res = categories.remove(Intent.CATEGORY_HOME);
            Slog.w(TAG, "remove CATEGORY_HOME for package = " + info.activity.info.packageName
                    + ", res=" + res);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private ActivityInfo mWebViewActivity = null;

    @Override
    public ActivityInfo getActivityInfoInternal(ComponentName component, int flags, int userId) {
        if ("com.android.internal.app.PicoWebViewActivity".equals(component.getClassName())) {
            PackageSetting ps = mBase.mSettings.mPackages.get(component.getPackageName());
            if (ps == null) {
                Slog.w(TAG, "getActivityInfoInternal error for component: " + component);
                return null;
            }
            if (mWebViewActivity == null) {
                mWebViewActivity = new ActivityInfo();
                mWebViewActivity.name = PicoWebViewActivity.class.getName();
                mWebViewActivity.launchMode = ActivityInfo.LAUNCH_SINGLE_INSTANCE;
                mWebViewActivity.flags = ActivityInfo.FLAG_EXCLUDE_FROM_RECENTS
                        | ActivityInfo.FLAG_MULTIPROCESS;
                mWebViewActivity.theme = android.R.style.Theme_Material;
                mWebViewActivity.exported = true;
                mWebViewActivity.metaData = new Bundle();
                mWebViewActivity.metaData.putString("com.picovr.type", "vr");
                mWebViewActivity.metaData.putString("pvr.app.type", "vr");
                mWebViewActivity.getExt().setVrActivity(1);
            }
            mWebViewActivity.applicationInfo = PackageParser.generateApplicationInfo(ps.pkg,
                    flags, ps.readUserState(userId), userId);
            mWebViewActivity.packageName = component.getPackageName();
            mWebViewActivity.processName = mWebViewActivity.applicationInfo.processName;
            Slog.i(TAG, "getActivityInfoInternal for component: " + component
                    + ", replace Info: " + mWebViewActivity.applicationInfo);
            return mWebViewActivity;
        }
        return null;
    }
}
