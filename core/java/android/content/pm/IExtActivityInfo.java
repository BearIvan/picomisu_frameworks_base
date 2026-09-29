// Copyright 2026 Picomisu contributors
// SPDX-License-Identifier: Apache-2.0
package android.content.pm;

import android.os.Bundle;
import android.os.Parcel;
import com.pico.util.IExtBase;

/**
 * PICO VR and 2D-panel extension of {@link ActivityInfo}.
 * @hide
 */
public interface IExtActivityInfo extends IExtBase {
    int FLAG_VR_ACTIVITY = 1;
    int FLAG_VR_ACTIVITY_FORCE_RENDER = 2;
    int THEME_NO_CAPTION_BAR = 1;
    int THEME_NO_NAVIGATION_BAR = 2;

    void copyFrom(ActivityInfo orig);
    String get2dAppPosition();
    int get2dAppTheme();
    String getAppProperties();
    int getVrActivityFlag();
    boolean isVrActivity();
    boolean isVrActivityForceRender();
    void readFromParcel(Parcel source);
    void setVrActivity(int flag);
    void setVrActivityForceRenderFlag(int flag);
    void updateAppFeature(Bundle metaData);
    void writeToParcel(Parcel dest, int parcelableFlags);
}
