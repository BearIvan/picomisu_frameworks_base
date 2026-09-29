// Copyright 2026 Picomisu contributors
// SPDX-License-Identifier: Apache-2.0
package android.app;

import android.view.View;
import android.view.ViewRootImpl;

/**
 * PICO activity-thread extension.
 * @hide
 */
public class ExtActivityThreadImpl implements IExtActivityThread {
    public static final String TAG = "ActivityThread";
    private final ActivityThread mBase;

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
}
