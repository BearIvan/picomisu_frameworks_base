/*
 * Copyright 2026 Picomisu contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package com.android.server.api;

import android.content.Context;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Handler;
import android.os.IBinder;
import android.os.Parcel;
import android.os.RemoteCallbackList;
import android.provider.Settings;
import android.util.Slog;

import com.pico.api.app.IAppSession;

import java.util.ArrayList;
import java.util.List;

/**
 * Settings observers registered in system_server on behalf of native API layer clients
 * (factory PICO OS 5.13.7 com.android.server.api.ApiLayerSettingsObserverProxy). One observer
 * per (setting type, name) is shared by all sessions; each change is forwarded as a oneway
 * {@link MsgDispatcher#CODE_DISPATCH_SETTINGS_VALUE_FOR_NATIVE} transaction.
 *
 * @hide
 */
public class ApiLayerSettingsObserverProxy {
    static final int SETTINGS_TYPE_GLOBAL = 1;
    static final int SETTINGS_TYPE_SYSTEM = 2;
    static final int SETTINGS_TYPE_SECURE = 3;
    static final int SETTINGS_VALUE_TYPE_INT = 1;
    static final int SETTINGS_VALUE_TYPE_STRING = 2;

    private static final List<SettingsObserverRecord> sSettingsObserverRecordList =
            new ArrayList<>();
    private static final RemoteCallbackList<IAppSession> sClientBinderList =
            new RemoteCallbackList<IAppSession>() {
                @Override
                public void onCallbackDied(IAppSession callback) {
                    synchronized (sClientBinderList) {
                        for (SettingsObserverRecord record
                                : querySettingsObserverRecordByBinder(callback.asBinder())) {
                            record.removeSession(callback);
                            if (record.getSessionCount() == 0) {
                                record.unregisterSettingsObserver();
                                sSettingsObserverRecordList.remove(record);
                            }
                        }
                    }
                }
            };

    private class SettingsObserverRecord {
        private final ContentObserver mContentObserver;
        private final Context mContext;
        private final Handler mHandler;
        private final String mName;
        private final List<IBinder> mSessionList = new ArrayList<>();
        private final int mSettingType;
        private Uri mUri;
        private final int mValueType;

        SettingsObserverRecord(Context context, String name, int settingType, int valueType,
                Handler handler) {
            mContext = context;
            mName = name;
            mSettingType = settingType;
            mValueType = valueType;
            mHandler = handler;
            if (settingType == SETTINGS_TYPE_GLOBAL) {
                mUri = Settings.Global.getUriFor(name);
            } else if (settingType == SETTINGS_TYPE_SYSTEM) {
                mUri = Settings.System.getUriFor(name);
            } else if (settingType == SETTINGS_TYPE_SECURE) {
                mUri = Settings.Secure.getUriFor(name);
            }
            mContentObserver = new ContentObserver(mHandler) {
                @Override
                public void onChange(boolean selfChange, Uri uri) {
                    onSettingsChanged();
                }
            };
        }

        boolean isMatch(String name, int settingType) {
            return name != null && name.equals(mName) && settingType == mSettingType;
        }

        boolean isMatch(IBinder binder) {
            return mSessionList.contains(binder);
        }

        boolean addSession(IAppSession session) {
            final IBinder binder = session.asBinder();
            if (mSessionList.contains(binder)) {
                return false;
            }
            mSessionList.add(binder);
            return true;
        }

        boolean removeSession(IAppSession session) {
            final IBinder binder = session.asBinder();
            if (!mSessionList.contains(binder)) {
                return false;
            }
            mSessionList.remove(binder);
            return true;
        }

        int getSessionCount() {
            return mSessionList.size();
        }

        void registerSettingsObserver() {
            mContext.getContentResolver().registerContentObserver(mUri, false, mContentObserver);
        }

        void unregisterSettingsObserver() {
            mContext.getContentResolver().unregisterContentObserver(mContentObserver);
        }

        private void onSettingsChanged() {
            String value = null;
            if (mSettingType == SETTINGS_TYPE_GLOBAL) {
                value = Settings.Global.getString(mContext.getContentResolver(), mName);
            } else if (mSettingType == SETTINGS_TYPE_SYSTEM) {
                value = Settings.System.getString(mContext.getContentResolver(), mName);
            } else if (mSettingType == SETTINGS_TYPE_SECURE) {
                value = Settings.Secure.getString(mContext.getContentResolver(), mName);
            }
            synchronized (sClientBinderList) {
                for (IBinder binder : mSessionList) {
                    sendDataToClient(binder, value);
                }
            }
        }

        private void sendDataToClient(IBinder binder, String value) {
            final Parcel data = Parcel.obtain();
            final Parcel reply = Parcel.obtain();
            try {
                data.writeInterfaceToken(MsgDispatcher.APP_SESSION_DESCRIPTOR);
                data.writeInt(mSettingType);
                data.writeString(mName);
                data.writeInt(mValueType);
                if (mValueType == SETTINGS_VALUE_TYPE_INT) {
                    data.writeInt(Integer.parseInt(value));
                } else if (mValueType == SETTINGS_VALUE_TYPE_STRING) {
                    data.writeString(value);
                }
                binder.transact(MsgDispatcher.CODE_DISPATCH_SETTINGS_VALUE_FOR_NATIVE, data, reply,
                        IBinder.FLAG_ONEWAY);
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                data.recycle();
                reply.recycle();
            }
        }
    }

    private static List<SettingsObserverRecord> querySettingsObserverRecordByBinder(
            IBinder binder) {
        final List<SettingsObserverRecord> records = new ArrayList<>();
        for (SettingsObserverRecord record : sSettingsObserverRecordList) {
            if (record.isMatch(binder)) {
                records.add(record);
            }
        }
        return records;
    }

    private static SettingsObserverRecord getSettingsObserverRecord(int settingType, String name) {
        for (SettingsObserverRecord record : sSettingsObserverRecordList) {
            if (record.isMatch(name, settingType)) {
                return record;
            }
        }
        return null;
    }

    public void registerSettingsObserver(Context context, IAppSession session, int settingType,
            String name, int valueType, Handler handler) {
        if (context == null || session == null || name == null) {
            Slog.w(ApiLayerService.TAG, "ignore registerSettingsObserver, context [" + context
                    + "], session [" + session + "], name [" + name + "]");
            return;
        }
        synchronized (sClientBinderList) {
            SettingsObserverRecord record = getSettingsObserverRecord(settingType, name);
            if (record == null) {
                record = new SettingsObserverRecord(context, name, settingType, valueType,
                        handler);
                record.registerSettingsObserver();
                sSettingsObserverRecordList.add(record);
                final List<SettingsObserverRecord> list =
                        querySettingsObserverRecordByBinder(session.asBinder());
                if (list.size() == 0) {
                    sClientBinderList.register(session);
                }
            }
            record.addSession(session);
        }
    }

    public void unregisterSettingsObserver(Context context, IAppSession session, int settingType,
            String name) {
        if (context == null || session == null || name == null) {
            return;
        }
        synchronized (sClientBinderList) {
            final SettingsObserverRecord record = getSettingsObserverRecord(settingType, name);
            if (record == null) {
                return;
            }
            if (record.removeSession(session)) {
                if (record.getSessionCount() == 0) {
                    record.unregisterSettingsObserver();
                    sSettingsObserverRecordList.remove(record);
                }
                final List<SettingsObserverRecord> list =
                        querySettingsObserverRecordByBinder(session.asBinder());
                if (list.size() == 0) {
                    sClientBinderList.unregister(session);
                }
            }
        }
    }
}
