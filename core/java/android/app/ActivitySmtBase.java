// Copyright 2026 Picomisu contributors
// SPDX-License-Identifier: Apache-2.0

package android.app;

import android.content.pm.ActivityInfo;

/**
 * Smartisan extension of an {@link Activity} (its {@code mSmtEx}): filters the first
 * {@link Activity#setRequestedOrientation} call of a landscape activity
 * (factory PICO OS 5.13.7 {@code android.app.ActivitySmtBase}).
 *
 * @hide
 */
public class ActivitySmtBase {
    public boolean hasRequestedOrientation = false;

    /**
     * Returns true when a landscape ({@link ActivityInfo#SCREEN_ORIENTATION_LANDSCAPE})
     * activity asks for landscape again before it requested any other orientation.
     */
    public boolean filterRequestedOrientation(int orientation, ActivityInfo activityInfo) {
        if (activityInfo != null && activityInfo.screenOrientation == 0
                && !hasRequestedOrientation) {
            if (orientation == 0) {
                return true;
            }
            hasRequestedOrientation = true;
            return false;
        }
        return false;
    }
}
