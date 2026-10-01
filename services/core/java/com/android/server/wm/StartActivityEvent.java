// Copyright 2026 Picomisu contributors
// SPDX-License-Identifier: Apache-2.0

package com.android.server.wm;

import android.os.RemoteException;
import android.util.Slog;
import com.android.server.am.IApplicationFreezer;

/**
 * Reconstructed from the PICO OS 5.13.7 factory services.
 *
 * @hide
 */
class StartActivityEvent extends FrozenPendingEvent {
    StartActivityEvent(WindowProcessController app) {
        super(app);
        this.unfreezeReason = IApplicationFreezer.UnfreezeReason.NEED_START_ACTIVITY;
    }

    @Override
    void handle() {
        try {
            this.mRootContainer.attachApplication(this.mApp);
        } catch (RemoteException e) {
            Slog.w("ActivityTaskManagerService", "attachApplication failed, proc=" + this.mApp);
        }
    }
}
