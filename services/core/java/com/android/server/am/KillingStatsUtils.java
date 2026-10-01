// Copyright 2026 Picomisu contributors
// SPDX-License-Identifier: Apache-2.0

package com.android.server.am;

import android.content.pm.PackageManagerInternal;
import com.android.server.LocalServices;

/**
 * Reconstructed from the PICO OS 5.13.7 factory services.
 *
 * @hide
 */
public class KillingStatsUtils {
    public static String getNameForUid(int uid) {
        PackageManagerInternal pm = (PackageManagerInternal) LocalServices.getService(PackageManagerInternal.class);
        String name = null;
        if (pm != null) {
            name = pm.getNameForUid(uid);
        }
        return name != null ? name : "unknown";
    }

    public static String buildAmKillingEventItem(String name, int uid, int adj, int procState, String reason) {
        return "{0|" + name + '|' + uid + '|' + adj + '|' + procState + '|' + reason + '}';
    }

    public static String buildLmkdKillingEventItem(String name, int uid, int adj, String reason) {
        return "{1|" + name + '|' + uid + '|' + adj + '|' + reason + '}';
    }

    public static String buildOtherKillingEventItem(String name, int uid, String reason) {
        return "{2|" + name + '|' + uid + '|' + reason + '}';
    }
}
