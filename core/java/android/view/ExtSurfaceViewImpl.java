// Copyright 2026 Picomisu contributors
// SPDX-License-Identifier: Apache-2.0
package android.view;

import android.app.Activity;
import android.content.Context;

/**
 * PICO SurfaceView extension (factory PICO OS 5.13.7 android.view.ExtSurfaceViewImpl): on the
 * default display, the VR shell and the see-through settings activities get a shared 1x1 hidden
 * surface from their SurfaceView holders instead of the real one.
 * @hide
 */
public class ExtSurfaceViewImpl implements IExtSurfaceView {
    private static Surface mEmptySurface;
    private static SurfaceControl mEmptySurfaceControl;
    private SurfaceView mBase;

    public ExtSurfaceViewImpl(SurfaceView base) {
        mBase = base;
    }

    @Override
    public Surface getSurface() {
        Context context = mBase.mContext;
        if (context != null && context.getDisplayId() == 0 && (context instanceof Activity)) {
            Activity activity = (Activity) context;
            String componentName = activity.getComponentName().flattenToShortString();
            if ("com.pvr.vrshell/.MainActivity".equals(componentName)
                    || "com.pvr.seethrough.setting/.MainActivity".equals(componentName)) {
                return createEmptySurface();
            }
        }
        return mBase.mSurface;
    }

    private Surface createEmptySurface() {
        if (mEmptySurfaceControl == null) {
            mEmptySurfaceControl = new SurfaceControl.Builder(new SurfaceSession())
                    .setName("empty_suface")
                    .setOpaque(true)
                    .setBufferSize(1, 1)
                    .setFormat(4)
                    .setFlags(4)
                    .build();
        }
        if (mEmptySurface == null) {
            mEmptySurface = new Surface();
            mEmptySurface.createFrom(mEmptySurfaceControl);
        }
        return mEmptySurface;
    }
}
