// Copyright 2026 Picomisu contributors
// SPDX-License-Identifier: Apache-2.0
package android.app;

import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.res.CompatibilityInfo;
import android.content.res.Configuration;
import android.hardware.display.DisplayManagerGlobal;
import android.os.Bundle;
import android.pico.utils.PicoUtils;
import android.util.Log;
import android.view.Display;
import android.view.IExtDisplay;
import android.view.View;
import android.view.ViewRootImpl;

import java.util.ArrayList;

/**
 * PICO activity-thread extension.
 * @hide
 */
public class ExtActivityThreadImpl implements IExtActivityThread {
    public static final String TAG = "ActivityThread";
    private ActivityThread mBase;
    /** Display of the first activity context (or of the bound application), -1 if none. */
    private int mDisplayId = -1;
    private ArrayList<Application.ActivityLifecycleCallbacks> mActivityLifecycleCallbacks =
            new ArrayList<>();

    public ExtActivityThreadImpl(ActivityThread base) {
        mBase = base;
    }

    /**
     * Whether the window of {@code viewRoot} must render normally. The first activity whose
     * decor is attached to {@code viewRoot} and is a VR activity decides; otherwise rendering
     * is forced. Unlike the factory, records without an Activity are skipped instead of
     * raising NullPointerException.
     */
    @Override
    public boolean isActivityForceRender(ViewRootImpl viewRoot) {
        if (mBase.mActivities.size() > 0) {
            for (ActivityThread.ActivityClientRecord r : mBase.mActivities.values()) {
                if (r == null || r.activity == null) {
                    continue;
                }
                View decor = r.activity.mDecor;
                if (decor != null && viewRoot == decor.getViewRootImpl()
                        && r.activityInfo.getExt().isVrActivity()) {
                    return r.activityInfo.getExt().isVrActivityForceRender();
                }
            }
        }
        return true;
    }

    /**
     * Records the display of the first activity context and, for a non-system application
     * living on a 2D virtual display, moves an activity context of the default display to
     * the application display.
     */
    @Override
    public ContextImpl adjustCreateBaseContextForActivity(ContextImpl appContext) {
        Application app = mBase.getApplication();
        if (app == null) {
            return appContext;
        }
        if (PicoUtils.isSystemApp(app.getApplicationInfo())) {
            return appContext;
        }
        if (mDisplayId < 0) {
            Display display = appContext.getDisplay();
            if (display != null
                    && (display.getFlags() & IExtDisplay.FLAG_2D_APP_VIRTUAL_DISPLAY) > 0) {
                Log.w(TAG, "adjustCreateBaseContextForActivity, update displayId ["
                        + mDisplayId + "], " + appContext);
            }
            mDisplayId = display.getDisplayId();
        }
        Display display = app.getDisplay();
        if (display != null && display.getDisplayId() > 0 && display.getExt().isVr2dDisplay()
                && appContext.getDisplayId() <= 0) {
            DisplayManagerGlobal dm = DisplayManagerGlobal.getInstance();
            Display compatibleDisplay = dm.getCompatibleDisplay(display.getDisplayId(),
                    appContext.getResources());
            if (compatibleDisplay == null) {
                return appContext;
            }
            appContext = (ContextImpl) appContext.createDisplayContext(compatibleDisplay);
            Log.w(TAG, "adjustCreateBaseContextForActivity adjust context display id to "
                    + display.getDisplayId());
        }
        return appContext;
    }

    /** Snapshot of the WebView activity lifecycle callbacks, or null when there are none. */
    @Override
    public Object[] collectActivityLifecycleCallbacks() {
        Object[] callbacks = null;
        synchronized (mActivityLifecycleCallbacks) {
            if (mActivityLifecycleCallbacks.size() > 0) {
                callbacks = mActivityLifecycleCallbacks.toArray();
            }
        }
        return callbacks;
    }

    @Override
    public int getDisplayId() {
        return mDisplayId;
    }

    @Override
    public void handleConfigurationChanged(Configuration config, CompatibilityInfo compat) {
        PicoUtils.initApplication(mBase.mInitialApplication.getApplicationInfo(), config);
    }

    /**
     * Asks the VR display service to show its loading UI when the application or activity
     * declares the "system" 2D loading flag. Returns false when the query fails.
     */
    @Override
    public boolean notifyAppLaunchStatus(ActivityThread.ActivityClientRecord acr) {
        try {
            String activityname = acr.activityInfo.name;
            String packagename = acr.activityInfo.packageName;
            Log.w(TAG, "notifyAppLaunchStatus " + packagename + " " + activityname);
            ComponentName componentName = new ComponentName(packagename, activityname);
            Bundle bundleApplication = mBase.getSystemContext().getPackageManager()
                    .getApplicationInfo(packagename, PackageManager.GET_META_DATA).metaData;
            String apploadingtype = bundleApplication == null
                    ? "none" : bundleApplication.getString("pxr.sdk.2dloading.flag");
            Bundle bundleActivity = mBase.getSystemContext().getPackageManager()
                    .getActivityInfo(componentName, PackageManager.GET_META_DATA).metaData;
            String activityloadingtype = bundleActivity == null
                    ? "none" : bundleActivity.getString("pxr.sdk.2dloading.flag");
            if ("system".equals(apploadingtype) || "system".equals(activityloadingtype)) {
                Intent loadingtintent = new Intent();
                loadingtintent.setAction("pvr.intent.action.vrdisplay");
                loadingtintent.setPackage("com.pvr.vrdisplay");
                loadingtintent.putExtra("action_type", -100);
                mBase.getSystemContext().startService(loadingtintent);
                Log.w(TAG, "apploadingtype is 2, show loading ui");
            }
            return true;
        } catch (Exception ex) {
            Log.e(TAG, "notifyAppLaunchStatus error:" + ex);
            return false;
        }
    }

    /** Records the application display and the configuration of the bound application. */
    @Override
    public void onBindApplication(ApplicationInfo appInfo, ActivityThread.AppBindData data) {
        Log.w(TAG, "onBindApplication appInfo [" + appInfo.packageName + "], ["
                + appInfo.getExt().getDisplayId() + "]");
        mDisplayId = appInfo.getExt().getDisplayId();
        PicoUtils.initApplication(appInfo, data.config);
    }

    @Override
    public void registerWebViewActivityLifecycleCallbacks(
            Application.ActivityLifecycleCallbacks callback) {
        if (callback == null) {
            return;
        }
        synchronized (mActivityLifecycleCallbacks) {
            mActivityLifecycleCallbacks.add(callback);
        }
    }

    @Override
    public void unregisterWebViewActivityLifecycleCallbacks(
            Application.ActivityLifecycleCallbacks callback) {
        if (callback == null) {
            return;
        }
        synchronized (mActivityLifecycleCallbacks) {
            mActivityLifecycleCallbacks.remove(callback);
        }
    }
}
