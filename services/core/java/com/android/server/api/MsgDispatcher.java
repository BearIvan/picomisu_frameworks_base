/*
 * Copyright 2026 Picomisu contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package com.android.server.api;

import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Parcel;
import android.os.RemoteCallbackList;
import android.view.KeyEvent;

import com.pico.api.app.IAppSession;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Delivers API layer events to registered {@link IAppSession} binders (factory PICO OS 5.13.7
 * com.android.server.api.MsgDispatcher). Each session registers with listening flags; an event is
 * sent as a oneway transaction on the session binder, with the codes and parcel layouts the
 * client-side AppSession expects. Events are posted to the "ApiLayer" worker thread.
 *
 * @hide
 */
public class MsgDispatcher {
    public static final String APP_SESSION_DESCRIPTOR = "com.pico.api.app.IAppSession";

    public static final int CODE_SEETHROUGH_STATE_CHANGED = 101;
    public static final int CODE_SHOWING_3D_APP_CHANGED = 102;
    public static final int CODE_IME_SHOWING_STATE_CHANGED = 103;
    public static final int CODE_SCREEN_STATE_CHANGED = 104;
    public static final int CODE_RUNNING_2D_APP_CHANGED = 105;
    public static final int CODE_DISPATCH_KEY_EVENT = 106;
    public static final int CODE_XR_RUNTIME_STATE_CHANGED = 107;
    public static final int CODE_DISPATCH_KEY_EVENT_FOR_NATIVE = 108;
    public static final int CODE_POWER_STATE_CHANGED = 109;
    public static final int CODE_DISPATCH_SETTINGS_VALUE_FOR_NATIVE = 110;
    public static final int CODE_DISPATCH_BROADCAST_FOR_NATIVE = 111;
    public static final int CODE_DISPATCH_ACTIVITY_STARTING = 112;

    public static final int FLAG_LISTENING_SEETHROUGH_CHANGE = 1;
    public static final int FLAG_LISTENING_3D_APP_SHOWING = 2;
    public static final int FLAG_LISTENING_IME_SHOWING_STATE = 4;
    public static final int FLAG_LISTENING_SCREEN_STATE_CHANGE = 8;
    public static final int FLAG_LISTENING_RUNNING_2D_APP_CHANGE = 16;
    public static final int FLAG_LISTENING_KEY_EVENT = 32;
    public static final int FLAG_LISTENING_XR_RUNTIME_STATE = 64;
    public static final int FLAG_LISTENING_KEY_EVENT_FOR_NATIVE = 128;
    public static final int FLAG_LISTENING_POWER_STATE = 256;
    public static final int FLAG_LISTENING_ACTIVITY_STARTING = 512;

    private static final HandlerThread sWorkerThread = new HandlerThread(ApiLayerService.TAG);
    private static final Handler sH;
    private static final Map<IBinder, Integer> sListeningFlagMap = new HashMap<>();
    private static final RemoteCallbackList<IAppSession> sAppSessionList =
            new RemoteCallbackList<IAppSession>() {
                @Override
                public void onCallbackDied(IAppSession callback) {
                    synchronized (sAppSessionList) {
                        sListeningFlagMap.remove(callback.asBinder());
                    }
                }
            };

    static {
        sWorkerThread.start();
        sH = new Handler(sWorkerThread.getLooper());
    }

    /** Writes the event payload after the interface token. */
    private interface Payload {
        void write(Parcel data);
    }

    public Handler getHandler() {
        return sH;
    }

    public void registerAppSession(IAppSession session, int listeningFlags) {
        final IBinder binder = session.asBinder();
        synchronized (sAppSessionList) {
            if (!sListeningFlagMap.containsKey(binder)) {
                sAppSessionList.register(session);
            }
            sListeningFlagMap.put(binder, listeningFlags);
        }
    }

    public void unregisterAppSession(IAppSession session, int listeningFlags) {
        final IBinder binder = session.asBinder();
        synchronized (sAppSessionList) {
            if (listeningFlags == 0) {
                sAppSessionList.unregister(session);
                sListeningFlagMap.remove(binder);
            } else {
                sListeningFlagMap.put(binder, listeningFlags);
            }
        }
    }

    private int getListenerFlags(IAppSession session) {
        final Integer flags = sListeningFlagMap.get(session.asBinder());
        return flags == null ? 0 : flags;
    }

    /** Posts a broadcast of {@code code} to every session listening with {@code flag}. */
    private void post(int flag, int code, Payload payload) {
        sH.post(() -> broadcast(session -> {
            if ((getListenerFlags(session) & flag) > 0) {
                send(session, code, payload, true);
            }
        }));
    }

    private void broadcast(Consumer<IAppSession> action) {
        synchronized (sAppSessionList) {
            sAppSessionList.broadcast(action);
        }
    }

    private static void send(IAppSession session, int code, Payload payload, boolean withReply) {
        final Parcel data = Parcel.obtain();
        final Parcel reply = withReply ? Parcel.obtain() : null;
        try {
            data.writeInterfaceToken(APP_SESSION_DESCRIPTOR);
            payload.write(data);
            session.asBinder().transact(code, data, reply, IBinder.FLAG_ONEWAY);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            data.recycle();
            if (reply != null) {
                reply.recycle();
            }
        }
    }

    private static void writeNullableString(Parcel data, String value) {
        if (value == null) {
            data.writeInt(0);
        } else {
            data.writeInt(1);
            data.writeString(value);
        }
    }

    public void dispatchSeethroughState(int state) {
        post(FLAG_LISTENING_SEETHROUGH_CHANGE, CODE_SEETHROUGH_STATE_CHANGED,
                data -> data.writeInt(state));
    }

    public void dispatchShowing3dApp(String pkg) {
        post(FLAG_LISTENING_3D_APP_SHOWING, CODE_SHOWING_3D_APP_CHANGED,
                data -> writeNullableString(data, pkg));
    }

    public void dispatchXrRuntimeState(Bundle state) {
        post(FLAG_LISTENING_XR_RUNTIME_STATE, CODE_XR_RUNTIME_STATE_CHANGED, data -> {
            if (state == null) {
                data.writeInt(0);
            } else {
                data.writeInt(1);
                state.writeToParcel(data, 0);
            }
        });
    }

    public void dispatchRunning2dApp(String running2dAppData) {
        post(FLAG_LISTENING_RUNNING_2D_APP_CHANGE, CODE_RUNNING_2D_APP_CHANGED,
                data -> writeNullableString(data, running2dAppData));
    }

    public void dispatchImeState(boolean showing) {
        post(FLAG_LISTENING_IME_SHOWING_STATE, CODE_IME_SHOWING_STATE_CHANGED,
                data -> data.writeInt(showing ? 1 : 0));
    }

    public void dispatchScreenState(boolean on) {
        post(FLAG_LISTENING_SCREEN_STATE_CHANGE, CODE_SCREEN_STATE_CHANGED,
                data -> data.writeInt(on ? 1 : 0));
    }

    public void dispatchPowerStateChanged(int state) {
        post(FLAG_LISTENING_POWER_STATE, CODE_POWER_STATE_CHANGED, data -> data.writeInt(state));
    }

    /** A session gets the KeyEvent parcel, or with FLAG_LISTENING_KEY_EVENT_FOR_NATIVE its fields. */
    public void dispatchKeyEvent(KeyEvent keyEvent) {
        sH.post(() -> {
            broadcast(session -> {
                final int flags = getListenerFlags(session);
                if ((flags & FLAG_LISTENING_KEY_EVENT) > 0) {
                    send(session, CODE_DISPATCH_KEY_EVENT, data -> {
                        data.writeInt(1);
                        keyEvent.writeToParcel(data, 0);
                    }, true);
                } else if ((flags & FLAG_LISTENING_KEY_EVENT_FOR_NATIVE) > 0) {
                    send(session, CODE_DISPATCH_KEY_EVENT_FOR_NATIVE, data -> {
                        data.writeInt(keyEvent.getDeviceId());
                        data.writeInt(keyEvent.getSource());
                        data.writeInt(keyEvent.getDisplayId());
                        data.writeInt(keyEvent.getMetaState());
                        data.writeInt(keyEvent.getAction());
                        data.writeInt(keyEvent.getKeyCode());
                        data.writeInt(keyEvent.getScanCode());
                        data.writeInt(keyEvent.getRepeatCount());
                        data.writeInt(keyEvent.getFlags());
                        data.writeLong(keyEvent.getDownTime());
                        data.writeLong(keyEvent.getEventTime());
                    }, true);
                }
            });
            keyEvent.recycle();
        });
    }

    public void dispatchActivityStarting(ActivityInfo aInfo) {
        sH.post(() -> broadcast(session -> {
            if ((getListenerFlags(session) & FLAG_LISTENING_ACTIVITY_STARTING) > 0) {
                // The factory sends this one without a reply parcel.
                send(session, CODE_DISPATCH_ACTIVITY_STARTING, data -> {
                    if (aInfo == null) {
                        data.writeInt(0);
                    } else {
                        data.writeInt(1);
                        aInfo.writeToParcel(data, 0);
                    }
                }, false);
            }
        }));
    }
}
