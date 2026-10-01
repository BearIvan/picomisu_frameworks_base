// Copyright 2026 Picomisu contributors
// SPDX-License-Identifier: Apache-2.0

package com.android.server;

/**
 * Reconstructed from the PICO OS 5.13.7 factory services.
 *
 * @hide
 */
public interface IGameBalanceService {
    default void setGameBalanceOpen(boolean open) {
    }

    default boolean isLastGameBalance() {
        return false;
    }

    default void setLastGameBalance(boolean lastGameBalance) {
    }

    default int getGameBalanceFlag() {
        return 0;
    }

    default boolean isGameBalanceMode() {
        return false;
    }

    default int getCurrentGameUid() {
        return 0;
    }

    default void notifySFGameBalanceChanged(boolean gameBalance) {
    }

    default void setDebugGameBalance(boolean debug) {
    }
}
