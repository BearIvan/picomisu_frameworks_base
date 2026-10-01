// Copyright 2026 Picomisu contributors
// SPDX-License-Identifier: Apache-2.0

package com.android.server.wm;

import android.view.WindowManagerPolicyConstantsSmtEx;

/**
 * Reconstructed from the PICO OS 5.13.7 factory services.
 *
 * @hide
 */
public abstract class WindowManagerInternalSmtBase {
    public abstract void registerVisibleWindowChangeListener(WindowManagerPolicyConstantsSmtEx.VisibleWindowChangeListenerSmtEx visibleWindowChangeListenerSmtEx);

    public abstract void unRegisterVisibleWindowChangeListener(WindowManagerPolicyConstantsSmtEx.VisibleWindowChangeListenerSmtEx visibleWindowChangeListenerSmtEx);
}
