// Copyright 2026 Picomisu contributors
// SPDX-License-Identifier: Apache-2.0
package com.android.server.am;

import android.content.Intent;

import com.pico.util.IExtBase;

/**
 * PICO service extension (factory PICO OS 5.13.7 com.android.server.am.IExtActiveServices).
 * @hide
 */
public interface IExtActiveServices extends IExtBase {
    String SYS_PXR_START_BOOTCOMPLETED = "persist.pvr.startbootcompleted";

    boolean disableStartSyncAdapter(ServiceRecord r, ProcessRecord app);

    void onBindServiceLocked(Intent service);
}
