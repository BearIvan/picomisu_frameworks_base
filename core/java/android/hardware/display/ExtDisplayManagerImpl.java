// Copyright 2026 Picomisu contributors
// SPDX-License-Identifier: Apache-2.0
package android.hardware.display;

import android.content.Context;
import android.os.Binder;
import android.os.Process;
import android.os.SystemProperties;
import android.util.Log;

/**
 * PICO display manager extension (factory PICO OS 5.13.7
 * android.hardware.display.ExtDisplayManagerImpl).
 * @hide
 */
public class ExtDisplayManagerImpl implements IExtDisplayManager {
    private static final String TAG = "DisplayManager";
    private DisplayManager mBase;

    public ExtDisplayManagerImpl(DisplayManager base) {
        mBase = base;
    }

    /**
     * DisplayManager.registerDisplayListener: true (the listener is not registered) unless the
     * call comes from the main thread outside a binder call (calling pid equal to the thread
     * id), or pvr.display.listener.debug is false.
     */
    @Override
    public boolean registerDisplayListener(Context context) {
        int callingPid = Binder.getCallingPid();
        if (Process.myTid() == callingPid
                || !SystemProperties.getBoolean("pvr.display.listener.debug", true)) {
            return false;
        }
        if (SystemProperties.getBoolean("pvr.display.listener.debug", false)) {
            Log.v(TAG, "package:" + context.getPackageName() + " with callingPid=" + callingPid
                    + " currentPid=" + Process.myPid() + " currentTid=" + Process.myTid()
                    + " is register displayListener,ignore!");
        }
        return true;
    }
}
