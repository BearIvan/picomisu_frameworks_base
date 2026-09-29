// Copyright 2026 Picomisu contributors
// SPDX-License-Identifier: Apache-2.0
package android.content.pm;

import android.os.Parcel;

/**
 * PICO VR application flags and 2D virtual-display settings carried with
 * {@link ApplicationInfo}.
 * @hide
 */
public class ExtApplicationInfoImpl implements IExtApplicationInfo {
    private transient ApplicationInfo mBase;
    private int mVrAppFlag;
    private int m2dAppDensity;
    private int mVirtualDisplayPortraitWidth = 0;
    private int mVirtualDisplayPortraitHeight = 0;
    private int mVirtualDisplayLandscapeWidth = 0;
    private int mVirtualDisplayLandscapeHeight = 0;
    private int mForceOrientation = -1;
    private int mDefaultOrientation = 0;
    private int mLaunchActivityOrientation = -1;
    private int mDisplayId = -1;

    public ExtApplicationInfoImpl(ApplicationInfo base) {
        mBase = base;
    }

    @Override
    public void copyFrom(ApplicationInfo orig) {
        mVrAppFlag = orig.getExt().getVrAppFlag();
        m2dAppDensity = orig.getExt().get2dAppDensity();
        mVirtualDisplayPortraitWidth = orig.getExt().get2dAppPortraitWidth();
        mVirtualDisplayPortraitHeight = orig.getExt().get2dAppPortraitHeight();
        mVirtualDisplayLandscapeWidth = orig.getExt().get2dAppLandscapeWidth();
        mVirtualDisplayLandscapeHeight = orig.getExt().get2dAppLandscapeHeight();
        mForceOrientation = orig.getExt().get2dAppForceOrientation();
        mDefaultOrientation = orig.getExt().get2dAppDefaultOrientation();
        mLaunchActivityOrientation = orig.getExt().getLaunchActivityOrientation();
        mDisplayId = orig.getExt().getDisplayId();
    }

    @Override
    public int get2dAppDefaultOrientation() {
        return mDefaultOrientation;
    }

    @Override
    public int get2dAppDensity() {
        return m2dAppDensity;
    }

    @Override
    public int get2dAppForceOrientation() {
        return mForceOrientation;
    }

    @Override
    public int get2dAppLandscapeHeight() {
        return mVirtualDisplayLandscapeHeight;
    }

    @Override
    public int get2dAppLandscapeWidth() {
        return mVirtualDisplayLandscapeWidth;
    }

    @Override
    public int get2dAppOrientation() {
        int orientation = get2dAppOrientation(mLaunchActivityOrientation);
        return orientation == -1 ? mDefaultOrientation : orientation;
    }

    /** Maps an activity orientation to 0 (landscape), 1 (portrait) or -1. */
    @Override
    public int get2dAppOrientation(int orientation) {
        if (mForceOrientation != -1) {
            return mForceOrientation;
        }
        switch (orientation) {
            case ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE:
            case ActivityInfo.SCREEN_ORIENTATION_USER_LANDSCAPE:
            case ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE:
            case ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE:
                return 0;
            case ActivityInfo.SCREEN_ORIENTATION_PORTRAIT:
            case ActivityInfo.SCREEN_ORIENTATION_USER_PORTRAIT:
            case ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT:
            case ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT:
                return 1;
            default:
                return -1;
        }
    }

    @Override
    public int get2dAppPortraitHeight() {
        return mVirtualDisplayPortraitHeight;
    }

    @Override
    public int get2dAppPortraitWidth() {
        return mVirtualDisplayPortraitWidth;
    }

    @Override
    public int getDisplayId() {
        return mDisplayId;
    }

    @Override
    public int getLaunchActivityOrientation() {
        return mLaunchActivityOrientation;
    }

    @Override
    public int getVrAppFlag() {
        return mVrAppFlag;
    }

    @Override
    public boolean isAllComponentVr() {
        return (mVrAppFlag & FLAG_VR_APP) != 0 && (mVrAppFlag & FLAG_VR_APP_HAVE_2D_ACTIVITY) == 0;
    }

    @Override
    public boolean isVrApp() {
        return (mVrAppFlag & FLAG_VR_APP) != 0;
    }

    @Override
    public boolean isVrApplication() {
        return (mVrAppFlag & FLAG_APPLICATION_HAVE_VR_FLAG) != 0;
    }

    @Override
    public void readFromParcel(Parcel source) {
        mVrAppFlag = source.readInt();
        m2dAppDensity = source.readInt();
        mVirtualDisplayPortraitWidth = source.readInt();
        mVirtualDisplayPortraitHeight = source.readInt();
        mVirtualDisplayLandscapeWidth = source.readInt();
        mVirtualDisplayLandscapeHeight = source.readInt();
        mForceOrientation = source.readInt();
        mDefaultOrientation = source.readInt();
        mLaunchActivityOrientation = source.readInt();
        mDisplayId = source.readInt();
    }

    @Override
    public void set2dAppDefaultOrientation(int orientation) {
        mDefaultOrientation = orientation;
    }

    @Override
    public void set2dAppDensity(int density) {
        m2dAppDensity = density;
    }

    @Override
    public void set2dAppForceOrientation(int orientation) {
        mForceOrientation = orientation;
    }

    @Override
    public void set2dAppLandscapeHeight(int height) {
        mVirtualDisplayLandscapeHeight = height;
    }

    @Override
    public void set2dAppLandscapeWidth(int width) {
        mVirtualDisplayLandscapeWidth = width;
    }

    @Override
    public void set2dAppPortraitHeight(int height) {
        mVirtualDisplayPortraitHeight = height;
    }

    @Override
    public void set2dAppPortraitWidth(int width) {
        mVirtualDisplayPortraitWidth = width;
    }

    @Override
    public void setDisplayId(int displayId) {
        mDisplayId = displayId;
    }

    @Override
    public void setLaunchActivityOrientation(int orientation) {
        mLaunchActivityOrientation = orientation;
    }

    @Override
    public void setVrAppFlag(int flag) {
        mVrAppFlag |= flag;
    }

    @Override
    public void writeToParcel(Parcel dest) {
        dest.writeInt(mVrAppFlag);
        dest.writeInt(m2dAppDensity);
        dest.writeInt(mVirtualDisplayPortraitWidth);
        dest.writeInt(mVirtualDisplayPortraitHeight);
        dest.writeInt(mVirtualDisplayLandscapeWidth);
        dest.writeInt(mVirtualDisplayLandscapeHeight);
        dest.writeInt(mForceOrientation);
        dest.writeInt(mDefaultOrientation);
        dest.writeInt(mLaunchActivityOrientation);
        dest.writeInt(mDisplayId);
    }
}
