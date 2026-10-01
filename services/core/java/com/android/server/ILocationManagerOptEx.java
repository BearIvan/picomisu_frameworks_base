// Copyright 2026 Picomisu contributors
// SPDX-License-Identifier: Apache-2.0

package com.android.server;

import android.content.Context;
import android.os.DebugSmtEx;
import java.util.HashSet;

/**
 * Reconstructed from the PICO OS 5.13.7 factory services.
 *
 * @hide
 */
public interface ILocationManagerOptEx {
    default void init(LocationManagerService service) {
        DebugSmtEx.printDefaultFunInfo(getClass());
    }

    default void init(LocationManagerService service, Context context) {
        DebugSmtEx.printDefaultFunInfo(getClass());
    }

    default void updateAffectedProviders(int uid, boolean foreground, boolean frozen, HashSet<String> affectedProviders) {
        DebugSmtEx.printDefaultFunInfo(getClass());
    }

    default void onUidFrozenChangedLocked(int uid, boolean frozen) {
        DebugSmtEx.printDefaultFunInfo(getClass());
    }

    default boolean checkUidFreeze(int uid) {
        DebugSmtEx.printDefaultFunInfo(getClass());
        return false;
    }
}
