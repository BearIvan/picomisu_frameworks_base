// Copyright 2026 Picomisu contributors
// SPDX-License-Identifier: Apache-2.0

package android.app;

import android.os.ParcelFileDescriptor;
import android.os.RemoteException;

import smartisanos.util.FeatLog;

/**
 * Smartisan extension state of an {@link ActivityThread} (its {@code mSmtEx}). Reconstructed
 * from the PICO OS 5.13.7 factory framework; only the members reached by the Smartisan
 * {@link IApplicationThread} methods are present.
 *
 * @hide
 */
public class ActivityThreadSmtBase {
    public static final int COMPLETE_PREFETCH_BIND_APPLICATION = 1011;

    /** Bind data of a prefetched (pre-started) process, bound when it is really started. */
    ActivityThread.AppBindData mPrefetchData = null;
    protected ActivityThread mActivityThread;

    public ActivityThreadSmtBase(ActivityThread activityThread) {
        mActivityThread = activityThread;
    }

    /** Smartisan part of the {@link ActivityThread.ApplicationThread} binder. */
    public class ApplicationThreadEx {
        private ActivityThread.ApplicationThread mApplicationThread;

        public ApplicationThreadEx(ActivityThread.ApplicationThread applicationThread) {
            mApplicationThread = applicationThread;
        }

        public void scheduleMethodTrace(int type, long flags, String cmd,
                ParcelFileDescriptor fd) {
        }

        public void configArtTracer(String[] state) {
            SysFwBridge.getFactory().getArtTracerUtils().startArtTracer(state,
                    mActivityThread.mInitialApplication.getApplicationContext());
        }

        public final void completePrefetchBindApplication(long startSeq) {
            FeatLog.d("ActivityThread", "FEAT_PERF_PREFETCH", 30,
                    "c/s: execute complete prefetch bind application for process = "
                    + mPrefetchData.appInfo.packageName);
            mActivityThread.sendMessage(COMPLETE_PREFETCH_BIND_APPLICATION, startSeq);
            if (mPrefetchData != null) {
                final IActivityManager mgr = ActivityManager.getService();
                try {
                    mgr.attachApplication(mActivityThread.getApplicationThread(), startSeq);
                } catch (RemoteException ex) {
                    throw ex.rethrowFromSystemServer();
                }
            }
        }
    }
}
