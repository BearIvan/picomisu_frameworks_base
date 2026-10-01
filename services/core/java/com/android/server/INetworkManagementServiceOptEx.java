// Copyright 2026 Picomisu contributors
// SPDX-License-Identifier: Apache-2.0

package com.android.server;

import android.content.Context;
import android.os.DebugSmtEx;

/**
 * Reconstructed from the PICO OS 5.13.7 factory services.
 *
 * @hide
 */
public interface INetworkManagementServiceOptEx {
    default void init(Context context, NetworkManagementService managementService) {
        DebugSmtEx.printDefaultFunInfo(getClass());
    }

    default void updateUidNetworkAccessPreviledge() {
        DebugSmtEx.printDefaultFunInfo(getClass());
    }
}
