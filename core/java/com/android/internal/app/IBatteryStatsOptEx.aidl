// Copyright 2026 Picomisu contributors
// SPDX-License-Identifier: Apache-2.0
// Reconstructed from the factory PICO OS 5.13.7 DEX by tools/reconstruct-pico-aidl.py.
package com.android.internal.app;

/** @hide */
interface IBatteryStatsOptEx {
    void notePCModeOn();
    void notePCModeOff();
    void noteResetPCMode();
    void noteNotificationOn();
    void noteNotificationOff();
    void noteScreenExtState(boolean ScreenExtOn);
    void syncStateIfNeed();
}
