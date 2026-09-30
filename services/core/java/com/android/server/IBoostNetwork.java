// Copyright 2026 Picomisu contributors
// SPDX-License-Identifier: Apache-2.0

package com.android.server;

import android.content.Context;
import android.content.pm.ApplicationInfo;

/**
 * Smartisan network boost implemented by the optional multi-platform services
 * ({@link IMultiPlatSvsFactory#getBoostNetwork()}). Reconstructed from the PICO OS 5.13.7
 * factory services.
 *
 * @hide
 */
public interface IBoostNetwork {
    void boostNetworkInternal(ApplicationInfo info);

    void handleBoostNetwork();

    void init(Context context);
}
