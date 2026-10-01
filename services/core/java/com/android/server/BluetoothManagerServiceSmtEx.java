// Copyright 2026 Picomisu contributors
// SPDX-License-Identifier: Apache-2.0

package com.android.server;

import android.app.SmtOpsManager;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.os.RemoteCallbackListSmtEx;
import android.os.RemoteException;
import android.util.Slog;

/**
 * Reconstructed from the PICO OS 5.13.7 factory services.
 *
 * @hide
 */
class BluetoothManagerServiceSmtEx {
    private static final int MESSAGE_APP_UNFREEZE_CALLBACK = 600;
    private static final String TAG = "BluetoothManagerServiceSmtEx";
    private BluetoothManagerService mBms;
    private final ApplicationFreezerInternalSmt.IFrozenCallback mFreezeCallback = new ApplicationFreezerInternalSmt.IFrozenCallback() {
        @Override
        public void onAppFreeze(int pid, int uid) {
            Slog.d(BluetoothManagerServiceSmtEx.TAG, "onAppFreeze pid: " + pid + ", uid: " + uid);
        }

        @Override
        public void onAppUnfreeze(int pid, int uid) {
            Slog.d(BluetoothManagerServiceSmtEx.TAG, "onAppUnfreeze pid: " + pid + ", uid: " + uid);
            Message unfreezeMsg = BluetoothManagerServiceSmtEx.this.mHandler.obtainMessage(
                    MESSAGE_APP_UNFREEZE_CALLBACK);
            unfreezeMsg.arg1 = pid;
            unfreezeMsg.arg2 = uid;
            BluetoothManagerServiceSmtEx.this.mHandler.sendMessage(unfreezeMsg);
        }
    };
    private Handler mHandler;

    public BluetoothManagerServiceSmtEx(BluetoothManagerService bms, Handler handler) {
        this.mBms = bms;
        this.mHandler = handler;
    }

    void registerBleCallback() {
    }

    void unregisterBleCallback() {
    }

    public void handleMessage(Message msg) {
        if (msg.what == MESSAGE_APP_UNFREEZE_CALLBACK) {
            Slog.d(TAG, "MESSAGE_APP_UNFREEZE_CALLBACK");
            int pid = msg.arg1;
            int uid = msg.arg2;
            sendStateOnAppUnfreezeCallback(pid, uid);
        }
    }

    void sendStateOnAppUnfreezeCallback(int pid, int uid) {
        try {
            int n = this.mBms.mCallbacks.beginBroadcast();
            for (int i = 0; i < n; i++) {
                if (RemoteCallbackListSmtEx.getRegisteredCallbackPid(this.mBms.mCallbacks, i) == pid
                        && RemoteCallbackListSmtEx.getRegisteredCallbackUid(this.mBms.mCallbacks, i)
                        == uid) {
                    int state = this.mBms.mState;
                    if (state == BluetoothAdapter.STATE_OFF) {
                        this.mBms.mCallbacks.getBroadcastItem(i).onBluetoothServiceDown();
                    } else if (state == BluetoothAdapter.STATE_ON) {
                        try {
                            this.mBms.mBluetoothLock.writeLock().lock();
                            if (this.mBms.mBluetooth != null) {
                                this.mBms.mCallbacks.getBroadcastItem(i)
                                        .onBluetoothServiceUp(this.mBms.mBluetooth);
                            }
                        } finally {
                            this.mBms.mBluetoothLock.writeLock().unlock();
                        }
                    }
                }
            }
        } catch (RemoteException e) {
            Slog.e(TAG, "Unable to call action when App Unfreeze ! ! !");
        } finally {
            this.mBms.mCallbacks.finishBroadcast();
        }
    }

    public void pendingBluetoothState(int pid, int uid) {
        ApplicationFreezerHelperSmt.registerFrozenCallbackByPidOnce(pid, uid, this.mFreezeCallback);
    }

    boolean hasSmtOp(Context context, int callingUid, String packageName) {
        SmtOpsManager smtOps = (SmtOpsManager) context.getSystemService("smtops");
        if (smtOps.noteOp(SmtOpsManager.OP_BLUETOOTH_CHANGE, callingUid, packageName)
                != SmtOpsManager.MODE_ALLOWED) {
            return false;
        }
        return true;
    }
}
