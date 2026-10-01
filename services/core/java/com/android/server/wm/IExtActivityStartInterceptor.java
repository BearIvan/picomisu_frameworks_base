// Copyright 2026 Picomisu contributors
// SPDX-License-Identifier: Apache-2.0
package com.android.server.wm;

import android.content.Context;

import com.pico.util.IExtBase;

/**
 * PICO activity start interceptor extension (factory PICO OS 5.13.7
 * com.android.server.wm.IExtActivityStartInterceptor).
 * @hide
 */
public interface IExtActivityStartInterceptor extends IExtBase {
    boolean intercept(Context context);
}
