// Copyright 2026 Picomisu contributors
// SPDX-License-Identifier: Apache-2.0

package android.os;

import java.util.concurrent.Executor;

/**
 * Shared Smartisan background thread "smt.worker" (factory PICO OS 5.13.7
 * {@code android.os.CommonWorkerThread}).
 *
 * @hide
 */
public final class CommonWorkerThread extends HandlerThread {
    private static final long SLOW_DISPATCH_THRESHOLD_MS = 10_000;
    private static final long SLOW_DELIVERY_THRESHOLD_MS = 30_000;
    private static CommonWorkerThread sInstance;
    private static Handler sHandler;
    private static HandlerExecutor sHandlerExecutor;

    private CommonWorkerThread() {
        super("smt.worker", android.os.Process.THREAD_PRIORITY_BACKGROUND);
    }

    private static void ensureThreadLocked() {
        if (sInstance == null) {
            sInstance = new CommonWorkerThread();
            sInstance.start();
            final Looper looper = sInstance.getLooper();
            looper.setTraceTag(Trace.TRACE_TAG_APP);
            looper.setSlowLogThresholdMs(
                    SLOW_DISPATCH_THRESHOLD_MS, SLOW_DELIVERY_THRESHOLD_MS);
            sHandler = new Handler(sInstance.getLooper());
            sHandlerExecutor = new HandlerExecutor(sHandler);
        }
    }

    public static CommonWorkerThread get() {
        if (sInstance == null) {
            synchronized (CommonWorkerThread.class) {
                ensureThreadLocked();
            }
        }
        return sInstance;
    }

    public static Handler getHandler() {
        if (sInstance == null) {
            synchronized (CommonWorkerThread.class) {
                ensureThreadLocked();
            }
        }
        return sHandler;
    }

    public static Executor getExecutor() {
        synchronized (CommonWorkerThread.class) {
            ensureThreadLocked();
            return sHandlerExecutor;
        }
    }
}
