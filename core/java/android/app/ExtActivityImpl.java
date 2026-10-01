// Copyright 2026 Picomisu contributors
// SPDX-License-Identifier: Apache-2.0
package android.app;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.ServiceManager;
import android.text.TextUtils;
import android.util.Log;
import android.util.Slog;
import android.view.Window;
import android.view.inputmethod.InputMethodManager;

import com.pvr.IPvrManagerService;

/**
 * PICO activity extension (factory PICO OS 5.13.7 android.app.ExtActivityImpl):
 * <ul>
 * <li>onResume reports "activityOnResume:&lt;package&gt;,&lt;class&gt;" to pvr_manager
 * ("activity_status");</li>
 * <li>permission requests of a VR app or activity (pvr.app.type / com.picovr.type "vr" meta-data)
 * use the VR permission dialog action, and the optional permission descriptions of
 * {@link Activity#requestPermissions(String[], int, String[])} are passed to it;</li>
 * <li>finishing the Douyin login activity hides its soft input.</li>
 * </ul>
 * @hide
 */
public class ExtActivityImpl implements IExtActivity {
    private static final String TAG = "Activity";
    private static final String PICO_PERMISSIONS_DESCRIPTION_KEY = "pico_permissions_description";
    private static final String VR_PERMISSION_ACTION =
            "android.content.pm.action.REQUEST_PERMISSIONS_VR";

    private Activity mBase;
    private IPvrManagerService mPvrManagerService;

    public ExtActivityImpl(Activity base) {
        mBase = base;
    }

    @Override
    public void onResumeCalled() {
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
                // As on the factory, the class name is the one of this extension object.
                mPvrManagerService.sendPvrMessages("activity_status",
                        "activityOnResume:" + mBase.getPackageName() + ","
                                + getClass().getName());
            } catch (Exception e) {
                Slog.e(TAG, "mPvrManagerService sendPvrMessages error");
            }
        }
    }

    @Override
    public void onRequestPermissionsFromFragmentCalled(Intent intent,
            String[] permissionDescriptions) {
        setActionAndDescriptions(intent, permissionDescriptions);
    }

    @Override
    public void requestPermissionsCalled(Intent intent, String[] permissionDescriptions) {
        setActionAndDescriptions(intent, permissionDescriptions);
    }

    private void setActionAndDescriptions(Intent intent, String[] permissionDescriptions) {
        if (isVrType()) {
            intent.setAction(VR_PERMISSION_ACTION);
        }
        if (permissionDescriptions != null) {
            intent.putExtra(PICO_PERMISSIONS_DESCRIPTION_KEY, permissionDescriptions);
        }
    }

    private boolean isVrType() {
        try {
            ApplicationInfo callingApplicationInfo = mBase.getPackageManager()
                    .getApplicationInfo(mBase.getPackageName(), PackageManager.GET_META_DATA);
            boolean callingIsVrApp = callingApplicationInfo.metaData != null
                    && (TextUtils.equals(callingApplicationInfo.metaData.getString("pvr.app.type"),
                            "vr")
                    || TextUtils.equals(callingApplicationInfo.metaData.getString(
                            "com.picovr.type"), "vr"));
            if (callingIsVrApp) {
                return callingIsVrApp;
            }
            ActivityInfo activityInfo = mBase.getPackageManager().getActivityInfo(
                    mBase.getComponentName(), PackageManager.GET_META_DATA);
            boolean isVrActivity = activityInfo != null && activityInfo.metaData != null
                    && (TextUtils.equals(activityInfo.metaData.getString("pvr.app.type"), "vr")
                    || TextUtils.equals(activityInfo.metaData.getString("com.picovr.type"),
                            "vr"));
            if (isVrActivity) {
                return isVrActivity;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    @Override
    public void onFinish() {
        if (ComponentName.unflattenFromString("com.ss.android.ugc.aweme/"
                + "com.ss.android.ugc.aweme.account.business.login.DYLoginActivity")
                .equals(mBase.getComponentName())) {
            forceHideImeWhenActivityFinish();
        }
    }

    private void forceHideImeWhenActivityFinish() {
        try {
            Window window = mBase.getWindow();
            if (window != null && window.getDecorView() != null) {
                InputMethodManager imm = (InputMethodManager) mBase.getSystemService(
                        Context.INPUT_METHOD_SERVICE);
                if (imm != null && imm.isActive(imm.getExt().getServedView())) {
                    Log.w(TAG, "forceHideImeWhenActivityFinish " + imm.getExt().getServedView());
                    imm.hideSoftInputFromWindow(window.getDecorView().getWindowToken(), 0);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
