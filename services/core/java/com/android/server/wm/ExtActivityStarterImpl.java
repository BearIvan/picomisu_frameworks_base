// Copyright 2026 Picomisu contributors
// SPDX-License-Identifier: Apache-2.0
package com.android.server.wm;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.os.ServiceManager;
import android.os.SystemProperties;
import android.pico.utils.PicoUtils;
import android.text.TextUtils;
import android.util.Slog;

import com.android.server.api.ApiLayerService;
import com.pvr.IPvrManagerService;

import java.util.HashMap;

/**
 * PICO activity starter extension (factory PICO OS 5.13.7
 * com.android.server.wm.ExtActivityStarterImpl): starts are cancelled while the VR loading
 * screen or the app abort mode is up, third-party apps may not start a launcher, missing VR
 * runtime permissions are requested first, and pvr_manager / the API layer learn about every
 * activity start.
 */
public class ExtActivityStarterImpl implements IExtActivityStarter {
    private static final String TAG = "ActivityTaskManager";
    private static final HashMap<String, String[]> WHITELIST_PCAKGE_PERMISSION = new HashMap<>();
    private ActivityStarter mBase;
    private IPvrManagerService mPvrManagerService;

    static {
        WHITELIST_PCAKGE_PERMISSION.put("com.motionx.pico.xkart", new String[]{
                "android.permission.WRITE_EXTERNAL_STORAGE",
                "android.permission.READ_EXTERNAL_STORAGE"});
        WHITELIST_PCAKGE_PERMISSION.put("com.tvb.cubism", new String[]{
                "android.permission.WRITE_EXTERNAL_STORAGE",
                "android.permission.READ_EXTERNAL_STORAGE"});
        WHITELIST_PCAKGE_PERMISSION.put("com.StormingTech.RestInPiecesPico", new String[]{
                "android.permission.WRITE_EXTERNAL_STORAGE",
                "android.permission.READ_EXTERNAL_STORAGE"});
        WHITELIST_PCAKGE_PERMISSION.put("com.SpheroomLimited.Jentrix", new String[]{
                "android.permission.WRITE_EXTERNAL_STORAGE",
                "android.permission.READ_EXTERNAL_STORAGE"});
        WHITELIST_PCAKGE_PERMISSION.put("com.MyronSoftware.Deisim", new String[]{
                "android.permission.WRITE_EXTERNAL_STORAGE",
                "android.permission.READ_EXTERNAL_STORAGE"});
        WHITELIST_PCAKGE_PERMISSION.put("com.sumalab.CrisisVRigade2", new String[]{
                "android.permission.WRITE_EXTERNAL_STORAGE",
                "android.permission.READ_EXTERNAL_STORAGE"});
    }

    public ExtActivityStarterImpl(ActivityStarter base) {
        mBase = base;
    }

    /**
     * True (START_CANCELED) while sys.pxr.loading.status or sys.pxr.appabort.mode is 1. The
     * usage access settings started by the Tencent app store lose FLAG_ACTIVITY_NEW_TASK.
     */
    @Override
    public boolean interruptStartActivity(String callingPackage, Intent intent) {
        if ("com.tencent.android.qqdownloader".equals(callingPackage)
                && "android.settings.USAGE_ACCESS_SETTINGS".equals(intent.getAction())) {
            intent.setFlags(intent.getFlags() & ~Intent.FLAG_ACTIVITY_NEW_TASK);
        }
        return SystemProperties.getInt("sys.pxr.loading.status", 0) == 1
                || SystemProperties.getInt("sys.pxr.appabort.mode", 0) == 1;
    }

    /** The API layer and pvr_manager ("activity_status") learn about the start. */
    @Override
    public void sendActivityStartingMsg(ActivityInfo aInfo) {
        ApiLayerService.getInstance().onActivityStarting(aInfo);
        if (mPvrManagerService == null || mPvrManagerService.asBinder() == null
                || !mPvrManagerService.asBinder().isBinderAlive()) {
            if (ServiceManager.getService("pvr_manager") != null) {
                mPvrManagerService = IPvrManagerService.Stub.asInterface(
                        ServiceManager.getService("pvr_manager"));
            } else {
                Slog.w(TAG, "pvr_manager has not been added to ServiceManager,do nothing.");
            }
        }
        if (mPvrManagerService != null) {
            try {
                mPvrManagerService.sendPvrMessages("activity_status", "activityStarting:"
                        + aInfo.applicationInfo.packageName + "," + aInfo.name);
            } catch (Exception e) {
                Slog.e(TAG, "mPvrManagerService sendPvrMessages error");
            }
        }
    }

    /** Third-party apps (uid above 10000) may not start a CATEGORY_HOME activity. */
    @Override
    public boolean refuseStartLauncher(Intent intent, int callingUid) {
        if (intent.hasCategory(Intent.CATEGORY_HOME) && callingUid > 10000) {
            Slog.w(TAG, "refuse third part app start launcher.");
            return true;
        }
        return false;
    }

    @Override
    public boolean helpRequestPermission(ActivityInfo aInfo, Context context, int userId) {
        return PicoUtils.helpRequestPermission(aInfo, context, userId);
    }

    /** A prefetched (pre-started) app starting the VR permission activity. */
    @Override
    public boolean isPrefetchAppRequestPermission(WindowProcessController caller,
            ActivityInfo aInfo) {
        return caller != null && caller.mInfo != null && aInfo != null
                && caller.mInfo.getSmtEx().isPrefetch
                && TextUtils.equals(aInfo.name,
                        "com.android.packageinstaller.permission.ui.pico.VrGrantPermissionsActivity");
    }
}
