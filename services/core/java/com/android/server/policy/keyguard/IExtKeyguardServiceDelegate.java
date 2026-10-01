// Copyright 2026 Picomisu contributors
// SPDX-License-Identifier: Apache-2.0
package com.android.server.policy.keyguard;

import android.content.ComponentName;
import android.content.res.Resources;
import android.os.Handler;

import com.pico.util.IExtBase;

/**
 * PICO keyguard service delegate extension (factory PICO OS 5.13.7
 * com.android.server.policy.keyguard.IExtKeyguardServiceDelegate).
 * @hide
 */
public interface IExtKeyguardServiceDelegate extends IExtBase {
    ComponentName updateKeyguardStatus(Handler handler, Resources resources,
            ComponentName keyguardComponent);
}
