// Copyright 2026 Picomisu contributors
// SPDX-License-Identifier: Apache-2.0

package android.app;

import android.os.Binder;
import android.os.IBinder;
import android.os.Process;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.util.Singleton;
import android.util.Slog;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Client of the Smartisan "freeze" service ({@link IFreezeManager}, served by the
 * FreezeManagerService of the optional sys services JAR). Processes register callbacks that
 * run when a frozen process is unfrozen. Reconstructed from the PICO OS 5.13.7 factory
 * framework; the native counterpart is libbinder's android::FreezeManager.
 *
 * @hide
 */
public class FreezeManager {
    private static final String TAG = "FreezeManager";
    public static final String FREEZE_SERVICE = "freeze";
    private static final int FREEZE_APP = 1;
    private static final int UN_FREEZE_APP = 2;

    private static FreezeManager instance;

    public interface UnFreezeCallback {
        void onAppUnfreeze(Object cachedData);
    }

    private class CallbackData {
        public UnFreezeCallback mCallback;
        public Object mCachedData;
        public TargetDeathRecipient mDeathRecipient;

        public CallbackData(UnFreezeCallback callback, Object cachedData,
                TargetDeathRecipient deathRecipient) {
            mCallback = callback;
            mCachedData = cachedData;
            mDeathRecipient = deathRecipient;
        }
    }

    private Map<Integer, Map<KeyPair, CallbackData>> unFreezeOnceCallBacks = new HashMap<>();
    private Map<Integer, Map<KeyPair, CallbackData>> unFreezeCallBacks = new HashMap<>();

    Map<Object, CallbackData> selfCallbacks = new HashMap<>();

    private FreezeManager() {
    }

    private void dispatchUnfreezeEvent(int pid) {
        if (pid == Process.myPid()) {
            for (CallbackData callback : selfCallbacks.values()) {
                if (callback.mCallback != null) {
                    callback.mCallback.onAppUnfreeze(callback.mCachedData);
                }
            }
        } else {
            unFreezeCallback(pid, unFreezeCallBacks);
            unFreezeCallback(pid, unFreezeOnceCallBacks);
            unFreezeOnceCallBacks.remove(pid);
        }
    }

    private void unFreezeCallback(int remotePid,
            Map<Integer, Map<KeyPair, CallbackData>> callbacks) {
        synchronized (this) {
            Map<KeyPair, CallbackData> map = callbacks.get(remotePid);
            if (map == null) {
                return;
            }
            for (CallbackData callbackData : map.values()) {
                if (callbackData == null || callbackData.mCallback == null) {
                    return;
                }
                callbackData.mCallback.onAppUnfreeze(callbackData.mCachedData);
            }
        }
    }

    public static IFreezeManager getService() {
        return IFreezeManagerSingleton.get();
    }

    private static final Singleton<IFreezeManager> IFreezeManagerSingleton =
            new Singleton<IFreezeManager>() {
                @Override
                protected IFreezeManager create() {
                    final IBinder b = ServiceManager.getService(FREEZE_SERVICE);
                    final IFreezeManager fm = IFreezeManager.Stub.asInterface(b);
                    return fm;
                }
            };

    private IUnFreezeCallback.Stub apiCallback = new IUnFreezeCallback.Stub() {
        @Override
        public void onUnFreeze(int remotePid) {
            dispatchUnfreezeEvent(remotePid);
        }
    };

    public void registerSelfUnFreezeListener(Object owner, UnFreezeCallback localCallBack,
            Object cachedData, boolean byOnce) {
        if (owner == null || localCallBack == null) {
            return;
        }
        synchronized (selfCallbacks) {
            try {
                if (selfCallbacks.size() <= 0) {
                    getService().registerUnFreezeListener(Process.myPid(), apiCallback, byOnce);
                }
                CallbackData selfCallback = new CallbackData(localCallBack, cachedData, null);
                selfCallbacks.put(owner, selfCallback);
            } catch (RemoteException e) {
                Slog.e(TAG, "regist self error", e);
            }
        }
    }

    public void unRegisterSelfUnFreezeListener(Object owner) {
        if (owner == null) {
            return;
        }
        synchronized (selfCallbacks) {
            if (selfCallbacks.containsKey(owner)) {
                selfCallbacks.remove(owner);
            }
        }
    }

    public void registerUnFreezeListener(Object owner, int remotePid, IBinder target,
            UnFreezeCallback localCallBack, Object cachedData, boolean byOnce) {
        int callingUid = Binder.getCallingUid();
        if (callingUid > 10000) {
            return;
        }
        registerUnFreezeListenerInner(owner, remotePid, target, localCallBack, cachedData, byOnce);
    }

    private void registerUnFreezeListenerInner(Object owner, int remotePid, IBinder target,
            UnFreezeCallback localCallBack, Object cachedData, boolean byOnce) {
        if (owner == null || target == null || localCallBack == null || remotePid <= 0) {
            return;
        }
        try {
            Map<KeyPair, CallbackData> map;
            if (!byOnce) {
                map = unFreezeCallBacks.get(remotePid);
                if (map == null || map.isEmpty()) {
                    map = new HashMap<>();
                    unFreezeCallBacks.put(remotePid, map);
                }
            } else {
                map = unFreezeOnceCallBacks.get(remotePid);
                if (map == null || map.isEmpty()) {
                    map = new HashMap<>();
                    unFreezeOnceCallBacks.put(remotePid, map);
                }
            }
            KeyPair source = containsKey(map, owner, target);
            if (source == null) {
                source = new KeyPair(owner, target);
            }
            TargetDeathRecipient deathRecipient = new TargetDeathRecipient(remotePid);
            map.put(source, new CallbackData(localCallBack, cachedData, deathRecipient));
            getService().registerUnFreezeListener(remotePid, apiCallback, byOnce);
            target.linkToDeath(deathRecipient, 0);
        } catch (RemoteException e) {
            Slog.e(TAG, "register listener error ", e);
        }
    }

    public void registerUnFreezeListener(Object owner, IBinder target,
            UnFreezeCallback localCallBack, Object cachedData, boolean byOnce) {
        int remotePid = Binder.getLastFrozenPid();
        registerUnFreezeListenerInner(owner, remotePid, target, localCallBack, cachedData, byOnce);
    }

    public void unRegisterUnFreezeListener(Object owner, IBinder target) {
        if (owner == null || target == null) {
            return;
        }
        unRegisterUnFreezeListener(owner, target, unFreezeOnceCallBacks);
        unRegisterUnFreezeListener(owner, target, unFreezeCallBacks);
    }

    private void unRegisterUnFreezeListener(Object owner, IBinder target,
            Map<Integer, Map<KeyPair, CallbackData>> callbacks) {
        try {
            synchronized (this) {
                Iterator<Map.Entry<Integer, Map<KeyPair, CallbackData>>> it =
                        callbacks.entrySet().iterator();
                while (it.hasNext()) {
                    Map.Entry<Integer, Map<KeyPair, CallbackData>> entry = it.next();
                    Map<KeyPair, CallbackData> map = entry.getValue();
                    KeyPair keyPair = containsKey(map, owner, target);
                    if (keyPair != null) {
                        CallbackData callbackData = map.get(keyPair);
                        if (callbackData != null) {
                            target.unlinkToDeath(callbackData.mDeathRecipient, 0);
                        }
                        map.remove(keyPair);
                        if (map.size() <= 0) {
                            getService().unRegisterUnFreezeListener(entry.getKey(), apiCallback);
                            it.remove();
                        }
                    }
                }
            }
        } catch (RemoteException e) {
            Slog.e(TAG, "un register listener error ", e);
        }
    }

    public void unRegisterUnFreezeListener(IBinder target) {
        if (target == null) {
            return;
        }
        unRegisterUnFreezeListener(target, unFreezeCallBacks);
        unRegisterUnFreezeListener(target, unFreezeOnceCallBacks);
    }

    public void freezeUid(int uid) {
        try {
            getService().freezeUid(uid);
        } catch (RemoteException e) {
            Slog.e(TAG, "freezeUid error " + e);
        }
    }

    private void unRegisterUnFreezeListener(IBinder target,
            Map<Integer, Map<KeyPair, CallbackData>> callbacks) {
        try {
            synchronized (this) {
                Iterator<Map.Entry<Integer, Map<KeyPair, CallbackData>>> it =
                        callbacks.entrySet().iterator();
                while (it.hasNext()) {
                    Map.Entry<Integer, Map<KeyPair, CallbackData>> entry = it.next();
                    Map<KeyPair, CallbackData> map = entry.getValue();
                    KeyPair keyPair = containsKey(map, target);
                    if (keyPair != null) {
                        CallbackData callbackData = map.get(keyPair);
                        if (callbackData != null) {
                            target.unlinkToDeath(callbackData.mDeathRecipient, 0);
                        }
                        map.remove(keyPair);
                        if (map.size() <= 0) {
                            getService().unRegisterUnFreezeListener(entry.getKey(), apiCallback);
                            it.remove();
                        }
                    }
                }
            }
        } catch (RemoteException e) {
            Slog.e(TAG, "un register listener error ", e);
        }
    }

    public static FreezeManager getInstance() {
        if (instance == null) {
            synchronized (FreezeManager.class) {
                if (instance == null) {
                    instance = new FreezeManager();
                }
            }
        }
        return instance;
    }

    private void binderDeathCallback(int remotePid,
            Map<Integer, Map<KeyPair, CallbackData>> callbacks) {
        synchronized (this) {
            if (callbacks.containsKey(remotePid)) {
                // The factory reads the persistent registry here for both registries.
                Map<KeyPair, CallbackData> map = unFreezeCallBacks.get(remotePid);
                if (map != null) {
                    Iterator<Map.Entry<KeyPair, CallbackData>> it = map.entrySet().iterator();
                    while (it.hasNext()) {
                        Map.Entry<KeyPair, CallbackData> entry = it.next();
                        entry.getKey().mBinder.unlinkToDeath(entry.getValue().mDeathRecipient, 0);
                    }
                }
                callbacks.remove(remotePid);
                try {
                    getService().unRegisterUnFreezeListener(remotePid, apiCallback);
                } catch (RemoteException e) {
                    Slog.e(TAG, "binder dead call back", e);
                }
            }
        }
    }

    private final class TargetDeathRecipient implements IBinder.DeathRecipient {
        final int mRemotePid;

        TargetDeathRecipient(int remotePid) {
            mRemotePid = remotePid;
        }

        @Override
        public void binderDied() {
            binderDeathCallback(mRemotePid, unFreezeCallBacks);
            binderDeathCallback(mRemotePid, unFreezeOnceCallBacks);
        }
    }

    private class KeyPair {
        private final Object mOwner;
        private final IBinder mBinder;

        public KeyPair(Object owner, IBinder binder) {
            mOwner = owner;
            mBinder = binder;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof KeyPair)) {
                return false;
            }
            KeyPair key = (KeyPair) o;
            return mOwner == key.mOwner && mBinder == key.mBinder;
        }

        public boolean equals(Object owner, IBinder binder) {
            return mOwner == owner && mBinder == binder;
        }
    }

    private KeyPair containsKey(Map<KeyPair, CallbackData> map, Object owner, IBinder binder) {
        for (KeyPair key : map.keySet()) {
            if (key.equals(owner, binder)) {
                return key;
            }
        }
        return null;
    }

    private KeyPair containsKey(Map<KeyPair, CallbackData> map, IBinder binder) {
        for (KeyPair key : map.keySet()) {
            if (key.mBinder == binder) {
                return key;
            }
        }
        return null;
    }
}
