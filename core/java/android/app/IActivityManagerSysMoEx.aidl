// Copyright 2026 Picomisu contributors
// SPDX-License-Identifier: Apache-2.0
// Reconstructed from the factory PICO OS 5.13.7 DEX by tools/reconstruct-pico-aidl.py.
package android.app;

import android.app.ISysClient;

/** @hide */
interface IActivityManagerSysMoEx {
    void registerSysClient(ISysClient client);
    long getRomFreeMemoryKb();
    String getSmtExtraInfo(int pid);
    boolean isForegroundProcess(String processName);
    String getPackageName(int pid);
}
