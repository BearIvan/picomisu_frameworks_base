// Copyright 2026 Picomisu contributors
// SPDX-License-Identifier: Apache-2.0

package com.android.server.am;

/**
 * Reconstructed from the PICO OS 5.13.7 factory services.
 *
 * @hide
 */
public interface IActiveUidsOptEx extends IUidCallback {
    default void registerCallback(IUidCallback callback) {
    }

    default void unregisterCallback(IUidCallback callback) {
    }
}
