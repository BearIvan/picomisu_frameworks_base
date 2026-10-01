// Copyright 2026 Picomisu contributors
// SPDX-License-Identifier: Apache-2.0
package com.android.server.am;

import android.content.pm.ResolveInfo;

import com.pico.util.IExtBase;

/**
 * PICO broadcast extension (factory PICO OS 5.13.7 com.android.server.am.IExtBroadcastQueue).
 * @hide
 */
public interface IExtBroadcastQueue extends IExtBase {
    String SYS_PXR_START_BOOTCOMPLETED = "persist.pvr.startbootcompleted";

    boolean skipProcessNextBroadcast(boolean skip, ProcessRecord app, ResolveInfo info,
            BroadcastRecord r, int receiverUid);
}
