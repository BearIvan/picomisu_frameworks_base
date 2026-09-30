// Copyright 2026 Picomisu contributors
// SPDX-License-Identifier: Apache-2.0
// Reconstructed from the factory PICO OS 5.13.7 DEX by tools/reconstruct-pico-aidl.py.
package com.pvr;

import android.os.Bundle;

/** @hide */
interface ISysDataSyncService {
    oneway void onUsageEvent(in Bundle bundle);
    oneway void onAppDied(in Bundle bundle);
    oneway void onReportAid(String pkg, String aid);
    oneway void onTeaTrackerEvent(String event, String appid, String params);
    int queryProfile(String profileType, String packageName, String versionName, String versionCode, String filePath);
    boolean uploadProfile(String profileType, String packageName, String versionName, String versionCode, String filePath);
    oneway void onMetricEvent(String metricEvent, String params);
    oneway void onSlardarEvent(String event, String params);
    oneway void flushTeaTrackerEvents();
}
