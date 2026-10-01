// Copyright 2026 Picomisu contributors
// SPDX-License-Identifier: Apache-2.0

package com.android.server.wm;

import android.app.IApplicationThread;
import android.app.servertransaction.ActivityLifecycleItem;
import android.app.servertransaction.DestroyActivityItem;
import android.os.IBinder;
import android.util.Slog;
import android.view.IApplicationToken;
import com.android.server.am.IApplicationFreezer;

/**
 * Reconstructed from the PICO OS 5.13.7 factory services.
 *
 * @hide
 */
class DestroyActivityEvent extends FrozenPendingEvent {
    private DestroyActivityItem activityLifecycleItem;
    private IApplicationToken.Stub appToken;
    private IApplicationThread thread;

    DestroyActivityEvent(WindowProcessController app, IApplicationThread _thread, IApplicationToken.Stub _appToken, DestroyActivityItem _activityLifecycleItem) {
        super(app);
        this.unfreezeReason = IApplicationFreezer.UnfreezeReason.NEED_DESTROY_ACTIVITY;
        this.thread = _thread;
        this.appToken = _appToken;
        this.activityLifecycleItem = _activityLifecycleItem;
    }

    @Override
    void handle() {
        try {
            this.mApp.mAtm.getLifecycleManager().scheduleTransaction(this.thread, (IBinder) this.appToken, (ActivityLifecycleItem) this.activityLifecycleItem);
        } catch (Exception e) {
            Slog.w("ActivityTaskManagerService", "scheduleTransaction failed" + this.mApp);
        }
    }
}
