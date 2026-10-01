// Copyright 2026 Picomisu contributors
// SPDX-License-Identifier: Apache-2.0

package com.android.server.pm;

import android.content.pm.ApplicationInfo;
import android.content.pm.ApplicationInfoSmtBase;
import android.content.pm.IPackageManagerSmtEx;
import android.content.pm.PackageManager;
import android.content.pm.PackageParser;
import android.os.Binder;
import android.util.ArrayMap;
import android.util.ArraySet;
import android.util.Log;
import android.util.Slog;
import android.util.Xml;

import com.android.internal.util.XmlUtils;
import com.android.server.SysOptBridge;
import com.android.server.pm.dex.DexoptOptions;

import org.xmlpull.v1.XmlPullParser;

import smartisanos.os.PeroptWhiteListParser;

import java.io.File;
import java.io.FileInputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * Smartisan extension of the {@link PackageManagerService}, whose
 * {@link IPackageManagerSmtExBase} binder is returned by {@code IPackageManager.getISmtEx()}.
 * Reconstructed from the PICO OS 5.13.7 factory services; only the members reached by the
 * {@link IPackageManagerSmtEx} methods and by the PackageManagerService hooks
 * (refuseOverrideSdkClazz) are present.
 *
 * @hide
 */
public class PackageManagerServiceSmtBase {
    static final String TAG = "PackageManager";
    // Packages listed in /data/system/overrideSdk.xml (closeOverrideProc), loaded once.
    public static ArraySet<String> mOverrideClazzIgnoreProcs = null;

    private IPackageManagerSmtEx mIPackageManagerSmtEx = this.new IPackageManagerSmtExBase();
    protected PackageManagerService mPmService;
    protected PackageManagerServiceMonitorEx mPmServiceMonitorEx;

    protected PeroptWhiteListParser mPeroptWhiteListParser;

    protected PackageManagerServiceSmtBase(PackageManagerService pmService,
            PackageManagerServiceMonitorEx pmServiceMonitorEx) {
        mPmService = pmService;
        mPmServiceMonitorEx = pmServiceMonitorEx;
    }

    protected void SmartisanOSInit() {
        mPeroptWhiteListParser = PeroptWhiteListParser.getInstance();
    }

    protected void updateSmartisanFlagValue(ApplicationInfo info, PackageParser.Package pkg) {
        if (mPeroptWhiteListParser != null) {
            mPeroptWhiteListParser.updateSmartisanFlagValue(info, pkg);
        } else {
            Slog.e(TAG, "mPeroptWhiteListParser is null");
        }
    }

    protected void updateSmartisanFlagValue(ApplicationInfoSmtBase infoSmtEx, String packageName) {
        if (mPeroptWhiteListParser != null) {
            mPeroptWhiteListParser.updateSmartisanFlagValue(infoSmtEx, packageName);
        } else {
            Slog.e(TAG, "mPeroptWhiteListParser is null");
        }
    }

    public void updatePackagesKilledTime(String packageName, int type, long time) {
        synchronized (mPmService.mPackages) {
            PackageParser.Package pkg = mPmService.mPackages.get(packageName);
            if (pkg != null && pkg.applicationInfo != null) {
                ApplicationInfo info = pkg.applicationInfo;
                info.getSmtEx().beKilledTime = time;
                info.getSmtEx().beKilledType = type;
                if (ApplicationInfoSmtBase.TYPE_BG_HIGH_CPU == type) {
                    info.getSmtEx().killedTimes++;
                } else {
                    info.getSmtEx().killedTimes = 0;
                }
            }
        }
    }

    public ArrayMap<String, PackageParser.Package> getPackageMap() {
        return mPmService.mPackages;
    }

    public void updateAppTypeInfo(String packageName, int flag) {
        synchronized (mPmService.mPackages) {
            PackageParser.Package pkg = mPmService.mPackages.get(packageName);
            if (pkg != null && pkg.applicationInfo != null) {
                ApplicationInfo info = pkg.applicationInfo;
                int oldFlag = info.getSmtEx().appTypeFlag;
                if (flag == 1 && (info.flags & ApplicationInfo.FLAG_SYSTEM) != 0) {
                    info.getSmtEx().appTypeFlag = 2;
                } else if (oldFlag != flag) {
                    info.getSmtEx().appTypeFlag = flag;
                }
            }
        }
    }

    public void parseAppRefreshRate(String jsonStr) {
        SysOptBridge.getFactory().getSmartService().parseAppRefreshRate(jsonStr);
    }

    public boolean performDexOptModeSmt(String packageName, boolean checkProfiles,
            String targetCompilerFilter, boolean force, boolean bootComplete, String splitName,
            boolean immediately) {
        int flags = (checkProfiles ? DexoptOptions.DEXOPT_CHECK_FOR_PROFILES_UPDATES : 0)
                | (force ? DexoptOptions.DEXOPT_FORCE : 0)
                | (bootComplete ? DexoptOptions.DEXOPT_BOOT_COMPLETE : 0);
        DexoptOptions dexOptions = new DexoptOptions(packageName,
                PackageManagerService.REASON_UNKNOWN, targetCompilerFilter, splitName, flags);
        return mPmService.performDexOpt(new DexoptOptions(packageName,
                PackageManagerService.REASON_UNKNOWN, targetCompilerFilter, splitName, flags));
    }

    public IPackageManagerSmtEx getISmtEx() {
        return mIPackageManagerSmtEx;
    }

    /** Binder of the Smartisan package manager extension. */
    public class IPackageManagerSmtExBase extends IPackageManagerSmtEx.Stub {
        protected IPackageManagerSmtExBase() {
        }

        @Override
        public boolean performDexOptModeSmt(String packageName, boolean checkProfiles,
                String targetCompilerFilter, boolean force, boolean bootComplete,
                String splitName, boolean immediately) {
            if (!checkPermissionByUid()) {
                Log.d(TAG, "performDexOptModeSmt no Permission");
                return false;
            }
            return PackageManagerServiceSmtBase.this.performDexOptModeSmt(packageName,
                    checkProfiles, targetCompilerFilter, force, bootComplete, splitName,
                    immediately);
        }

        @Override
        public boolean isTaskPersist(String packagename, int userId) {
            if (!checkPermissionByUid()) {
                Log.d(TAG, "isTaskPersist no Permission");
                return false;
            }
            return mPmServiceMonitorEx.isTaskPersist(packagename, userId);
        }

        @Override
        public void updateAppTypeInfo(String packageName, int flag) {
            if (!checkPermissionByUid()) {
                Log.d(TAG, "updateAppTypeInfo no Permission");
                return;
            }
            PackageManagerServiceSmtBase.this.updateAppTypeInfo(packageName, flag);
        }

        @Override
        public void parseAppRefreshRate(String jsonStr) {
            if (!checkPermissionByUid()) {
                Log.d(TAG, "parseAppRefreshRate no Permission");
                return;
            }
            PackageManagerServiceSmtBase.this.parseAppRefreshRate(jsonStr);
        }

        @Override
        public void clearOverrideFlag(String packageName) {
            if (!checkPermissionByUid()) {
                Log.d(TAG, "clearOverrideFlag no Permission");
                return;
            }
            PackageManagerServiceSmtBase.this.clearOverrideFlag(packageName);
        }

        @Override
        public void updateAppInfo(String packageName, String appInfoJsonConfig, long appLastTime,
                boolean removeUnityChoreographerVsync, boolean permissionFlag, boolean isDexApp,
                boolean downloadProfileFlag) {
            if (!checkPermissionByUid()) {
                // Factory log message, copied from clearOverrideFlag.
                Log.d(TAG, "clearOverrideFlag no Permission");
                return;
            }
            PackageManagerServiceSmtBase.this.updateAppInfo(packageName, appInfoJsonConfig,
                    appLastTime, removeUnityChoreographerVsync, permissionFlag, isDexApp,
                    downloadProfileFlag);
        }
    }

    public void updatePackagesInWhiteList(int type) {
        synchronized (mPmService.mPackages) {
            for (Map.Entry<String, PackageParser.Package> entry
                    : mPmService.mPackages.entrySet()) {
                String pkgName = entry.getKey();
                PackageParser.Package pkg = entry.getValue();
                if (pkg != null && pkg.applicationInfo != null) {
                    PeroptWhiteListParser.updateFlagValueByType(pkg.applicationInfo, type);
                }
            }
        }
    }

    public void clearOverrideFlag(String packageName) {
        synchronized (mPmService.mPackages) {
            mPmService.mPackages.get(packageName).applicationInfo.getSmtEx().mOverrideClassSDK = 0;
            PackageSetting ps = mPmService.mSettings.mPackages.get(packageName);
            ps.pkg.applicationInfo.getSmtEx().mOverrideClassSDK = 0;
            mPmService.mSettings.mPackages.put(packageName, ps);
        }
    }

    /**
     * Whether the class SDK override is refused for {@code proc}: listed as
     * {@code <package closeOverrideProc="...">} in /data/system/overrideSdk.xml, which is read
     * the first time it exists (factory).
     */
    public boolean refuseOverrideSdkClazz(String proc) {
        try {
            File file = new File("/data/system/overrideSdk.xml");
            if (mOverrideClazzIgnoreProcs == null && file.exists()) {
                mOverrideClazzIgnoreProcs = new ArraySet<>(4);
                FileInputStream str = new FileInputStream("/data/system/overrideSdk.xml");
                XmlPullParser parser = Xml.newPullParser();
                parser.setInput(str, StandardCharsets.UTF_8.name());
                int type;
                while ((type = parser.next()) != XmlPullParser.START_TAG
                        && type != XmlPullParser.END_DOCUMENT) {
                }
                if (type != XmlPullParser.START_TAG) {
                    return false;
                }
                int outerDepth = parser.getDepth();
                while ((type = parser.next()) != XmlPullParser.END_DOCUMENT
                        && (type != XmlPullParser.END_TAG || parser.getDepth() > outerDepth)) {
                    if (type == XmlPullParser.END_TAG || type == XmlPullParser.TEXT) {
                        continue;
                    }
                    String tagName = parser.getName();
                    if (tagName.equals("package")) {
                        String name = parser.getAttributeValue(null, "closeOverrideProc");
                        mOverrideClazzIgnoreProcs.add(name);
                    } else {
                        XmlUtils.skipCurrentTag(parser);
                    }
                }
                str.close();
            }
        } catch (Exception e) {
            Slog.e(TAG, "refuseOverrideSdkClazz failed");
        }
        return mOverrideClazzIgnoreProcs != null && mOverrideClazzIgnoreProcs.contains(proc);
    }

    public void updateOverrideSdkClazzClose() {
        synchronized (mPmService.mPackages) {
            for (PackageParser.Package p : mPmService.mPackages.values()) {
                p.applicationInfo.getSmtEx().mOverrideClassSDK = 0;
            }
        }
    }

    public void updateAppInfo(String packageName, String appInfoJsonConfig, long appLastTime,
            boolean removeUnityChoreographerVsync, boolean permissionFlag, boolean isDexApp,
            boolean downloadProfileFlag) {
        synchronized (mPmService.mPackages) {
            PackageParser.Package pkg = mPmService.mPackages.get(packageName);
            if (pkg != null && pkg.applicationInfo != null) {
                pkg.applicationInfo.appInfoJsonConfig = appInfoJsonConfig;
                pkg.applicationInfo.appLastTime = appLastTime;
                pkg.applicationInfo.getSmtEx().removeUnityChoreographerVsync =
                        removeUnityChoreographerVsync;
                pkg.applicationInfo.getSmtEx().permissionFlag = permissionFlag;
                if (isDexApp) {
                    pkg.applicationInfo.getSmtEx().isDexApp = isDexApp;
                }
                if (downloadProfileFlag) {
                    pkg.applicationInfo.getSmtEx().downloadProfileFlag = downloadProfileFlag;
                }
            }
        }
    }

    private boolean checkPermissionByUid() {
        int callerUid = Binder.getCallingUid();
        PackageManager pkgManager = mPmService.mContext.getPackageManager();
        ApplicationInfo myAppinfo = new ApplicationInfo();
        try {
            myAppinfo = pkgManager.getApplicationInfo(mPmService.mContext.getPackageName(), 0);
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        int myUid = myAppinfo.uid;
        if (pkgManager.checkSignatures(myUid, callerUid) == PackageManager.SIGNATURE_MATCH) {
            return true;
        }
        return false;
    }
}
