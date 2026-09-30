// Copyright 2026 Picomisu contributors
// SPDX-License-Identifier: Apache-2.0
package com.pico.api.app;

import android.os.Parcel;

/**
 * Receives the transactions delivered to an {@link AppSession} binder.
 *
 * @hide
 */
public interface AppSessionCallback {
    /** Handles one transaction; returns {@code true} when it was consumed. */
    default boolean onTransact(int code, Parcel data, Parcel reply, int flags) {
        return false;
    }
}
