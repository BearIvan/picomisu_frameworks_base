// Copyright 2026 Picomisu contributors
// SPDX-License-Identifier: Apache-2.0
package android.app;

import android.content.Context;
import android.os.RemoteException;
import android.os.SystemProperties;
import android.pico.utils.Features;
import android.view.IWindowManager;

/**
 * PICO KeyguardManager extension (factory PICO OS 5.13.7 android.app.ExtKeyguardManagerImpl).
 * Without the VR keyguard (com.picovr.keyguard), the device and the keyguard count as secure
 * unless keyguard.secured.debug is 0 or 1; 2 and above ask the window manager / trust manager.
 * @hide
 */
public class ExtKeyguardManagerImpl implements IExtKeyguardManager {
    private KeyguardManager mBase;

    public ExtKeyguardManagerImpl(KeyguardManager base) {
        mBase = base;
    }

    @Override
    public boolean hasVrKeyguard() {
        return Features.isKeyguardEnabled();
    }

    @Override
    public boolean isKeyguardSecure(IWindowManager wm, Context context) {
        int debugValue = SystemProperties.getInt("keyguard.secured.debug", -1);
        if (debugValue < 0) {
            return true;
        }
        if (debugValue < 2) {
            return debugValue == 1;
        }
        try {
            return wm.isKeyguardSecure(context.getUserId());
        } catch (RemoteException e) {
            return false;
        }
    }

    @Override
    public boolean isDeviceSecure(Context context) {
        int debugValue = SystemProperties.getInt("keyguard.secured.debug", -1);
        if (debugValue < 0) {
            return true;
        }
        if (debugValue < 2) {
            return debugValue == 1;
        }
        return mBase.isDeviceSecure(context.getUserId());
    }
}
