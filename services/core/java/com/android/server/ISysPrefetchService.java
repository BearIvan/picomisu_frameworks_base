// Copyright 2026 Picomisu contributors
// SPDX-License-Identifier: Apache-2.0

package com.android.server;

import java.util.List;

/**
 * Reconstructed from the PICO OS 5.13.7 factory services.
 *
 * @hide
 */
public interface ISysPrefetchService {
    public static final boolean DEBUG_PREFETCH = true;
    public static final String PREFETCH_TAG = "smart_prefetch";

    default boolean isDoPrefetch() {
        return false;
    }

    default void setDoPrefetch(boolean prefetch) {
    }

    default void setPrefetchProcessMaxSize(int maxSize) {
    }

    default void notifyPrefetched(String packageName, int uid) {
    }

    default void notifyPrefetchSuccess(String packageName, int uid) {
    }

    default void notifyPrefetchKilled(String packageName, int uid) {
    }

    default void addSystemAppNoPrefetch(String packageName) {
    }

    default void addRecentPrefetch(String packageName) {
    }

    default void addAlivePrefetch(String packageName) {
    }

    default void startPrefetchApp() {
    }

    default void sendFreezeCurrentPrefetchMsg(int pid) {
    }

    default void sendPendingFreezePrefetchMsg() {
    }

    default void updatePrefetchApps(List<String> needPrefetchApps, int flag) {
    }

    default void removeAlivePrefetch(String packageName) {
    }
}
