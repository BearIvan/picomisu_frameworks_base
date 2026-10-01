// Copyright 2026 Picomisu contributors
// SPDX-License-Identifier: Apache-2.0

package com.android.server;

import com.android.server.am.ProcessRecord;

/**
 * Reconstructed from the PICO OS 5.13.7 factory services.
 *
 * @hide
 */
public interface ApplicationFreezerInternalSmt {
    boolean registerFrozenCallback(IFrozenCallback iFrozenCallback, boolean z);

    boolean registerFrozenCallbackByPidOnce(int i, int i2, IFrozenCallback iFrozenCallback);

    boolean unregisterFrozenCallbackByPidOnce(int i, int i2, IFrozenCallback iFrozenCallback);

    public interface IFrozenCallback {
        default void onAppFreeze(int pid, int uid) {
        }

        default void onAppUnfreeze(int pid, int uid) {
        }

        default void onAppFreeze(ProcessRecord proc) {
        }

        default void onAppUnfreeze(ProcessRecord proc) {
        }
    }
}
