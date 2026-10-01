// Copyright 2026 Picomisu contributors
// SPDX-License-Identifier: Apache-2.0

package com.android.server.am;

import java.util.List;

/**
 * Reconstructed from the PICO OS 5.13.7 factory services.
 *
 * @hide
 */
public class PrefetchStatsUtils {
    public static String buildUpdatePrefetchEventItem(List<String> packageNames, int flag, long updateTime) {
        if (packageNames == null) {
            return "";
        }
        int size = packageNames.size();
        StringBuilder killEvent = new StringBuilder("{");
        killEvent.append(0);
        killEvent.append('|');
        killEvent.append(size);
        killEvent.append('|');
        killEvent.append(flag);
        killEvent.append('|');
        killEvent.append(updateTime);
        killEvent.append('|');
        for (int i = 0; i < size; i++) {
            if (i == size - 1) {
                killEvent.append(packageNames.get(i));
            } else {
                killEvent.append(packageNames.get(i));
                killEvent.append('|');
            }
        }
        killEvent.append('}');
        return killEvent.toString();
    }

    public static String buildStartPrefetchEventItem(String packageName, long startTime) {
        return "{1|" + packageName + '|' + startTime + '}';
    }

    public static String buildPrefetchStartedEventItem(String packageName, long frozenTime, int memToal, int memSwap) {
        return "{2|" + packageName + '|' + frozenTime + '|' + memToal + '|' + memSwap + '}';
    }

    public static String buildRealStartEventItem(String packageName, long realStartTime) {
        return "{3|" + packageName + '|' + realStartTime + '}';
    }

    public static String buildPrefetchKilledEventItem(String packageName, long killedTime, String killedReason) {
        return "{4|" + packageName + '|' + killedTime + '|' + killedReason + '}';
    }

    public static String buildPrefetchVersionErrorEventItem(String packageName, long currentTime, String versionName) {
        return "{5|" + packageName + '|' + currentTime + '|' + versionName + '}';
    }
}
