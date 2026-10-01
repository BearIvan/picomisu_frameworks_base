// Copyright 2026 Picomisu contributors
// SPDX-License-Identifier: Apache-2.0
package com.android.server.am;

import android.content.pm.ApplicationInfo;
import android.content.pm.ResolveInfo;
import android.os.SystemProperties;
import android.util.Slog;

/**
 * PICO broadcast extension (factory PICO OS 5.13.7 com.android.server.am.ExtBroadcastQueueImpl):
 * with persist.pvr.startbootcompleted=1, BOOT_COMPLETED, MEDIA_MOUNTED, MEDIA_BUTTON and
 * bluetooth STATE_CHANGED are not delivered to a manifest receiver of a non-system app whose
 * process is not running.
 * @hide
 */
public class ExtBroadcastQueueImpl implements IExtBroadcastQueue {
    public static final String SYS_PXR_START_BOOTCOMPLETED = "persist.pvr.startbootcompleted";
    private static final String TAG = "BroadcastQueue";
    private BroadcastQueue mBase;

    public ExtBroadcastQueueImpl(BroadcastQueue base) {
        mBase = base;
    }

    @Override
    public boolean skipProcessNextBroadcast(boolean skip, ProcessRecord app, ResolveInfo info,
            BroadcastRecord r, int receiverUid) {
        boolean startBootCompleted =
                SystemProperties.getInt(SYS_PXR_START_BOOTCOMPLETED, -1) == 1;
        ApplicationInfo apps = info.activityInfo.applicationInfo;
        if (!skip && startBootCompleted && (apps.flags & ApplicationInfo.FLAG_SYSTEM) <= 0
                && app == null) {
            if ("android.intent.action.BOOT_COMPLETED".equals(r.intent.getAction())
                    || "android.intent.action.MEDIA_MOUNTED".equals(r.intent.getAction())
                    || "android.intent.action.MEDIA_BUTTON".equals(r.intent.getAction())
                    || "android.bluetooth.adapter.action.STATE_CHANGED".equals(
                            r.intent.getAction())) {
                Slog.w(TAG, "broadcasting : " + r.intent.toString() + "is not allowed :  "
                        + info.activityInfo.packageName + " (pid=" + r.callingPid + ", uid="
                        + receiverUid + ") received .");
                return true;
            }
        }
        return skip;
    }
}
