// Copyright 2026 Picomisu contributors
// SPDX-License-Identifier: Apache-2.0

package com.android.server.wm;

import android.view.IWindowSessionSmtEx;

import com.android.server.SysOptBridge;

/**
 * Smartisan extension of a window {@link Session} (its {@code mSmtEx}). Reconstructed from the
 * PICO OS 5.13.7 factory services.
 *
 * @hide
 */
public class SessionSmtBase {
    private IWindowSessionSmtEx mIWindowSessionSmtEx = (this).new IWindowSessionSmtExBase();
    protected Session mSession;

    public SessionSmtBase(Session session) {
        mSession = session;
    }

    public void updateVisibleSurfaceViewArea(int pid, int width, int height,
            boolean currentVisible) {
        SysOptBridge.getFactory().getSmartService().updateFocusSurfaceViewArea(pid, width, height,
                currentVisible);
    }

    public void onSurfaceViewVisibilityChanged(int pid, int visibility) {
        SysOptBridge.getFactory().getSmartService().onSurfaceViewVisibilityChanged(pid,
                visibility);
    }

    IWindowSessionSmtEx getISmtEx() {
        return mIWindowSessionSmtEx;
    }

    /** Binder of the Smartisan window session extension. */
    public class IWindowSessionSmtExBase extends IWindowSessionSmtEx.Stub {
        protected IWindowSessionSmtExBase() {
        }

        @Override
        public void updateVisibleSurfaceViewArea(int pid, int width, int height,
                boolean currentVisible) {
            SessionSmtBase.this.updateVisibleSurfaceViewArea(pid, width, height, currentVisible);
        }

        @Override
        public void onSurfaceViewVisibilityChanged(int pid, int visibility) {
            SessionSmtBase.this.onSurfaceViewVisibilityChanged(pid, visibility);
        }
    }
}
