// Copyright 2026 Picomisu contributors
// SPDX-License-Identifier: Apache-2.0

package android.os;

import android.app.ActivityManager;
import android.app.ApplicationErrorReport;

/**
 * The target of a binder transaction is a frozen (Smartisan freeze) process. Reconstructed
 * from the PICO OS 5.13.7 factory framework; {@link #frozenObjectFromNative} is called by the
 * binder JNI when a transaction fails because the target process is frozen.
 *
 * @hide
 */
public class FrozenObjectException extends DeadObjectException {
    private static final String FROZEN_OBJECT_TAG = "FrozenObject";

    public FrozenObjectException() {
        super();
    }

    public FrozenObjectException(String message) {
        super(message);
    }

    private static void frozenObjectFromNative() {
        Throwable tr = new Throwable("target process is frozen");
        ApplicationErrorReport.ParcelableCrashInfo crashInfo =
                new ApplicationErrorReport.ParcelableCrashInfo(tr);
        ActivityManager.getSmtEx().frozenObjectFromNative(crashInfo);
    }
}
