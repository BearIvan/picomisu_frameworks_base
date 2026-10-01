// Copyright 2026 Picomisu contributors
// SPDX-License-Identifier: Apache-2.0
package com.android.server.policy.keyguard;

import android.app.ActivityTaskManager;
import android.content.ComponentName;
import android.content.res.Resources;
import android.os.Handler;
import android.os.RemoteException;
import android.pico.utils.Features;

/**
 * PICO keyguard service delegate extension (factory PICO OS 5.13.7
 * com.android.server.policy.keyguard.ExtKeyguardServiceDelegateImpl): with the PICO keyguard
 * (Features.isKeyguardEnabled) the keyguard service bound by the window manager policy is
 * com.picovr.keyguard/com.pico.vr.keyguard.KeyguardService. Without it the configured component
 * is kept and the lock screen is reported hidden to the activity task manager.
 * @hide
 */
public class ExtKeyguardServiceDelegateImpl implements IExtKeyguardServiceDelegate {
    private KeyguardServiceDelegate mBase;

    public ExtKeyguardServiceDelegateImpl(KeyguardServiceDelegate base) {
        mBase = base;
    }

    @Override
    public ComponentName updateKeyguardStatus(Handler handler, Resources resources,
            ComponentName keyguardComponent) {
        if (Features.isKeyguardEnabled()) {
            return ComponentName.unflattenFromString(
                    "com.picovr.keyguard/com.pico.vr.keyguard.KeyguardService");
        }
        handler.post(() -> {
            try {
                ActivityTaskManager.getService().setLockScreenShown(false, false);
            } catch (RemoteException e) {
            }
        });
        return keyguardComponent;
    }
}
