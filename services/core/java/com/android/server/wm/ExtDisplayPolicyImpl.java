// Copyright 2026 Picomisu contributors
// SPDX-License-Identifier: Apache-2.0
package com.android.server.wm;

import android.graphics.Rect;
import android.view.WindowManager;

/**
 * PICO display policy extension (factory PICO OS 5.13.7
 * com.android.server.wm.ExtDisplayPolicyImpl).
 */
public class ExtDisplayPolicyImpl implements IExtDisplayPolicy {
    private DisplayPolicy mBase;

    public ExtDisplayPolicyImpl(DisplayPolicy policy) {
        mBase = policy;
    }

    /** DisplayPolicy.layoutWindowLw: windows of type 2998 are laid out in the stable frame. */
    @Override
    public void calculateFrameWhenLayoutWindowLw(WindowState win, DisplayFrames displayFrames,
            Rect pf, Rect df, Rect of, Rect cf, Rect vf, Rect dcf, Rect sf) {
        WindowManager.LayoutParams attrs = win.getAttrs();
        int type = attrs.type;
        if (type == 2998) {
            cf.set(displayFrames.mStable);
            of.set(displayFrames.mStable);
            df.set(displayFrames.mStable);
            pf.set(displayFrames.mStable);
        }
    }
}
