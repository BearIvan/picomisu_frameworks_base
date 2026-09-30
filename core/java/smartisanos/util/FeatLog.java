// Copyright 2026 Picomisu contributors
// SPDX-License-Identifier: Apache-2.0

package smartisanos.util;

import android.os.SystemProperties;
import android.util.Slog;
import android.util.SmtSysLog;

/**
 * Smartisan feature log. Reconstructed from the PICO OS 5.13.7 factory framework.
 *
 * @hide
 */
public class FeatLog {
    private static final int LOG_STR_DEFAULT_CAP = 64;
    private static final String FEAT_LOG_PROP = "persist.sys.feature.log";

    public static final boolean LEVEL_1 = false;
    // Not a compile-time constant in the factory (the preserved JARs read it with sget): it is
    // assigned false in <clinit>. The right operand is never evaluated (javac emits iconst_0).
    public static final boolean LEVEL_2 = LEVEL_1
            && SystemProperties.getBoolean(FEAT_LOG_PROP, false);

    public static void d(String tag, String featTag, int order, String msg) {
        StringBuilder sb = new StringBuilder(LOG_STR_DEFAULT_CAP);
        sb.append("[").append(featTag).append("][").append(order).append("] ").append(msg);
        Slog.d(tag, sb.toString());
    }

    public static void d(String tag, String featTag, int order, String msg, Throwable t) {
        StringBuilder sb = new StringBuilder(LOG_STR_DEFAULT_CAP);
        sb.append("[").append(featTag).append("][").append(order).append("] ").append(msg);
        Slog.d(tag, sb.toString(), t);
    }

    public static void i(String tag, String featTag, int order, String msg) {
        StringBuilder sb = new StringBuilder(LOG_STR_DEFAULT_CAP);
        sb.append("[").append(featTag).append("][").append(order).append("] ").append(msg);
        Slog.i(tag, sb.toString());
    }

    public static void i(String tag, String featTag, int order, String msg, Throwable t) {
        StringBuilder sb = new StringBuilder(LOG_STR_DEFAULT_CAP);
        sb.append("[").append(featTag).append("][").append(order).append("] ").append(msg);
        Slog.i(tag, sb.toString(), t);
    }

    public static void w(String tag, String featTag, int order, String msg) {
        StringBuilder sb = new StringBuilder(LOG_STR_DEFAULT_CAP);
        sb.append("[").append(featTag).append("][").append(order).append("] ").append(msg);
        Slog.w(tag, sb.toString());
    }

    public static void w(String tag, String featTag, int order, String msg, Throwable t) {
        StringBuilder sb = new StringBuilder(LOG_STR_DEFAULT_CAP);
        sb.append("[").append(featTag).append("][").append(order).append("] ").append(msg);
        Slog.w(tag, sb.toString(), t);
    }

    public static void e(String tag, String featTag, int order, String msg) {
        StringBuilder sb = new StringBuilder(LOG_STR_DEFAULT_CAP);
        sb.append("[").append(featTag).append("][").append(order).append("] ").append(msg);
        Slog.e(tag, sb.toString());
        SmtSysLog.fatal(tag, sb.toString());
    }

    public static void e(String tag, String featTag, int order, String msg, Throwable tr) {
        StringBuilder sb = new StringBuilder(LOG_STR_DEFAULT_CAP);
        sb.append("[").append(featTag).append("][").append(order).append("] ").append(msg);
        Slog.e(tag, sb.toString(), tr);
        SmtSysLog.fatal(tag, sb.toString(), tr);
    }

    public static void config(boolean enable) {
        try {
            SystemProperties.set(FEAT_LOG_PROP, String.valueOf(enable));
        } catch (Exception e) {
            Slog.e("FeatLog", "config feature log failed!", e);
        }
    }
}
