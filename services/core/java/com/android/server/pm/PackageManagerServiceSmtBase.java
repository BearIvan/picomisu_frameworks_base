// Copyright 2026 Picomisu contributors
// SPDX-License-Identifier: Apache-2.0

package com.android.server.pm;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ApplicationInfo;
import android.content.pm.ApplicationInfoSmtBase;
import android.content.pm.IPackageManagerSmtEx;
import android.content.pm.PackageInfo;
import android.content.pm.PackageInstaller;
import android.content.pm.PackageManager;
import android.content.pm.PackageParser;
import android.os.BatteryManagerInternal;
import android.os.Binder;
import android.os.CommonWorkerThread;
import android.os.Message;
import android.os.SystemProperties;
import android.util.ArrayMap;
import android.util.ArraySet;
import android.util.Log;
import android.util.Slog;
import android.util.Xml;

import com.android.internal.util.XmlUtils;
import com.android.server.ISmartAnaly;
import com.android.server.LocalServices;
import com.android.server.SysOptBridge;
import com.android.server.pm.dex.DexoptOptions;
import com.android.server.wm.BlurStartingWindowUtils;

import org.xmlpull.v1.XmlPullParser;

import smartisanos.os.PeroptWhiteListParser;
import smartisanos.util.FeatLog;

import java.io.File;
import java.io.FileInputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * Smartisan extension of the {@link PackageManagerService}, whose
 * {@link IPackageManagerSmtExBase} binder is returned by {@code IPackageManager.getISmtEx()}.
 * Reconstructed from the PICO OS 5.13.7 factory services. As on the factory, nothing calls the
 * pre-install check (checkPreinstallApp), the idle dexopt handler (handleMessageSmt) or the
 * blurred starting window file deletes.
 *
 * @hide
 */
public class PackageManagerServiceSmtBase {
    protected static final String ACTION_PENDING_INTENT_PREINSTALL = "preinstall_app";
    protected static final String ACTION_PREINSTALL_APP_DONE = "smt.action.preinstall_done";
    protected static final boolean DEBUGGABLE = SystemProperties.getInt("ro.debuggable", 0) == 1
            || "US".equals(SystemProperties.get("ro.build.region", "CN"));
    protected static final boolean DEBUG_ADB_INSTALL = true;
    protected static final boolean DEBUG_PACKAGE_LOCK = false;
    private static final String SAMRTISAN_PREINSTALL_CHECKED_PROPERTY =
            "persist.service.smartisan.preinstallchecked";
    static final String TAG = "PackageManager";
    protected static final String VENDOR_PREINSTALL_APP = "/oem/thirdapp";
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

    public void checkPreinstallApp() {
        if (mPmService.mIsUpgrade) {
            CommonWorkerThread.getHandler().post(this::doCheckOtaUpgradeApp);
            return;
        }
        String propValue = SystemProperties.get(SAMRTISAN_PREINSTALL_CHECKED_PROPERTY, "not_set");
        Slog.d(TAG, "preinstallchecked property: " + propValue);
        if ("y".equals(propValue)) {
            SystemProperties.set(SAMRTISAN_PREINSTALL_CHECKED_PROPERTY, Integer.toString(0));
            Slog.d(TAG, "reset preinstallchecked property to 0, compatible with old logic");
            return;
        }
        if ("0".equals(propValue)) {
            Slog.d(TAG, "preinstallchecked return directly");
            return;
        }
        FilenameFilter filter = (dir, name) -> name.startsWith("vmdl") && name.endsWith(".tmp");
        File scanDir = new File("/data/app/");
        for (File file : scanDir.listFiles(filter)) {
            Slog.d(TAG, "preinstallchecked delete " + file.toString());
            deleteDirectory(file);
        }
        CommonWorkerThread.getHandler().post(this::doCheckPreinstallApp);
    }

    public void parseAppRefreshRate(String jsonStr) {
        SysOptBridge.getFactory().getSmartService().parseAppRefreshRate(jsonStr);
    }

    private void doCheckPreinstallApp() {
        try {
            Slog.d(TAG, "doCheckPreinstallApp");
            File file = new File(VENDOR_PREINSTALL_APP);
            File[] children = file.listFiles();
            PackageParser parser = new PackageParser();
            List<File> apksToInstall = new LinkedList<>();
            for (File child : children) {
                PackageParser.Package parsed = parser.parsePackage(child, 0);
                if (!mPmService.isPackageAvailable(parsed.packageName,
                        mPmService.mContext.getUserId())) {
                    apksToInstall.add(child);
                }
            }
            if (!apksToInstall.isEmpty()) {
                doInstall(apksToInstall);
            }
        } catch (Throwable thr) {
            Slog.e(TAG, "doCheckPreinstallApp error!", thr);
        }
    }

    private void doCheckOtaUpgradeApp() {
        try {
            Slog.d(TAG, "doCheckOtaUpgradeApp");
            File file = new File(VENDOR_PREINSTALL_APP);
            File[] children = file.listFiles();
            PackageParser parser = new PackageParser();
            List<File> apksToInstall = new LinkedList<>();
            for (File child : children) {
                PackageParser.Package parsed = parser.parsePackage(child, 0);
                PackageInfo packageInfo = mPmService.getPackageInfo(parsed.packageName, 0,
                        mPmService.mContext.getUserId());
                if (packageInfo != null
                        && packageInfo.getLongVersionCode() < parsed.getLongVersionCode()) {
                    apksToInstall.add(child);
                }
            }
            if (!apksToInstall.isEmpty()) {
                doInstall(apksToInstall);
            }
        } catch (Throwable thr) {
            Slog.e(TAG, "doCheckOtaUpgradeApp error!", thr);
        }
    }

    private void doInstall(List<File> apksToInstall) throws IOException {
        BroadcastReceiver receiver = new InstallReceiver(apksToInstall.size());
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(ACTION_PENDING_INTENT_PREINSTALL);
        mPmService.mContext.registerReceiver(receiver, intentFilter);
        PackageInstaller installer = mPmService.mContext.getPackageManager().getPackageInstaller();
        byte[] buffer = new byte[1024 * 1024];
        for (File apk : apksToInstall) {
            Slog.d(TAG, "doCheckPreinstallApp preinstalling " + apk.getAbsolutePath());
            PackageInstaller.SessionParams sessionParams = new PackageInstaller.SessionParams(
                    PackageInstaller.SessionParams.MODE_FULL_INSTALL);
            sessionParams.installFlags |= PackageManager.INSTALL_ALL_USERS;
            int sessionId = installer.createSession(sessionParams);
            PackageInstaller.Session session = installer.openSession(sessionId);
            InputStream in = new FileInputStream(apk);
            long sizeBytes = apk.length();
            OutputStream out = session.openWrite(ACTION_PENDING_INTENT_PREINSTALL, 0, sizeBytes);
            try {
                int c;
                while ((c = in.read(buffer)) != -1) {
                    out.write(buffer, 0, c);
                    if (sizeBytes > 0) {
                        final float fraction = ((float) c / (float) sizeBytes);
                        session.addProgress(fraction);
                    }
                }
                session.fsync(out);
            } finally {
                closeQuietly(in);
                closeQuietly(out);
            }
            Intent broadcastIntent = new Intent(ACTION_PENDING_INTENT_PREINSTALL);
            broadcastIntent.setPackage(mPmService.mContext.getPackageName());
            PendingIntent pendingIntent = PendingIntent.getBroadcast(mPmService.mContext,
                    sessionId, broadcastIntent, PendingIntent.FLAG_UPDATE_CURRENT);
            session.commit(pendingIntent.getIntentSender());
        }
    }

    private static void closeQuietly(AutoCloseable closeable) {
        if (closeable != null) {
            try {
                closeable.close();
            } catch (RuntimeException rethrown) {
                throw rethrown;
            } catch (Exception ignored) {
            }
        }
    }

    private static class InstallReceiver extends BroadcastReceiver {
        int installCount;

        InstallReceiver(int count) {
            installCount = count;
            Slog.d(TAG, "preinstallchecked property: " + installCount);
            SystemProperties.set(SAMRTISAN_PREINSTALL_CHECKED_PROPERTY,
                    Integer.toString(installCount));
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            installCount--;
            SystemProperties.set(SAMRTISAN_PREINSTALL_CHECKED_PROPERTY,
                    Integer.toString(installCount));
            Slog.d(TAG, "preinstallchecked property: " + installCount);
            if (installCount == 0) {
                Intent broadcast = new Intent();
                broadcast.setAction(ACTION_PREINSTALL_APP_DONE);
                context.sendBroadcast(broadcast);
                context.unregisterReceiver(this);
                Slog.d(TAG, "sending preinstall done broadcast");
            }
        }
    }

    private static void deleteDirectory(File directory) {
        File[] files = directory.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    deleteDirectory(file);
                } else {
                    file.delete();
                }
            }
        }
        directory.delete();
    }

    void deleteBswFileIfExist(String packageName) {
        BlurStartingWindowUtils.deleteBswFileIfExist(packageName);
    }

    void deleteAllBswFiles() {
        long start = System.currentTimeMillis();
        BlurStartingWindowUtils.deleteAllBswFiles();
        Slog.v(TAG, "delete bsw files cost:" + (System.currentTimeMillis() - start));
    }

    static void handleMessageSmt(Message msg, PackageManagerService pms) {
        if (msg.what == PackageManagerServiceMonitorEx.DEX2OPT_IN_SCREENOFF_IDLE_MSG) {
            synchronized (PackageManagerServiceMonitorEx.pendingDexoptMap) {
                try {
                    Iterator<Map.Entry<String, DexoptOptions>> it =
                            PackageManagerServiceMonitorEx.pendingDexoptMap.entrySet().iterator();
                    while (it.hasNext()) {
                        if (!PackageManagerServiceMonitorEx.sDexoptResumed) {
                            return;
                        }
                        BatteryManagerInternal batteryManagerInternal =
                                LocalServices.getService(BatteryManagerInternal.class);
                        int batteryLevel = batteryManagerInternal.getBatteryLevel();
                        if (batteryLevel < 20) {
                            FeatLog.w(TAG, "FEAT_DELAY_DEX2OAT", 0,
                                    "battery level low, dexopt delayed batterylevel:"
                                            + batteryLevel);
                            return;
                        }
                        Map.Entry<String, DexoptOptions> item = it.next();
                        ISmartAnaly analysis = SysOptBridge.getFactory().getSmtAnalysis();
                        if (analysis.checkFileLock(item.getKey())) {
                            analysis.AnalysisFlock(item.getKey());
                            FeatLog.w(TAG, "FEAT_DELAY_DEX2OAT", 0,
                                    "file lock warning give up:" + item.getKey());
                            continue;
                        }
                        boolean success = pms.performDexOpt(item.getValue());
                        if (success) {
                            FeatLog.d(TAG, "FEAT_DELAY_DEX2OAT", 10,
                                    "idle dexopt success pkg:" + item.getKey());
                        } else {
                            FeatLog.e(TAG, "FEAT_DELAY_DEX2OAT", 10,
                                    "idle dexopt failure pkg:" + item.getKey());
                        }
                        it.remove();
                    }
                    PackageManagerServiceMonitorEx.stopIdleDex2oat();
                } catch (Exception e) {
                    PackageManagerServiceMonitorEx.stopIdleDex2oat();
                    FeatLog.e(TAG, "FEAT_DELAY_DEX2OAT", 0,
                            "Exception for dexopt " + e.getMessage());
                }
            }
        }
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
