// Copyright 2026 Picomisu contributors
// SPDX-License-Identifier: Apache-2.0
// Reconstructed from the factory PICO OS 5.13.7 DEX by tools/reconstruct-pico-aidl.py.
package android.app;

import android.app.IPrefetchCallback;
import android.app.IPrefetchObserver;

/** @hide */
interface IPrefetchManager {
    void registerStartPrefetchListener(IPrefetchCallback localCallBack);
    void registerPrefetchObserver(IPrefetchObserver observer);
    void unregisterPrefetchObserver(IPrefetchObserver observer);
    boolean isPrefetch(String packageName);
}
