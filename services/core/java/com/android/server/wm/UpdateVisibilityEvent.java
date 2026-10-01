// Copyright 2026 Picomisu contributors
// SPDX-License-Identifier: Apache-2.0

package com.android.server.wm;

import com.android.server.am.IApplicationFreezer;

/**
 * Reconstructed from the PICO OS 5.13.7 factory services.
 *
 * @hide
 */
class UpdateVisibilityEvent extends FrozenPendingEvent {
    UpdateVisibilityEvent(WindowProcessController app) {
        super(app);
        this.unfreezeReason = IApplicationFreezer.UnfreezeReason.NEED_UPDATE_VISIBILITY;
    }

    @Override
    void handle() {
        this.mRootContainer.ensureActivitiesVisible(null, 0, false);
    }
}
