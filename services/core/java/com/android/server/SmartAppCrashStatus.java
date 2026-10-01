// Copyright 2026 Picomisu contributors
// SPDX-License-Identifier: Apache-2.0

package com.android.server;

/**
 * Reconstructed from the PICO OS 5.13.7 factory services.
 *
 * @hide
 */
public class SmartAppCrashStatus extends UploadEvent {
    public String packageName;
    public int dailyCrashCount = 0;
    public int versionCrashCount = 0;
    public double dailyUsageTime = 0.0d;
    public double versionUsageTime = 0.0d;
    public double dailyCrashRate = 0.0d;
    public double versionCrashRate = 0.0d;
}
