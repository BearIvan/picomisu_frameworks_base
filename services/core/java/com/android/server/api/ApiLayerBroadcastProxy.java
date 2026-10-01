/*
 * Copyright 2026 Picomisu contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package com.android.server.api;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Parcel;
import android.os.RemoteCallbackList;
import android.text.TextUtils;
import android.util.Slog;

import com.pico.api.app.IAppSession;

import java.util.ArrayList;
import java.util.List;

/**
 * Broadcast receivers registered in system_server on behalf of native API layer clients
 * (factory PICO OS 5.13.7 com.android.server.api.ApiLayerBroadcastProxy). One receiver per
 * action is shared by all sessions; each broadcast is forwarded to the sessions as a oneway
 * {@link MsgDispatcher#CODE_DISPATCH_BROADCAST_FOR_NATIVE} transaction.
 *
 * @hide
 */
public class ApiLayerBroadcastProxy {
    private static final List<BroadcastReceiverRecord> sBroadcastReceiverRecordList =
            new ArrayList<>();
    private static final RemoteCallbackList<IAppSession> sClientBinderList =
            new RemoteCallbackList<IAppSession>() {
                @Override
                public void onCallbackDied(IAppSession callback) {
                    synchronized (sClientBinderList) {
                        for (BroadcastReceiverRecord record
                                : queryBroadcastReceiverRecordByBinder(callback.asBinder())) {
                            record.removeSession(callback);
                            if (record.getSessionCount() == 0) {
                                record.unregisterBroadcastReceiver();
                                sBroadcastReceiverRecordList.remove(record);
                            }
                        }
                    }
                }
            };

    private class BroadcastReceiverRecord {
        private final String mAction;
        private final List<IBinder> mClientList = new ArrayList<>();
        private final Context mContext;
        private final String mDataScheme;
        private final Handler mHandler;
        private final BroadcastReceiver mReceiver;

        BroadcastReceiverRecord(Context context, String action, String dataScheme,
                Handler handler) {
            mContext = context;
            mAction = action;
            mDataScheme = dataScheme;
            mHandler = handler;
            mReceiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    final Uri data = intent.getData();
                    onReceiveData(data != null ? data.getEncodedSchemeSpecificPart() : null,
                            intent.getExtras());
                }
            };
        }

        boolean isMatch(String action) {
            return action != null && action.equals(mAction);
        }

        boolean isMatch(IBinder binder) {
            return mClientList.contains(binder);
        }

        boolean addSession(IAppSession session) {
            final IBinder binder = session.asBinder();
            if (mClientList.contains(binder)) {
                return false;
            }
            mClientList.add(binder);
            return true;
        }

        boolean removeSession(IAppSession session) {
            final IBinder binder = session.asBinder();
            if (!mClientList.contains(binder)) {
                return false;
            }
            mClientList.remove(binder);
            return true;
        }

        int getSessionCount() {
            return mClientList.size();
        }

        private void onReceiveData(String schemeSpecificPart, Bundle extras) {
            synchronized (sClientBinderList) {
                for (IBinder binder : mClientList) {
                    sendDataToClient(binder, schemeSpecificPart, extras);
                }
            }
        }

        private void sendDataToClient(IBinder binder, String schemeSpecificPart, Bundle extras) {
            final Parcel data = Parcel.obtain();
            final Parcel reply = Parcel.obtain();
            try {
                data.writeInterfaceToken(MsgDispatcher.APP_SESSION_DESCRIPTOR);
                data.writeString(mAction);
                if (extras == null) {
                    data.writeInt(0);
                } else {
                    data.writeInt(1);
                    extras.writeToParcel(data, 0);
                }
                if (schemeSpecificPart == null) {
                    data.writeInt(0);
                } else {
                    data.writeInt(1);
                    data.writeString(schemeSpecificPart);
                }
                binder.transact(MsgDispatcher.CODE_DISPATCH_BROADCAST_FOR_NATIVE, data, reply,
                        IBinder.FLAG_ONEWAY);
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                data.recycle();
                reply.recycle();
            }
        }

        void registerBroadcastReceiver() {
            final IntentFilter filter = new IntentFilter();
            filter.addAction(mAction);
            if (!TextUtils.isEmpty(mDataScheme)) {
                filter.addDataScheme(mDataScheme);
            }
            mContext.registerReceiver(mReceiver, filter, null, mHandler);
        }

        void unregisterBroadcastReceiver() {
            mContext.unregisterReceiver(mReceiver);
        }
    }

    private static List<BroadcastReceiverRecord> queryBroadcastReceiverRecordByBinder(
            IBinder binder) {
        final List<BroadcastReceiverRecord> records = new ArrayList<>();
        for (BroadcastReceiverRecord record : sBroadcastReceiverRecordList) {
            if (record.isMatch(binder)) {
                records.add(record);
            }
        }
        return records;
    }

    private static BroadcastReceiverRecord getBroadcastReceiverRecord(String action) {
        for (BroadcastReceiverRecord record : sBroadcastReceiverRecordList) {
            if (record.isMatch(action)) {
                return record;
            }
        }
        return null;
    }

    public void registerBroadcastReceiver(Context context, IAppSession session, String action,
            String dataScheme, Handler handler) {
        if (context == null || session == null || action == null) {
            Slog.w(ApiLayerService.TAG, "ignore registerBroadcastReceiver, context [" + context
                    + "], session [" + session + "], action [" + action + "]");
            return;
        }
        Slog.w(ApiLayerService.TAG, "registerBroadcastReceiver action [" + action
                + "], dataScheme [" + dataScheme + "]");
        synchronized (sClientBinderList) {
            BroadcastReceiverRecord record = getBroadcastReceiverRecord(action);
            if (record == null) {
                record = new BroadcastReceiverRecord(context, action, dataScheme, handler);
                record.registerBroadcastReceiver();
                sBroadcastReceiverRecordList.add(record);
                final List<BroadcastReceiverRecord> list =
                        queryBroadcastReceiverRecordByBinder(session.asBinder());
                if (list.size() == 0) {
                    sClientBinderList.register(session);
                }
            }
            record.addSession(session);
        }
    }

    public void unregisterBroadcastReceiver(Context context, IAppSession session,
            String action) {
        if (context == null || session == null || action == null) {
            return;
        }
        Slog.w(ApiLayerService.TAG, "unregisterBroadcastReceiver action [" + action + "]");
        synchronized (sClientBinderList) {
            final BroadcastReceiverRecord record = getBroadcastReceiverRecord(action);
            if (record == null) {
                return;
            }
            if (record.removeSession(session)) {
                if (record.getSessionCount() == 0) {
                    record.unregisterBroadcastReceiver();
                    sBroadcastReceiverRecordList.remove(record);
                }
                final List<BroadcastReceiverRecord> list =
                        queryBroadcastReceiverRecordByBinder(session.asBinder());
                if (list.size() == 0) {
                    sClientBinderList.unregister(session);
                }
            }
        }
    }
}
