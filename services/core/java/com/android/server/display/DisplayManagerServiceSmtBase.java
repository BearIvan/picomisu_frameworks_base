// Copyright 2026 Picomisu contributors
// SPDX-License-Identifier: Apache-2.0

package com.android.server.display;

import android.hardware.display.IDisplayManagerCallback;
import android.os.RemoteException;
import android.util.Slog;
import android.util.SparseArray;
import com.android.server.ApplicationFreezerHelperSmt;
import com.android.server.ApplicationFreezerInternalSmt;
import com.android.server.am.ActivityManagerDebugConfigSmtEx;

/**
 * Reconstructed from the PICO OS 5.13.7 factory services.
 *
 * @hide
 */
public abstract class DisplayManagerServiceSmtBase {
    private static final String TAG = "DisplayManagerServiceSmtEx";
    protected DisplayManagerService mService;

    public DisplayManagerServiceSmtBase(DisplayManagerService service) {
        this.mService = service;
    }

    static final class CallbackRecordSmtEx implements ApplicationFreezerInternalSmt.IFrozenCallback {
        SparseArray<Integer> mAddEventDisplayIds;
        IDisplayManagerCallback mCallback;
        SparseArray<Integer> mPendingEvents;
        public final int mUid;

        CallbackRecordSmtEx(int uid, IDisplayManagerCallback callback) {
            this.mUid = uid;
            this.mCallback = callback;
        }

        @Override
        public void onAppUnfreeze(int pid, int uid) {
            SparseArray<Integer> sparseArray = this.mPendingEvents;
            if (sparseArray != null) {
                synchronized (sparseArray) {
                    int N = this.mPendingEvents.size();
                    for (int i = 0; i < N; i++) {
                        int dis = this.mPendingEvents.keyAt(i);
                        int event = this.mPendingEvents.valueAt(i).intValue();
                        try {
                            this.mCallback.onDisplayEvent(dis, event);
                            if (ActivityManagerDebugConfigSmtEx.DEBUG_FREEZE) {
                                Slog.i(DisplayManagerServiceSmtBase.TAG, "send pending event, dis: " + dis + ", event: " + event + ", to pid: " + pid + ", uid: " + uid);
                            }
                        } catch (RemoteException e) {
                            Slog.w(DisplayManagerServiceSmtBase.TAG, "Failed to send pending event dis: " + dis + ", event: " + event + ", to pid: " + pid + ", uid: " + uid);
                        }
                    }
                    this.mPendingEvents.clear();
                    this.mAddEventDisplayIds.clear();
                }
            }
        }

        @Override
        public void onAppFreeze(int pid, int uid) {
        }

        public void notifyDisplayPendingEvent(int pid, int displayId, int event) {
            if (this.mPendingEvents == null) {
                this.mPendingEvents = new SparseArray<>();
            }
            if (this.mAddEventDisplayIds == null) {
                this.mAddEventDisplayIds = new SparseArray<>();
            }
            synchronized (this.mPendingEvents) {
                if (this.mPendingEvents.size() == 0) {
                    ApplicationFreezerHelperSmt.registerFrozenCallbackByPidOnce(pid, this.mUid, this);
                } else if (event == 3 && this.mAddEventDisplayIds.get(displayId) != null && this.mPendingEvents.get(displayId) != null) {
                    this.mPendingEvents.remove(displayId);
                    this.mAddEventDisplayIds.remove(displayId);
                    return;
                }
                if (event == 1) {
                    this.mAddEventDisplayIds.put(displayId, 0);
                }
                this.mPendingEvents.put(displayId, Integer.valueOf(event));
            }
        }
    }
}
