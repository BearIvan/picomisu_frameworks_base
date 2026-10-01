// Copyright 2026 Picomisu contributors
// SPDX-License-Identifier: Apache-2.0
package android.app;

import android.content.pm.ApplicationInfo;
import android.os.Build;
import android.pico.utils.PicoUtils;
import android.util.Log;
import android.view.Display;

import java.util.ArrayList;
import java.util.List;

/**
 * PICO context extension (factory PICO OS 5.13.7 android.app.ExtContextImplImpl): a context
 * without its own display of a non-system app that runs on a 2D app display (flag 1 << 15)
 * reports that display instead of display 0 (apps targeting API 30+, or listed ones).
 * @hide
 */
public class ExtContextImplImpl implements IExtContextImpl {
    private static final String TAG = "ContextImpl";
    private static final List<String> sSyncApplicationDisplayIdList = new ArrayList<>();
    private ContextImpl mBase;

    public ExtContextImplImpl(ContextImpl base) {
        mBase = base;
    }

    static {
        sSyncApplicationDisplayIdList.add("com.immomo.momo");
    }

    /** ContextImpl.getDisplay without a display: the 2D app display of the process, or null. */
    @Override
    public Display redirectDisplayIfNeeded(ResourcesManager resourcesManager) {
        Display display;
        ApplicationInfo applicationInfo;
        if (mBase.mMainThread == null || mBase.mMainThread.getExt().getDisplayId() < 0) {
            return null;
        }
        if ((Build.VERSION.SDK_INT <= 29
                        && !sSyncApplicationDisplayIdList.contains(mBase.getPackageName()))
                || (display = resourcesManager.getAdjustedDisplay(
                        mBase.mMainThread.getExt().getDisplayId(), mBase.getResources())) == null
                || (display.getFlags() & 32768) <= 0
                || (applicationInfo = mBase.getApplicationInfo()) == null
                || PicoUtils.isSystemApp(applicationInfo)) {
            return null;
        }
        Log.w(TAG, "redirectDisplayIfNeeded [" + mBase + "], [" + display + "]");
        return display;
    }
}
