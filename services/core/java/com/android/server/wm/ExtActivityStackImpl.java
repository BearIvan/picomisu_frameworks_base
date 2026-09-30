// Copyright 2026 Picomisu contributors
// SPDX-License-Identifier: Apache-2.0

package com.android.server.wm;

/**
 * PICO activity stack extension.
 * @hide
 */
public class ExtActivityStackImpl implements IExtActivityStack {
    private ActivityStack mBase;

    public ExtActivityStackImpl(ActivityStack base) {
        mBase = base;
    }

    /**
     * Asks the process of {@code r} to log its main thread stack for an activity pause, stop,
     * destroy or top-resumed-state-loss timeout.
     */
    @Override
    public void scheduleActivityTimeout(ActivityRecord r, String reason) {
        if (!r.hasProcess()) {
            return;
        }
        try {
            r.app.getThread().scheduleActivityTimeout(reason);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
