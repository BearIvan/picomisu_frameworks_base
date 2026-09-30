// Copyright 2026 Picomisu contributors
// SPDX-License-Identifier: Apache-2.0

package com.pxr.net.util;

import android.os.SystemClock;

/**
 * Simple elapsed-realtime stopwatch used by the PICO network probes.
 *
 * @hide
 */
public class Stopwatch {
    private long mStartTimeMs;
    private long mStopTimeMs;

    public boolean isStarted() {
        return (mStartTimeMs > 0);
    }

    public boolean isStopped() {
        return (mStopTimeMs > 0);
    }

    public boolean isRunning() {
        return (isStarted() && !isStopped());
    }

    /**
     * Start the Stopwatch if it has not been started yet.
     */
    public Stopwatch start() {
        if (!isStarted()) {
            mStartTimeMs = SystemClock.elapsedRealtime();
        }
        return this;
    }

    /**
     * Stop the Stopwatch and return the elapsed time in milliseconds.
     */
    public long stop() {
        if (isRunning()) {
            mStopTimeMs = SystemClock.elapsedRealtime();
        }
        // Return either the delta after having stopped, or 0.
        return (mStopTimeMs - mStartTimeMs);
    }

    /**
     * Return the elapsed time since start, stopping the Stopwatch if it is not running.
     */
    public long lap() {
        if (isRunning()) {
            return (SystemClock.elapsedRealtime() - mStartTimeMs);
        } else {
            return stop();
        }
    }

    /**
     * Reset the Stopwatch. It will be stopped when this method returns.
     */
    public void reset() {
        mStartTimeMs = 0;
        mStopTimeMs = 0;
    }
}
