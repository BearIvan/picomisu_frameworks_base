// Copyright 2026 Picomisu contributors
// SPDX-License-Identifier: Apache-2.0
package android.content.pm;

import android.os.Bundle;
import android.os.Parcel;
import android.text.TextUtils;

/**
 * PICO activity VR flags and 2D-panel metadata carried with {@link ActivityInfo}.
 * @hide
 */
public class ExtActivityInfoImpl implements IExtActivityInfo {
    public static final int FLAG_VR_ACTIVITY = 1;
    public static final int THEME_NO_CAPTION_BAR = 1;
    public static final int THEME_NO_NAVIGATION_BAR = 2;

    private transient ActivityInfo mBase;
    private int vrActivityFlag;
    private boolean mIsForceRender;
    private String m2dAppPosition = "far";
    private int m2dAppTheme = 0;
    private String mAppProperties;

    public ExtActivityInfoImpl(ActivityInfo base) {
        mBase = base;
    }

    // Factory copies do not include mIsForceRender; it is not read by any method.
    public void clone(IExtActivityInfo orig) {
        vrActivityFlag = orig.getVrActivityFlag();
        m2dAppPosition = orig.get2dAppPosition();
        m2dAppTheme = orig.get2dAppTheme();
        mAppProperties = orig.getAppProperties();
    }

    @Override
    public void copyFrom(ActivityInfo orig) {
        vrActivityFlag = orig.getExt().getVrActivityFlag();
        m2dAppPosition = orig.getExt().get2dAppPosition();
        m2dAppTheme = orig.getExt().get2dAppTheme();
        mAppProperties = orig.getExt().getAppProperties();
    }

    @Override
    public String get2dAppPosition() {
        return m2dAppPosition;
    }

    @Override
    public int get2dAppTheme() {
        return m2dAppTheme;
    }

    @Override
    public String getAppProperties() {
        return mAppProperties;
    }

    @Override
    public int getVrActivityFlag() {
        return vrActivityFlag;
    }

    @Override
    public boolean isVrActivity() {
        return (vrActivityFlag & FLAG_VR_ACTIVITY) != 0;
    }

    @Override
    public boolean isVrActivityForceRender() {
        return isVrActivity() && (vrActivityFlag & FLAG_VR_ACTIVITY_FORCE_RENDER) != 0;
    }

    @Override
    public void readFromParcel(Parcel source) {
        vrActivityFlag = source.readInt();
        m2dAppPosition = source.readString();
        m2dAppTheme = source.readInt();
        mAppProperties = source.readString();
    }

    @Override
    public void setVrActivity(int flag) {
        vrActivityFlag |= flag;
    }

    // The factory implementation ignores the argument and always sets the force-render bit.
    @Override
    public void setVrActivityForceRenderFlag(int flag) {
        vrActivityFlag |= FLAG_VR_ACTIVITY_FORCE_RENDER;
    }

    @Override
    public void updateAppFeature(Bundle metaData) {
        if (metaData == null) {
            return;
        }
        String position = metaData.getString("pico.vr.position", null);
        if (!TextUtils.isEmpty(position)) {
            m2dAppPosition = position.trim().toLowerCase();
        }
        mAppProperties = metaData.getString("pico.vr.app.prop", null);
        String theme = metaData.getString("pico.vr.theme", null);
        if (TextUtils.isEmpty(theme)) {
            return;
        }
        for (String item : theme.trim().toLowerCase().split("\\|")) {
            if (TextUtils.isEmpty(item)) {
                continue;
            }
            String name = item.trim().toLowerCase();
            if ("noCaptionBar".toLowerCase().equals(name)) {
                m2dAppTheme |= THEME_NO_CAPTION_BAR;
            } else if ("NoNavigationBar".toLowerCase().equals(name)) {
                m2dAppTheme |= THEME_NO_NAVIGATION_BAR;
            }
        }
    }

    @Override
    public void writeToParcel(Parcel dest, int parcelableFlags) {
        dest.writeInt(vrActivityFlag);
        dest.writeString(m2dAppPosition);
        dest.writeInt(m2dAppTheme);
        dest.writeString(mAppProperties);
    }
}
