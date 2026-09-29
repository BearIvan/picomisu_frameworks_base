// Copyright 2026 Picomisu contributors
// SPDX-License-Identifier: Apache-2.0
package android.content.pm;

import android.os.Parcel;
import com.pico.util.IExtBase;

/**
 * PICO VR application flags and 2D virtual-display settings of {@link ApplicationInfo}.
 * @hide
 */
public interface IExtApplicationInfo extends IExtBase {
    int FLAG_VR_APP = 1;
    int FLAG_VR_APP_HAVE_2D_ACTIVITY = 2;
    int FLAG_APPLICATION_HAVE_VR_FLAG = 4;

    void copyFrom(ApplicationInfo orig);
    int get2dAppDefaultOrientation();
    int get2dAppDensity();
    int get2dAppForceOrientation();
    int get2dAppLandscapeHeight();
    int get2dAppLandscapeWidth();
    int get2dAppOrientation();
    int get2dAppOrientation(int orientation);
    int get2dAppPortraitHeight();
    int get2dAppPortraitWidth();
    int getDisplayId();
    int getLaunchActivityOrientation();
    int getVrAppFlag();
    boolean isAllComponentVr();
    boolean isVrApp();
    boolean isVrApplication();
    void readFromParcel(Parcel source);
    void set2dAppDefaultOrientation(int orientation);
    void set2dAppDensity(int density);
    void set2dAppForceOrientation(int orientation);
    void set2dAppLandscapeHeight(int height);
    void set2dAppLandscapeWidth(int width);
    void set2dAppPortraitHeight(int height);
    void set2dAppPortraitWidth(int width);
    void setDisplayId(int displayId);
    void setLaunchActivityOrientation(int orientation);
    void setVrAppFlag(int flag);
    void writeToParcel(Parcel dest);
}
