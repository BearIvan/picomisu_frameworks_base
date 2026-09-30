// Copyright 2026 Picomisu contributors
// SPDX-License-Identifier: Apache-2.0

package android.os;

import com.android.internal.os.BatterySipper;
import com.android.internal.os.BatteryStatsHelper;

import java.io.PrintWriter;

/**
 * Smartisan battery stats extension (notification light, PC mode and external screen timers).
 * Reconstructed from the PICO OS 5.13.7 factory framework.
 *
 * @hide
 */
public abstract class BatteryStatsOptEx {
    public static final int STATE2_SCREEN_EXT_FLAG = 1 << 15;
    public static final int STATE2_NOTIFICATION_FLAG = 1 << 16;
    public static final int STATE2_PC_MODE_FLAG = 1 << 17;

    public abstract long getScreenExtOnTime(long elapsedRealtimeUs, int which);

    public abstract long getNotificationOnTime(long elapsedRealtimeUs, int which);

    public abstract long getPCModeOnTime(long elapsedRealtimeUs, int which);

    public void dumpOptLocked(PrintWriter pw, long rawRealtime, String prefix, StringBuilder sb,
            int which) {
        final long notificationLightTimeMs = getNotificationOnTime(rawRealtime, which) / 1000;
        sb.setLength(0);
        sb.append(prefix);
        sb.append("  Notification light time: ");
        BatteryStats.formatTimeMs(sb, notificationLightTimeMs);
        pw.println(sb.toString());
        pw.println();
    }

    public void dumpPowerUsage(BatterySipper bs, PrintWriter pw) {
        if (bs.audioPowerMah != 0) {
            pw.print(" audio=");
            pw.print(BatteryStatsHelper.makemAh(bs.audioPowerMah));
        }
        if (bs.videoPowerMah != 0) {
            pw.print(" video=");
            pw.print(BatteryStatsHelper.makemAh(bs.videoPowerMah));
        }
    }
}
