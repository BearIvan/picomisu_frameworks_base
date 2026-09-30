// Copyright 2026 Picomisu contributors
// SPDX-License-Identifier: Apache-2.0

package smartisanos.util;

import android.util.Slog;
import android.util.SmtSysLog;

/**
 * Smartisan feature log. Reconstructed from the PICO OS 5.13.7 factory framework; only the
 * members reached by the ported factory code are present.
 *
 * @hide
 */
public class FeatLog {
    private static final int LOG_STR_DEFAULT_CAP = 64;

    public static void d(String tag, String featTag, int order, String msg) {
        StringBuilder sb = new StringBuilder(LOG_STR_DEFAULT_CAP);
        sb.append("[").append(featTag).append("][").append(order).append("] ").append(msg);
        Slog.d(tag, sb.toString());
    }

    public static void i(String tag, String featTag, int order, String msg) {
        StringBuilder sb = new StringBuilder(LOG_STR_DEFAULT_CAP);
        sb.append("[").append(featTag).append("][").append(order).append("] ").append(msg);
        Slog.i(tag, sb.toString());
    }

    public static void e(String tag, String featTag, int order, String msg) {
        StringBuilder sb = new StringBuilder(LOG_STR_DEFAULT_CAP);
        sb.append("[").append(featTag).append("][").append(order).append("] ").append(msg);
        Slog.e(tag, sb.toString());
        SmtSysLog.fatal(tag, sb.toString());
    }
}
