// Copyright 2026 Picomisu contributors
// SPDX-License-Identifier: Apache-2.0

package com.android.server.policy;

import android.os.IBinder;
import android.view.View;

/**
 * Smartisan starting window surface that keeps its view for the screenshot cache.
 * Reconstructed from the PICO OS 5.13.7 factory services.
 *
 * @hide
 */
public class SmtStartingSurface extends SplashScreenSurface {
    private final View mView;

    public SmtStartingSurface(View view, IBinder appToken) {
        super(view, appToken);
        this.mView = view;
    }

    public View getStartingView() {
        return this.mView;
    }
}
