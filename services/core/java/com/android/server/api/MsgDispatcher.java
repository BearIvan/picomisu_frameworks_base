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

/**
 * Delivers API layer events to registered {@link IAppSession} binders (factory PICO OS 5.13.7
 * com.android.server.api.MsgDispatcher), ported method by method. Each session registers with
 * listening flags; an event is posted to the "ApiLayer" worker thread, broadcast to the sessions
 * whose flags include it and sent as a oneway transaction on the session binder, with the codes
 * and parcel layouts the client-side AppSession expects.
 *
 * @hide
 */
public class MsgDispatcher {
    public static final String APP_SESSION_DESCRIPTOR = "com.pico.api.app.IAppSession";
    public static final int CODE_DISPATCH_ACTIVITY_STARTING = 112;
    public static final int CODE_DISPATCH_BROADCAST_FOR_NATIVE = 111;
    public static final int CODE_DISPATCH_KEY_EVENT = 106;
    public static final int CODE_DISPATCH_KEY_EVENT_FOR_NATIVE = 108;
    public static final int CODE_DISPATCH_SETTINGS_VALUE_FOR_NATIVE = 110;
    public static final int CODE_IME_SHOWING_STATE_CHANGED = 103;
    public static final int CODE_POWER_STATE_CHANGED = 109;
    public static final int CODE_RUNNING_2D_APP_CHANGED = 105;
    public static final int CODE_SCREEN_STATE_CHANGED = 104;
    public static final int CODE_SEETHROUGH_STATE_CHANGED = 101;
    public static final int CODE_SHOWING_3D_APP_CHANGED = 102;
    public static final int CODE_XR_RUNTIME_STATE_CHANGED = 107;
    public static final int FLAG_LISTENING_3D_APP_SHOWING = 2;
    public static final int FLAG_LISTENING_ACTIVITY_STARTING = 512;
    public static final int FLAG_LISTENING_IME_SHOWING_STATE = 4;
    public static final int FLAG_LISTENING_KEY_EVENT = 32;
    public static final int FLAG_LISTENING_KEY_EVENT_FOR_NATIVE = 128;
    public static final int FLAG_LISTENING_POWER_STATE = 256;
    public static final int FLAG_LISTENING_RUNNING_2D_APP_CHANGE = 16;
    public static final int FLAG_LISTENING_SCREEN_STATE_CHANGE = 8;
    public static final int FLAG_LISTENING_SEETHROUGH_CHANGE = 1;
    public static final int FLAG_LISTENING_XR_RUNTIME_STATE = 64;
    private static final RemoteCallbackList<IAppSession> sAppSessionList;
    private static Handler sH;
    private static final Map<IBinder, Integer> sListeningFlagMap;
    private static final HandlerThread sWorkerThread = new HandlerThread(ApiLayerService.TAG);

    static {
        sWorkerThread.start();
        sH = new Handler(sWorkerThread.getLooper());
        sListeningFlagMap = new HashMap<>();
        sAppSessionList = new RemoteCallbackList<IAppSession>() {
            @Override
            public void onCallbackDied(IAppSession callback) {
                synchronized (sAppSessionList) {
                    sListeningFlagMap.remove(callback.asBinder());
                }
            }
        };
    }

    public Handler getHandler() {
        return sH;
    }

    public void registerAppSession(IAppSession session, int listeningFlags) {
        IBinder binder = session.asBinder();
        synchronized (sAppSessionList) {
            if (!sListeningFlagMap.containsKey(binder)) {
                sAppSessionList.register(session);
            }
            sListeningFlagMap.put(binder, Integer.valueOf(listeningFlags));
        }
    }

    public void unregisterAppSession(IAppSession session, int listeningFlags) {
        IBinder binder = session.asBinder();
        synchronized (sAppSessionList) {
            if (listeningFlags == 0) {
                sAppSessionList.unregister(session);
                sListeningFlagMap.remove(binder);
            } else {
                sListeningFlagMap.put(binder, Integer.valueOf(listeningFlags));
            }
        }
    }

    private int getListenerFlags(IAppSession session) {
        Integer flags = sListeningFlagMap.get(session.asBinder());
        if (flags == null) {
            return 0;
        }
        return flags.intValue();
    }

    public void dispatchSeethroughState(final int state) {
        sH.post(() -> {
            synchronized (sAppSessionList) {
                sAppSessionList.broadcast(session -> {
                    int listeningFlags = getListenerFlags(session);
                    if ((listeningFlags & FLAG_LISTENING_SEETHROUGH_CHANGE) > 0) {
                        sendSeethroughState(session, state);
                    }
                });
            }
        });
    }

    private void sendSeethroughState(IAppSession session, int state) {
        Parcel data = Parcel.obtain();
        Parcel reply = Parcel.obtain();
        try {
            data.writeInterfaceToken(APP_SESSION_DESCRIPTOR);
            data.writeInt(state);
            session.asBinder().transact(CODE_SEETHROUGH_STATE_CHANGED, data, reply,
                    IBinder.FLAG_ONEWAY);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            data.recycle();
            reply.recycle();
        }
    }

    public void dispatchShowing3dApp(final String pkg) {
        sH.post(() -> {
            synchronized (sAppSessionList) {
                sAppSessionList.broadcast(session -> {
                    int listeningFlags = getListenerFlags(session);
                    if ((listeningFlags & FLAG_LISTENING_3D_APP_SHOWING) > 0) {
                        sendShowing3dApp(session, pkg);
                    }
                });
            }
        });
    }

    private void sendShowing3dApp(IAppSession session, String pkg) {
        Parcel data = Parcel.obtain();
        Parcel reply = Parcel.obtain();
        try {
            data.writeInterfaceToken(APP_SESSION_DESCRIPTOR);
            if (pkg == null) {
                data.writeInt(0);
            } else {
                data.writeInt(1);
                data.writeString(pkg);
            }
            session.asBinder().transact(CODE_SHOWING_3D_APP_CHANGED, data, reply,
                    IBinder.FLAG_ONEWAY);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            data.recycle();
            reply.recycle();
        }
    }

    public void dispatchXrRuntimeState(final Bundle state) {
        sH.post(() -> {
            synchronized (sAppSessionList) {
                sAppSessionList.broadcast(session -> {
                    int listeningFlags = getListenerFlags(session);
                    if ((listeningFlags & FLAG_LISTENING_XR_RUNTIME_STATE) > 0) {
                        sendXrRuntimeState(session, state);
                    }
                });
            }
        });
    }

    private void sendXrRuntimeState(IAppSession session, Bundle state) {
        Parcel data = Parcel.obtain();
        Parcel reply = Parcel.obtain();
        try {
            data.writeInterfaceToken(APP_SESSION_DESCRIPTOR);
            if (state == null) {
                data.writeInt(0);
            } else {
                data.writeInt(1);
                state.writeToParcel(data, 0);
            }
            session.asBinder().transact(CODE_XR_RUNTIME_STATE_CHANGED, data, reply,
                    IBinder.FLAG_ONEWAY);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            data.recycle();
            reply.recycle();
        }
    }

    public void dispatchRunning2dApp(final String running2dApp) {
        sH.post(() -> {
            synchronized (sAppSessionList) {
                sAppSessionList.broadcast(session -> {
                    int listeningFlags = getListenerFlags(session);
                    if ((listeningFlags & FLAG_LISTENING_RUNNING_2D_APP_CHANGE) > 0) {
                        sendRunning2dApp(session, running2dApp);
                    }
                });
            }
        });
    }

    private void sendRunning2dApp(IAppSession session, String running2dAppData) {
        Parcel data = Parcel.obtain();
        Parcel reply = Parcel.obtain();
        try {
            data.writeInterfaceToken(APP_SESSION_DESCRIPTOR);
            if (running2dAppData == null) {
                data.writeInt(0);
            } else {
                data.writeInt(1);
                data.writeString(running2dAppData);
            }
            session.asBinder().transact(CODE_RUNNING_2D_APP_CHANGED, data, reply,
                    IBinder.FLAG_ONEWAY);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            data.recycle();
            reply.recycle();
        }
    }

    public void dispatchImeState(final boolean showing) {
        sH.post(() -> {
            synchronized (sAppSessionList) {
                sAppSessionList.broadcast(session -> {
                    int listeningFlags = getListenerFlags(session);
                    if ((listeningFlags & FLAG_LISTENING_IME_SHOWING_STATE) > 0) {
                        sendImeShowingState(session, showing);
                    }
                });
            }
        });
    }

    private void sendImeShowingState(IAppSession session, boolean showing) {
        Parcel data = Parcel.obtain();
        Parcel reply = Parcel.obtain();
        try {
            data.writeInterfaceToken(APP_SESSION_DESCRIPTOR);
            data.writeInt(showing ? 1 : 0);
            session.asBinder().transact(CODE_IME_SHOWING_STATE_CHANGED, data, reply,
                    IBinder.FLAG_ONEWAY);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            data.recycle();
            reply.recycle();
        }
    }

    public void dispatchScreenState(final boolean on) {
        sH.post(() -> {
            synchronized (sAppSessionList) {
                sAppSessionList.broadcast(session -> {
                    int listeningFlags = getListenerFlags(session);
                    if ((listeningFlags & FLAG_LISTENING_SCREEN_STATE_CHANGE) > 0) {
                        sendScreenState(session, on);
                    }
                });
            }
        });
    }

    private void sendScreenState(IAppSession session, boolean on) {
        Parcel data = Parcel.obtain();
        Parcel reply = Parcel.obtain();
        try {
            data.writeInterfaceToken(APP_SESSION_DESCRIPTOR);
            data.writeInt(on ? 1 : 0);
            session.asBinder().transact(CODE_SCREEN_STATE_CHANGED, data, reply,
                    IBinder.FLAG_ONEWAY);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            data.recycle();
            reply.recycle();
        }
    }

    /** A session gets the KeyEvent parcel, or with FLAG_LISTENING_KEY_EVENT_FOR_NATIVE its fields. */
    public void dispatchKeyEvent(final KeyEvent keyEvent) {
        sH.post(() -> {
            synchronized (sAppSessionList) {
                sAppSessionList.broadcast(session -> {
                    int listeningFlags = getListenerFlags(session);
                    if ((listeningFlags & FLAG_LISTENING_KEY_EVENT) > 0) {
                        sendKeyEvent(session, keyEvent);
                    } else if ((listeningFlags & FLAG_LISTENING_KEY_EVENT_FOR_NATIVE) > 0) {
                        sendKeyEventForNative(session, keyEvent);
                    }
                });
            }
            keyEvent.recycle();
        });
    }

    private void sendKeyEvent(IAppSession session, KeyEvent event) {
        Parcel data = Parcel.obtain();
        Parcel reply = Parcel.obtain();
        try {
            data.writeInterfaceToken(APP_SESSION_DESCRIPTOR);
            if (event == null) {
                data.writeInt(0);
            } else {
                data.writeInt(1);
                event.writeToParcel(data, 0);
            }
            session.asBinder().transact(CODE_DISPATCH_KEY_EVENT, data, reply,
                    IBinder.FLAG_ONEWAY);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            data.recycle();
            reply.recycle();
        }
    }

    private void sendKeyEventForNative(IAppSession session, KeyEvent event) {
        Parcel data = Parcel.obtain();
        Parcel reply = Parcel.obtain();
        try {
            data.writeInterfaceToken(APP_SESSION_DESCRIPTOR);
            data.writeInt(event.getDeviceId());
            data.writeInt(event.getSource());
            data.writeInt(event.getDisplayId());
            data.writeInt(event.getMetaState());
            data.writeInt(event.getAction());
            data.writeInt(event.getKeyCode());
            data.writeInt(event.getScanCode());
            data.writeInt(event.getRepeatCount());
            data.writeInt(event.getFlags());
            data.writeLong(event.getDownTime());
            data.writeLong(event.getEventTime());
            session.asBinder().transact(CODE_DISPATCH_KEY_EVENT_FOR_NATIVE, data, reply,
                    IBinder.FLAG_ONEWAY);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            data.recycle();
            reply.recycle();
        }
    }

    public void dispatchActivityStarting(final ActivityInfo aInfo) {
        sH.post(() -> {
            synchronized (sAppSessionList) {
                sAppSessionList.broadcast(session -> {
                    int listeningFlags = getListenerFlags(session);
                    if ((listeningFlags & FLAG_LISTENING_ACTIVITY_STARTING) > 0) {
                        sendActivityStarting(session, aInfo);
                    }
                });
            }
        });
    }

    /** The factory sends this one without a reply parcel. */
    private void sendActivityStarting(IAppSession session, ActivityInfo aInfo) {
        Parcel data = Parcel.obtain();
        try {
            data.writeInterfaceToken(APP_SESSION_DESCRIPTOR);
            if (aInfo == null) {
                data.writeInt(0);
            } else {
                data.writeInt(1);
                aInfo.writeToParcel(data, 0);
            }
            session.asBinder().transact(CODE_DISPATCH_ACTIVITY_STARTING, data, null,
                    IBinder.FLAG_ONEWAY);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            data.recycle();
        }
    }

    public void dispatchPowerStateChanged(final int state) {
        sH.post(() -> {
            synchronized (sAppSessionList) {
                sAppSessionList.broadcast(session -> {
                    int listeningFlags = getListenerFlags(session);
                    if ((listeningFlags & FLAG_LISTENING_POWER_STATE) > 0) {
                        sendPowerState(session, state);
                    }
                });
            }
        });
    }

    private void sendPowerState(IAppSession session, int state) {
        Parcel data = Parcel.obtain();
        Parcel reply = Parcel.obtain();
        try {
            data.writeInterfaceToken(APP_SESSION_DESCRIPTOR);
            data.writeInt(state);
            session.asBinder().transact(CODE_POWER_STATE_CHANGED, data, reply,
                    IBinder.FLAG_ONEWAY);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            data.recycle();
            reply.recycle();
        }
    }
}
