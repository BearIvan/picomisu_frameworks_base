// Copyright 2026 Picomisu contributors
// SPDX-License-Identifier: Apache-2.0

package android.app;

import android.os.DebugSmtEx;
import android.os.ParcelFileDescriptor;

/**
 * Smartisan lab monitor cases (method tracing) of an application process, implemented by the
 * optional sysmonitor framework JAR ({@link SysMonitorFwBridge}). Reconstructed from the
 * PICO OS 5.13.7 factory framework; the default method is the factory behaviour when that JAR
 * is absent.
 *
 * @hide
 */
public interface ILabMonitorCase {
    default void scheduleMethodTrace(int type, long flags, String cmd, ParcelFileDescriptor fd) {
        DebugSmtEx.printDefaultFunInfo(getClass());
    }
}
