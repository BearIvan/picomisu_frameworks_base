// Copyright 2026 Picomisu contributors
// SPDX-License-Identifier: Apache-2.0

package android.util;

/**
 * Smartisan system log buffers. Reconstructed from the PICO OS 5.13.7 factory framework; only
 * the members reached by the ported factory code are present.
 *
 * @hide
 */
public final class SmtSysLog {
    public static final int LOG_ID_SYSFATAL = 9;

    public static int fatal(String tag, String msg) {
        return Log.println_native(LOG_ID_SYSFATAL, Log.ERROR, tag, msg);
    }
}
