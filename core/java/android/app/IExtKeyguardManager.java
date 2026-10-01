// Copyright 2026 Picomisu contributors
// SPDX-License-Identifier: Apache-2.0
package android.app;

import android.content.Context;
import android.view.IWindowManager;

import com.pico.util.IExtBase;

/**
 * PICO KeyguardManager extension (factory PICO OS 5.13.7 android.app.IExtKeyguardManager).
 * @hide
 */
public interface IExtKeyguardManager extends IExtBase {
    boolean hasVrKeyguard();

    boolean isDeviceSecure(Context context);

    boolean isKeyguardSecure(IWindowManager wm, Context context);
}
