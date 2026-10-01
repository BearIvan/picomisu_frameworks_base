// Copyright 2026 Picomisu contributors
// SPDX-License-Identifier: Apache-2.0
package com.android.server.wm;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.hardware.display.DisplayManager;
import android.os.Bundle;
import android.os.SystemProperties;
import android.pico.utils.Features;
import android.util.Log;
import android.view.Display;

/**
 * PICO activity start interceptor extension (factory PICO OS 5.13.7
 * com.android.server.wm.ExtActivityStartInterceptorImpl): in hand-tracking mode a VR app that
 * does not support hand tracking is replaced by the hand dialog
 * (pvrdisplay.intent.action.HAND_DIALOG) on the VRShell virtual display.
 */
public class ExtActivityStartInterceptorImpl implements IExtActivityStartInterceptor {
    public static boolean DEBUG = false;
    private static final String TAG = "InterceptorUtils";
    private ActivityStartInterceptor mBase;

    public ExtActivityStartInterceptorImpl(ActivityStartInterceptor base) {
        mBase = base;
    }

    @Override
    public boolean intercept(Context context) {
        if (!Features.isHandDialogEnabled()) {
            return false;
        }
        try {
            ApplicationInfo applicationInfo = context.getPackageManager().getApplicationInfo(
                    mBase.mAInfo.packageName, PackageManager.GET_META_DATA);
            boolean coreSystemApp = applicationInfo != null && applicationInfo.isSystemApp()
                    && !"com.ss.android.ttvr".equals(applicationInfo.packageName)
                    && !"com.ss.android.ttvr.global".equals(applicationInfo.packageName)
                    && !"com.picovr.wing.videoplayer".equals(applicationInfo.packageName)
                    && !"com.picovr.gallery_local".equals(applicationInfo.packageName);
            boolean isVr = mBase.mAInfo != null && mBase.mAInfo.getExt().isVrActivity();
            if (isVr && !coreSystemApp) {
                boolean handMode = SystemProperties.getInt(
                        "sys.pxr.trackingservice.gesturemode", 0) == 1;
                if (!handMode) {
                    if (DEBUG) {
                        Log.i(TAG, "intercept: not handMode");
                    }
                    return false;
                }
                if (isAppSupportHandMode(applicationInfo)) {
                    if (DEBUG) {
                        Log.i(TAG, "intercept: isAppSupportHandMode");
                    }
                    return false;
                }
                Intent i = new Intent("pvrdisplay.intent.action.HAND_DIALOG");
                i.putExtra("realIntent", new Intent(mBase.mIntent));
                if (mBase.mActivityOptions != null) {
                    i.putExtra("options", new Bundle(mBase.mActivityOptions.toBundle()));
                    mBase.mActivityOptions.setLaunchDisplayId(getVRShellVirtualDisplayId(context));
                }
                mBase.mIntent = i;
                Log.w(TAG, "intercept: vr app is not support hand mode, so show dialog instead.");
                return true;
            }
            if (DEBUG) {
                Log.i(TAG, "intercept: not VrApp or is SystemApp");
            }
            return false;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
            return false;
        }
    }

    /** The VRShell virtual display ("PvrShellDisplay"), or -1. */
    private static int getVRShellVirtualDisplayId(Context context) {
        DisplayManager displayManager = context.getSystemService(DisplayManager.class);
        if (displayManager == null) {
            return -1;
        }
        for (Display display : displayManager.getDisplays()) {
            if (display.getType() == Display.TYPE_VIRTUAL
                    && "PvrShellDisplay".equals(display.getName())) {
                return display.getDisplayId();
            }
        }
        return -1;
    }

    private static boolean isAppSupportHandMode(ApplicationInfo applicationInfo) {
        return applicationInfo != null && applicationInfo.metaData != null
                && applicationInfo.metaData.getInt("handtracking") == 1;
    }
}
