// Copyright 2026 Picomisu contributors
// SPDX-License-Identifier: Apache-2.0
package android.view;

import android.content.pm.ApplicationInfo;
import android.util.Log;

/**
 * PICO window manager extension (factory PICO OS 5.13.7 android.view.ExtWindowManagerImplImpl):
 * non-system 2D apps may not add floating windows of the deprecated TYPE_TOAST (2005) other than
 * real toasts.
 * @hide
 */
public class ExtWindowManagerImplImpl implements IExtWindowManagerImpl {
    private WindowManagerImpl mBase;

    public ExtWindowManagerImplImpl(WindowManagerImpl base) {
        mBase = base;
    }

    @Override
    public boolean disableAddView(View view, ViewGroup.LayoutParams params) {
        if (!view.getExt().isTypeVR()
                && (view.getContext().getApplicationInfo().flags & ApplicationInfo.FLAG_SYSTEM)
                        == 0
                && ((WindowManager.LayoutParams) params).type == 2005
                && !"Toast".equals(((WindowManager.LayoutParams) params).getTitle())) {
            Log.i("WindowManagerImpl", "Package "
                    + view.getContext().getApplicationInfo().packageName
                    + " is adding a toast type float window,deny this operation!");
            return true;
        }
        return false;
    }
}
