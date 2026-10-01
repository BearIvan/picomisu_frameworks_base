// Copyright 2026 Picomisu contributors
// SPDX-License-Identifier: Apache-2.0
package android.app;

import android.os.SystemProperties;

/**
 * PICO dialog extension (factory PICO OS 5.13.7 android.app.ExtDialogImpl): system dialogs
 * (package "android") appear on the display of the top resumed activity, which system_server
 * publishes in pvr.focused.display.id.
 * @hide
 */
public class ExtDialogImpl implements IExtDialog {
    private Dialog mBase;

    public ExtDialogImpl(Dialog base) {
        mBase = base;
    }

    @Override
    public void adjustDialogContext() {
        if ("android".equals(mBase.mContext.getPackageName())) {
            int focusedDisplay = SystemProperties.getInt("pvr.focused.display.id", 0);
            mBase.mContext.updateDisplay(focusedDisplay);
        }
    }
}
