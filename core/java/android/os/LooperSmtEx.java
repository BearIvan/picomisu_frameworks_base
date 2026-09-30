// Copyright 2026 Picomisu contributors
// SPDX-License-Identifier: Apache-2.0

package android.os;

import java.util.ArrayList;
import java.util.List;

/**
 * Smartisan extension of a {@link Looper} ({@link Looper#getSmtEx()}): temporary slow dispatch
 * monitoring of the main looper, requested through the application thread. Reconstructed from
 * the PICO OS 5.13.7 factory framework.
 *
 * @hide
 */
public class LooperSmtEx {
    static final long MONITOR_SLOW_OPTION_TIME_OUT = 20000;
    static boolean mSlowMonitor = false;

    long mLastSetSlowTime = 0;
    List<String> mSlowTimeOperation = new ArrayList<>();
    private Looper mLooper;

    public LooperSmtEx(Looper looper) {
        mLooper = looper;
    }

    void findSlowTimeOperation(long dispatchStart, long dispatchEnd,
            long slowDispatchThresholdMs, Message msg) {
        if (mSlowMonitor) {
            if (dispatchStart == 0) {
                dispatchStart = mLastSetSlowTime;
            }
            if (dispatchEnd - dispatchStart > slowDispatchThresholdMs) {
                mSlowTimeOperation.add(msg.target + "=" + msg.callback + "=" + msg.what);
            }
            if (dispatchEnd - mLastSetSlowTime > MONITOR_SLOW_OPTION_TIME_OUT) {
                setSlowDispatchThresholdMs(0);
                mSlowTimeOperation.clear();
                mSlowMonitor = false;
            }
        }
    }

    public List<String> getSlowOperations() {
        return mSlowTimeOperation;
    }

    public void setSlowDispatchThresholdMs(long slowDispatchThresholdMs) {
        mLooper.mSlowDispatchThresholdMs = slowDispatchThresholdMs;
        mLastSetSlowTime = SystemClock.uptimeMillis();
        mSlowMonitor = true;
    }
}
