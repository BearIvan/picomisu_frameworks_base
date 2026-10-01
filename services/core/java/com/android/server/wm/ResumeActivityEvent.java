// Copyright 2026 Picomisu contributors
// SPDX-License-Identifier: Apache-2.0

package com.android.server.wm;

import com.android.server.am.IApplicationFreezer;

/**
 * Reconstructed from the PICO OS 5.13.7 factory services.
 *
 * @hide
 */
class ResumeActivityEvent extends FrozenPendingEvent {
    ResumeActivityEvent(WindowProcessController app) {
        super(app);
        this.unfreezeReason = IApplicationFreezer.UnfreezeReason.NEED_RESUME_ACTIVITY;
    }

    @Override
    void handle() {
        this.mRootContainer.resumeFocusedStacksTopActivities();
    }
}
