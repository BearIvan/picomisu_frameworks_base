// Copyright 2026 Picomisu contributors
// SPDX-License-Identifier: Apache-2.0

package com.android.server.wm;

/**
 * Smartisan extension of the {@link ActivityTaskManagerInternal}, returned by
 * {@code ActivityTaskManagerInternal.getSmtEx()}. Reconstructed from the PICO OS 5.13.7 factory
 * services.
 *
 * @hide
 */
public class ActivityTaskManagerInternalSmtBase {
    protected ActivityTaskManagerService mAtmServices;
    public ActivityTaskManagerServiceSmtBase mAtmsmt = null;

    public ActivityTaskManagerInternalSmtBase(ActivityTaskManagerService atmServices) {
        mAtmServices = atmServices;
    }

    public WindowProcessController getPreviousVrProcess() {
        if (mAtmsmt == null) {
            mAtmsmt = mAtmServices.getSmtEx();
        }
        if (mAtmsmt == null) {
            return null;
        }
        return mAtmsmt.getPreviousVrProcess();
    }

    public void onPrefetchProcessAdded(WindowProcessController proc) {
        synchronized (mAtmServices.mGlobalLockWithoutBoost) {
            mAtmServices.getSmtEx().mPrefetchProcessNames.put(proc.mName, proc.mUid, proc);
        }
    }

    public void onPrefetchProcessRemoved(String name, int uid) {
        synchronized (mAtmServices.mGlobalLockWithoutBoost) {
            mAtmServices.getSmtEx().mPrefetchProcessNames.remove(name, uid);
        }
    }
}
