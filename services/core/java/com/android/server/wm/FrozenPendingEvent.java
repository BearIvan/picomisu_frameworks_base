// Copyright 2026 Picomisu contributors
// SPDX-License-Identifier: Apache-2.0

package com.android.server.wm;

import com.android.server.am.IApplicationFreezer;

/**
 * Reconstructed from the PICO OS 5.13.7 factory services.
 *
 * @hide
 */
public abstract class FrozenPendingEvent {
    protected WindowProcessController mApp;
    protected RootActivityContainer mRootContainer;
    public IApplicationFreezer.UnfreezeReason unfreezeReason;

    abstract void handle();

    FrozenPendingEvent(WindowProcessController app) {
        this.mApp = app;
        this.mRootContainer = app.mAtm.mRootActivityContainer;
    }

    public boolean equals(FrozenPendingEvent event) {
        if (event == null || this.mApp != event.mApp || this.unfreezeReason != event.unfreezeReason) {
            return false;
        }
        return true;
    }
}
