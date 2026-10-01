// Copyright 2026 Picomisu contributors
// SPDX-License-Identifier: Apache-2.0
package com.android.server.wm;

import static com.android.server.wm.WindowManagerDebugConfig.DEBUG_WINDOW_MOVEMENT;

import android.util.Slog;

/**
 * PICO window token extension (factory PICO OS 5.13.7
 * com.android.server.wm.ExtWindowTokenImpl).
 */
public class ExtWindowTokenImpl implements IExtWindowToken {
    private WindowToken mBase;

    public ExtWindowTokenImpl(WindowToken base) {
        mBase = base;
    }

    /**
     * WindowToken.removeAllWindowsIfPossible: removes the windows from a copy of the child list,
     * so a window that removes other children of the token while it is removed does not break
     * the iteration.
     */
    @Override
    public boolean removeAllWindowsIfPossible() {
        WindowList<WindowState> tmpChildren = new WindowList<>();
        tmpChildren.addAll(mBase.mChildren);
        for (int i = tmpChildren.size() - 1; i >= 0; i--) {
            WindowState win = tmpChildren.get(i);
            if (DEBUG_WINDOW_MOVEMENT) {
                Slog.w("WindowManager", "removeAllWindowsIfPossible: removing win=" + win);
            }
            win.removeIfPossible();
        }
        tmpChildren.clear();
        return true;
    }
}
