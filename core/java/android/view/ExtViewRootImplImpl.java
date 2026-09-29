// Copyright 2026 Picomisu contributors
// SPDX-License-Identifier: Apache-2.0
package android.view;

import android.app.ActivityThread;
import android.provider.Settings;

/**
 * PICO view-root extension: VR activity skip-draw policy.
 * @hide
 */
public class ExtViewRootImplImpl implements IExtViewRootImpl {
    private static final String PACKAGE_PERMISSION_CTRL = "com.android.permissioncontroller";
    private static final String SETTINGS_VR_ACTIVITY_SKIP_RENDER_ENABLED =
            "vr_activity_skip_render_enabled";

    private final ViewRootImpl mBase;
    private boolean mCanSkipDraw = false;
    private boolean mIsCheckedSkipDraw = false;

    public ExtViewRootImplImpl(ViewRootImpl base) {
        mBase = base;
    }

    /**
     * Evaluated once per view root: the global setting (default 1) must be 1 and the
     * matching VR activity must not force rendering.
     */
    private boolean canSkipDraw() {
        if (mIsCheckedSkipDraw) {
            return mCanSkipDraw;
        }
        mIsCheckedSkipDraw = true;
        if (Settings.Global.getInt(mBase.mContext.getContentResolver(),
                SETTINGS_VR_ACTIVITY_SKIP_RENDER_ENABLED, 1) == 1) {
            // Without an ActivityThread no activity can match, which means force rendering;
            // the factory dereferences it unconditionally.
            ActivityThread thread = ActivityThread.currentActivityThread();
            mCanSkipDraw = thread != null && !thread.getExt().isActivityForceRender(mBase);
        }
        return mCanSkipDraw;
    }

    /**
     * Whether software drawing should use the PICO 1x1 VR canvas instead of the window
     * buffer. Permission-controller windows and displays other than 0 always draw.
     */
    @Override
    public boolean isSkipDrawVrActivity() {
        if (mBase.getTitle().toString().contains(PACKAGE_PERMISSION_CTRL)) {
            return false;
        }
        return canSkipDraw() && mBase.mDisplay.getDisplayId() == 0;
    }
}
