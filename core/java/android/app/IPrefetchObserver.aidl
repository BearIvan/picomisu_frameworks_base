// Copyright 2026 Picomisu contributors
// SPDX-License-Identifier: Apache-2.0
// Reconstructed from the factory PICO OS 5.13.7 DEX by tools/reconstruct-pico-aidl.py.
package android.app;

import android.app.PrefetchInfo;

/** @hide */
interface IPrefetchObserver {
    oneway void onPrefetchKilled(in PrefetchInfo item);
}
