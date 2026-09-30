// Copyright 2026 Picomisu contributors
// SPDX-License-Identifier: Apache-2.0
// Reconstructed from the factory PICO OS 5.13.7 DEX by tools/reconstruct-pico-aidl.py.
package android.app;

import android.app.AppInfoItem;

/** @hide */
interface IActivityLifeCycleObserver {
    oneway void onActivityStartForeground(in AppInfoItem item);
    oneway void onActivityResumeForeground(in AppInfoItem item);
    oneway void onActivityPauseBackground(in AppInfoItem item);
    oneway void onActivityStopBackground(in AppInfoItem item);
    oneway void onActivityDestroy(in AppInfoItem item);
}
