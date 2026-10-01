// Copyright 2026 Picomisu contributors
// SPDX-License-Identifier: Apache-2.0
package android.view;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.provider.PicoSettings;
import android.util.Log;

/**
 * PICO view extension (factory PICO OS 5.13.7 android.view.ExtViewImpl): whether the app of the
 * view is a VR app (PICO settings white list type "vr" or meta-data com.picovr.type=vr) and
 * whether it asks for a 180 degree display orientation.
 * @hide
 */
public class ExtViewImpl implements IExtView {
    private View mBase;

    public ExtViewImpl(View base) {
        mBase = base;
    }

    private boolean inStereoAppList() {
        String type = PicoSettings.WhiteList.getType(mBase.getContext().getContentResolver(),
                mBase.getContext().getPackageName(), null);
        Log.e("stereo", "PackageName:" + mBase.getContext().getPackageName() + " type:" + type);
        return "vr".equalsIgnoreCase(type);
    }

    @Override
    public boolean isTypeVR() {
        String value;
        if (inStereoAppList()) {
            return true;
        }
        Context context = mBase.getContext();
        if (context != null) {
            try {
                ApplicationInfo appInfo = context.getPackageManager().getApplicationInfo(
                        context.getPackageName(), PackageManager.GET_META_DATA);
                if (appInfo == null || appInfo.metaData == null
                        || (value = appInfo.metaData.getString("com.picovr.type")) == null) {
                    return false;
                }
                return value.equalsIgnoreCase("vr");
            } catch (PackageManager.NameNotFoundException e) {
            }
        }
        return false;
    }

    @Override
    public boolean isOrientation180() {
        Context context = mBase.getContext();
        if (context != null) {
            try {
                ApplicationInfo appInfo = context.getPackageManager().getApplicationInfo(
                        context.getPackageName(), PackageManager.GET_META_DATA);
                if (appInfo != null && appInfo.metaData != null) {
                    int value = appInfo.metaData.getInt("com.picovr.display.orientation");
                    return value == 180;
                }
            } catch (PackageManager.NameNotFoundException e) {
            }
        }
        return false;
    }
}
