// Copyright 2026 Picomisu contributors
// SPDX-License-Identifier: Apache-2.0

package android.view;

import android.os.RemoteException;

/**
 * Smartisan client side of {@link IWindowSessionSmtEx}. Reconstructed from the PICO OS 5.13.7
 * factory framework (which has no subclass or holder of it in the framework, services or the
 * preserved Smartisan JARs).
 *
 * @hide
 */
public abstract class WindowSessionSmtBase {
    private IWindowSessionSmtEx mISmtEx;

    protected IWindowSessionSmtEx getServiceSmtEx() {
        if (mISmtEx == null) {
            try {
                return WindowManagerGlobal.getWindowSession().getISmtEx();
            } catch (RemoteException e) {
                e.rethrowFromSystemServer();
            }
        }
        return mISmtEx;
    }

    public void updateVisibleSurfaceViewArea(int pid, int width, int height,
            boolean currentVisible) {
        try {
            getServiceSmtEx().updateVisibleSurfaceViewArea(pid, width, height, currentVisible);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void onSurfaceViewVisibilityChanged(int pid, int visibility) {
        try {
            getServiceSmtEx().onSurfaceViewVisibilityChanged(pid, visibility);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
