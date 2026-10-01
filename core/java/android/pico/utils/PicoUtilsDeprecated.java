// Copyright 2026 Picomisu contributors
// SPDX-License-Identifier: Apache-2.0

package android.pico.utils;

import android.app.ActivityThread;
import android.app.Application;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.IBinder;
import android.os.Parcel;
import android.os.ServiceManager;
import android.os.SystemProperties;
import android.pico.ns.NsTransactionInterface;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.DisplayInfo;
import android.view.ViewRootImpl;

/**
 * Deprecated PICO 2D-app resource helpers of the PICO OS 5.13.7 factory framework: resize the
 * application resources of non-VR, non-system apps to their display (2880 x 1600 virtual screen
 * on the default display) and tell the native shell about IME visibility. The factory keeps the
 * class but no longer calls it.
 *
 * @hide
 */
public class PicoUtilsDeprecated {
    private static final String TAG = "PicoResourcesUtils";
    private static final int DEF_VIRTUAL_SCREEN_WIDTH = 2880;
    private static final int DEF_VIRTUAL_SCREEN_HEIGHT = 1600;
    private static boolean ENABLE_RESOURCE_UPDATE =
            SystemProperties.getBoolean("persist.pvr.resources.update", true);
    private static boolean DEBUG_RESOURCES_UPDATE =
            SystemProperties.getBoolean("pvr.debug.resources", false);
    private static ApplicationInfo mSrcApplicationInfo;
    private static Context mSystemContext;

    public static void updateApplicationContextResources(ViewRootImpl viewRoot, Display display) {
        Application app = ActivityThread.currentApplication();
        if (!ENABLE_RESOURCE_UPDATE || app == null || app.getResources() == null || isVrType()) {
            return;
        }
        ApplicationInfo appInfo = app.getApplicationInfo();
        boolean isSystemApp = appInfo != null && appInfo.isSystemApp();
        String packageName = app.getPackageName();
        if (isSystemApp && !"com.android.inputmethod.latin".equals(packageName)
                && !"com.iflytek.inputmethod.pico".equals(packageName)) {
            return;
        }
        Resources appResouces = app.getResources();
        if (display.getDisplayId() == app.getDisplayId()
                && appResouces.getDisplayMetrics().widthPixels == display.getWidth()) {
            return;
        }
        DisplayMetrics dm = new DisplayMetrics();
        display.getMetrics(dm);
        app.updateDisplay(display.getDisplayId());
        if (needAdjust(viewRoot)) {
            Configuration configuration = new Configuration(appResouces.getConfiguration());
            configuration.orientation =
                    viewRoot.mContext.getResources().getConfiguration().orientation;
            appResouces.updateConfiguration(configuration, dm, null);
        } else {
            appResouces.updateConfiguration(appResouces.getConfiguration(), dm, null);
        }
        viewRoot.requestLayout();
        if (DEBUG_RESOURCES_UPDATE) {
            Log.e(TAG, "update application context resources: pkg= " + packageName
                    + ", displayId=" + display.getDisplayId()
                    + ", resourcesImpl=" + appResouces.getImpl());
        }
    }

    private static boolean needAdjust(ViewRootImpl viewRoot) {
        Application app = ActivityThread.currentApplication();
        String packageName = app.getPackageName();
        boolean isAdjustPkg = "com.tencent.android.qqdownloader".equals(packageName)
                || "com.tencent.mtt".equals(packageName);
        int appOrientation = app.getResources().getConfiguration().orientation;
        int actOrientation = viewRoot.mContext.getResources().getConfiguration().orientation;
        return isAdjustPkg && appOrientation != actOrientation;
    }

    private static boolean isVrType() {
        ApplicationInfo applicationInfo = null;
        try {
            applicationInfo = mSystemContext.getPackageManager()
                    .getApplicationInfo(mSrcApplicationInfo.packageName, 128);
        } catch (Exception e) {
            Log.e(TAG, e.toString());
        }
        if (applicationInfo == null || applicationInfo.metaData == null) {
            return false;
        }
        return "vr".equalsIgnoreCase(applicationInfo.metaData.getString("com.picovr.type"))
                || "vr".equalsIgnoreCase(applicationInfo.metaData.getString("pvr.app.type"));
    }

    public static void dispatchImeVisibleStatusToNS(int displayId) {
        try {
            IBinder ns = ServiceManager.getService("native_shell");
            if (ns == null || !ns.isBinderAlive()) {
                return;
            }
            Parcel data = Parcel.obtain();
            Parcel reply = Parcel.obtain();
            int code = NsTransactionInterface.CODE_CLIENT_ACQUIRE_SURFACE;
            data.writeInterfaceToken("com.bytedance.IRemoteCallback");
            data.writeInt(displayId);
            ns.transact(code, data, reply, 1);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void initApplication(ApplicationInfo applicationInfo) {
        ActivityThread activityThread = ActivityThread.currentActivityThread();
        mSystemContext = activityThread.getSystemContext();
        mSrcApplicationInfo = applicationInfo;
    }

    public static void updateDisplayInfo(DisplayInfo displayInfo) {
        ActivityThread activityThread = ActivityThread.currentActivityThread();
        if (ActivityThread.isSystem() || mSrcApplicationInfo == null
                || mSrcApplicationInfo.isSystemApp() || isVrType()
                || displayInfo.displayId != 0) {
            return;
        }
        if (DEBUG_RESOURCES_UPDATE) {
            Log.e(TAG, "updateDisplayInfo : pkg= " + mSrcApplicationInfo.packageName
                    + ", before DisplayInfo=" + displayInfo + ", change to 2880 * 1600");
        }
        int width = DEF_VIRTUAL_SCREEN_WIDTH;
        int height = DEF_VIRTUAL_SCREEN_HEIGHT;
        if (displayInfo.appWidth < displayInfo.appHeight) {
            width = DEF_VIRTUAL_SCREEN_HEIGHT;
            height = DEF_VIRTUAL_SCREEN_WIDTH;
        }
        displayInfo.appWidth = width;
        displayInfo.logicalWidth = width;
        displayInfo.appHeight = height;
        displayInfo.logicalHeight = height;
    }

    public static void updateDisplayMetrics(Application application) {
        ApplicationInfo info = application.getApplicationInfo();
        if (!ENABLE_RESOURCE_UPDATE || ActivityThread.isSystem() || info == null
                || info.isSystemApp() || isVrType()) {
            return;
        }
        DisplayMetrics orginDm = application.getResources().getDisplayMetrics();
        DisplayMetrics newDm = new DisplayMetrics();
        newDm.setTo(orginDm);
        int displayId = application.getDisplayId();
        if (displayId != 0) {
            Display display = application.getDisplay();
            display.getMetrics(newDm);
            if (newDm.widthPixels != 0 && newDm.heightPixels != 0) {
                if (DEBUG_RESOURCES_UPDATE) {
                    Log.e(TAG, "updateDisplayMetrics : pkg= " + application.getPackageName()
                            + ", before DisplayInfo=" + orginDm + ", change to " + newDm);
                }
                application.getResources().getDisplayMetrics().setTo(newDm);
                return;
            }
        }
        if (DEBUG_RESOURCES_UPDATE) {
            Log.e(TAG, "updateDisplayMetrics : pkg= " + application.getPackageName()
                    + ", before DisplayInfo=" + orginDm + ", change to 2880 * 1600");
        }
        orginDm.widthPixels = DEF_VIRTUAL_SCREEN_WIDTH;
        orginDm.heightPixels = DEF_VIRTUAL_SCREEN_HEIGHT;
    }
}
