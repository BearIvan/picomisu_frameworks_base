// Copyright 2026 Picomisu contributors
// SPDX-License-Identifier: Apache-2.0
package com.android.server;

import android.util.NtpTrustedTime;

import com.pico.util.IExtBase;

/**
 * PICO NewNetworkTimeUpdateService extension (factory PICO OS 5.13.7
 * com.android.server.IExtNewNetworkTimeUpdateService).
 */
public interface IExtNewNetworkTimeUpdateService extends IExtBase {
    void syncTimeFromServer(NtpTrustedTime time, int tryAgainCounter);
}
