// Copyright 2026 Picomisu contributors
// SPDX-License-Identifier: Apache-2.0

package android.app;

import android.util.Slog;

import java.util.ArrayList;
import java.util.List;

/**
 * Per-process registry of callbacks run when a Smartisan prefetched (pre-started) process is
 * really started ({@link IApplicationThread#onPrefetchRealStart}). Reconstructed from the
 * PICO OS 5.13.7 factory framework.
 *
 * @hide
 */
public class PrefetchRegister {
    private static final String TAG = "PrefetchRegister";
    private static final String PREFETCH_SERVICE = "prefetch";
    private static PrefetchRegister sInstance;
    private static Object mLock = new Object();
    private List<PrefetchCallbackLocal> callbacks = new ArrayList<>();

    /** Callback run once when the prefetched process is really started. */
    public interface PrefetchCallbackLocal {
        void onRealStart(int pid);
    }

    public static PrefetchRegister getInstance() {
        if (sInstance != null) {
            return sInstance;
        }
        synchronized (mLock) {
            if (sInstance == null) {
                sInstance = new PrefetchRegister();
            }
            return sInstance;
        }
    }

    public void registerStartPrefetchListener(int pid, PrefetchCallbackLocal callback) {
        synchronized (callbacks) {
            callbacks.add(callback);
        }
    }

    public void onRealStart(int pid) {
        synchronized (callbacks) {
            Slog.i(TAG, "prefetch real start pid = " + pid);
            for (PrefetchCallbackLocal callback : callbacks) {
                callback.onRealStart(pid);
            }
            callbacks.clear();
        }
    }
}
