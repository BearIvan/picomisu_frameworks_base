// Copyright 2026 Picomisu contributors
// SPDX-License-Identifier: Apache-2.0

package android.app;

import android.os.IBinder;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.util.Log;

import java.util.HashMap;
import java.util.HashSet;

/**
 * Client of the PICO scene information service ("sceneinfo_service", {@link ISceneInfoManager},
 * served by PicoSyshub). Reports scene data and multiplexes the local listeners of a process
 * onto one remote {@link IDataListener} per listener type. Reconstructed from the PICO OS 5.13.7
 * factory framework.
 *
 * @hide
 */
public class SceneInfoManager {
    public static final String TAG = "SceneInfoManager";
    public static final String PICO_SCENEINFO_SERVICE = "sceneinfo_service";
    private static ISceneInfoManager sBinder = null;

    private static class LazyHolder {
        private static final SceneInfoManager INSTANCE = new SceneInfoManager();
    }

    public static SceneInfoManager getInstance() {
        return LazyHolder.INSTANCE;
    }

    private SceneInfoManager() {
    }

    public enum ListenerType {
        TYPE_UNKONW(0),
        TYPE_MEDIAMETRICS(1);

        private final int value;

        ListenerType(int value) {
            this.value = value;
        }

        public final int value() {
            return value;
        }

        public static ListenerType valueOf(int value) {
            switch (value) {
                case 1:
                    return TYPE_MEDIAMETRICS;
                default:
                    return TYPE_UNKONW;
            }
        }
    }

    public static boolean isFeatEnable() {
        return true;
    }

    public static boolean registerListener(int listenerType, DataListener listener) {
        if (listenerType == ListenerType.TYPE_UNKONW.value() || listener == null
                || getService() == null) {
            return false;
        }
        return SceneInfoRegistry.getInstance().registerListenerLocal(listenerType, listener,
                listener.getClsName());
    }

    public static boolean registerListener(int listenerType, IDataListener listener,
            String clsName) {
        if (listenerType == ListenerType.TYPE_UNKONW.value() || listener == null
                || getService() == null) {
            return false;
        }
        return SceneInfoRegistry.getInstance().registerListenerLocal(listenerType, listener,
                clsName);
    }

    public static boolean unRegisterListener(int listenerType, IDataListener listener) {
        if (listenerType == ListenerType.TYPE_UNKONW.value() || listener == null
                || getService() == null) {
            return false;
        }
        return SceneInfoRegistry.getInstance().unRegisterListenerLocal(listenerType, listener);
    }

    public static boolean unRegisterListener(IDataListener listener) {
        if (listener == null || getService() == null) {
            return false;
        }
        // The factory calls the static registry method through the singleton instance.
        return SceneInfoRegistry.getInstance().unRegisterListenerLocal(listener);
    }

    private static boolean registerListenerRemote(int listenerType, IDataListener listener,
            String clsName) {
        try {
            return getService().registerListener(listenerType, listener, clsName);
        } catch (RemoteException e) {
            Log.e(TAG, "registerListenerRemote RemoteException! listenerType:" + listenerType
                    + " listener:" + listener + " clsName:" + clsName);
        }
        return false;
    }

    private static boolean unRegisterListenerRemote(int listenerType, IDataListener listener) {
        try {
            return getService().unRegisterListenerType(listenerType, listener);
        } catch (RemoteException e) {
            Log.e(TAG, "unRegisterListenerRemote RemoteException! listenerType:" + listenerType
                    + " listener:" + listener);
        }
        return false;
    }

    private static boolean unRegisterListenerRemote(IDataListener listener) {
        try {
            return getService().unRegisterListener(listener);
        } catch (RemoteException e) {
            Log.e(TAG, "unRegisterListenerRemote RemoteException! listener:" + listener);
        }
        return false;
    }

    public static boolean reportDataInfo(int listenerType, int event, SceneData data) {
        if (listenerType == ListenerType.TYPE_UNKONW.value() || data == null
                || getService() == null) {
            return false;
        }
        try {
            return getService().reportDataInfo(listenerType, event, data);
        } catch (RemoteException e) {
            Log.e(TAG, "reportDataInfo RemoteException! listenerType:" + listenerType
                    + " event:" + event);
        }
        return false;
    }

    public static SceneData getTypeLastReport(int listenerType) {
        if (listenerType == ListenerType.TYPE_UNKONW.value()) {
            return null;
        }
        try {
            return getService().getTypeLastReport(listenerType);
        } catch (RemoteException e) {
            Log.e(TAG, "getTypeLastReport RemoteException! listenerType:" + listenerType);
        }
        return null;
    }

    public static synchronized ISceneInfoManager getService() {
        if (isFeatEnable() && sBinder == null) {
            IBinder b = ServiceManager.getService(PICO_SCENEINFO_SERVICE);
            if (b == null) {
                Log.e(TAG, "can't get service binder: SceneInfoManager");
                return null;
            }
            sBinder = ISceneInfoManager.Stub.asInterface(b);
            if (sBinder == null) {
                Log.e(TAG, "can't get service interface: SceneInfoManager");
            }
            try {
                ServerDeathRecipient sdr = new ServerDeathRecipient();
                sBinder.asBinder().linkToDeath(sdr, 0);
            } catch (RemoteException e) {
                Log.e(TAG, "SceneInfoManager getService RemoteException:" + e);
                e.printStackTrace();
                sBinder = null;
            }
        }
        return sBinder;
    }

    private static final class ServerDeathRecipient implements IBinder.DeathRecipient {
        @Override
        public void binderDied() {
            Log.e(TAG, "sceneinfo_service binderDied!");
            if (sBinder != null) {
                sBinder.asBinder().unlinkToDeath(this, 0);
                sBinder = null;
            }
            getService();
            SceneInfoRegistry.getInstance().ServerDied();
        }
    }

    private static class SceneInfoRegistry {
        public static final String TAG = "SceneInfoRegistry";
        private static final String DES_EMPTY = "";
        private static HashMap<ListenerType, HashSet<LocalListener>> sListenerMap = null;
        public static DataInfoListener sLocalListener = null;

        private static class LazyHolder {
            private static final SceneInfoRegistry INSTANCE = new SceneInfoRegistry();
        }

        public static SceneInfoRegistry getInstance() {
            return LazyHolder.INSTANCE;
        }

        private SceneInfoRegistry() {
            sListenerMap = new HashMap<>();
            sLocalListener = new DataInfoListener(getClass().getName());
        }

        public boolean registerListenerLocal(int listenerType, IDataListener listener,
                String clsName) {
            boolean ret = false;
            synchronized (sListenerMap) {
                HashSet<LocalListener> set = sListenerMap.get(ListenerType.valueOf(listenerType));
                if (set == null || set.isEmpty()) {
                    set = new HashSet<>();
                    ret = registerListenerRemote(listenerType, sLocalListener, clsName);
                }
                LocalListener localL = new LocalListener(listenerType, listener, clsName);
                set.add(localL);
                sListenerMap.put(ListenerType.valueOf(listenerType), set);
            }
            return ret;
        }

        public static boolean unRegisterListenerLocal(IDataListener listener) {
            boolean ret = false;
            synchronized (sListenerMap) {
                for (ListenerType type : sListenerMap.keySet()) {
                    HashSet<LocalListener> set = sListenerMap.get(type);
                    HashSet<LocalListener> temp = new HashSet<>();
                    if (set != null) {
                        temp.addAll(set);
                        for (LocalListener client : temp) {
                            if (client.listener != null
                                    && client.listener.asBinder().equals(listener.asBinder())) {
                                set.remove(client);
                                if (set.isEmpty()) {
                                    ret = unRegisterListenerRemote(client.listenerType,
                                            sLocalListener);
                                }
                                sListenerMap.put(ListenerType.valueOf(client.listenerType), set);
                            }
                        }
                    }
                }
            }
            return ret;
        }

        public boolean unRegisterListenerLocal(int listenerType, IDataListener listener) {
            boolean ret = false;
            synchronized (sListenerMap) {
                HashSet<LocalListener> set = sListenerMap.get(ListenerType.valueOf(listenerType));
                HashSet<LocalListener> temp = new HashSet<>();
                if (set != null) {
                    temp.addAll(set);
                    for (LocalListener client : temp) {
                        if (client.listener != null
                                && client.listener.asBinder().equals(listener.asBinder())) {
                            set.remove(client);
                            if (set.isEmpty()) {
                                ret = unRegisterListenerRemote(client.listenerType,
                                        sLocalListener);
                            }
                            sListenerMap.put(ListenerType.valueOf(client.listenerType), set);
                        }
                    }
                }
            }
            return ret;
        }

        public void ServerDied() {
            sLocalListener.onServerDied();
        }

        public class DataInfoListener extends DataListener {
            public DataInfoListener(String clsName) {
                super(clsName);
            }

            @Override
            public void onInfoChange(int type, int event, SceneData data) {
                synchronized (sListenerMap) {
                    HashSet<LocalListener> set = sListenerMap.get(ListenerType.valueOf(type));
                    if (set != null) {
                        for (LocalListener client : set) {
                            if (client != null) {
                                try {
                                    client.listener.onInfoChange(type, event, data);
                                } catch (RemoteException e) {
                                    Log.e(TAG, "SceneInfoRegistry RemoteException " + client
                                            + " event:" + event + " e:" + e);
                                    e.printStackTrace();
                                }
                            }
                        }
                    }
                }
            }

            @Override
            public void onServerDied() {
                HashSet<LocalListener> temp = new HashSet<>();
                synchronized (sListenerMap) {
                    for (HashSet<LocalListener> set : sListenerMap.values()) {
                        temp.addAll(set);
                    }
                }
                for (LocalListener client : temp) {
                    try {
                        client.listener.onServerDied();
                    } catch (RemoteException e) {
                        Log.e(TAG, "onServerDied RemoteException " + client + " e:" + e);
                        e.printStackTrace();
                    }
                }
                sListenerMap.clear();
            }
        }

        public final class LocalListener {
            public int listenerType = ListenerType.TYPE_UNKONW.value();
            public IDataListener listener = null;
            public String clsName = DES_EMPTY;

            public LocalListener(int listenerType, IDataListener listener, String clsName) {
                this.listenerType = listenerType;
                this.listener = listener;
                this.clsName = clsName;
            }

            @Override
            public int hashCode() {
                return listener.asBinder().hashCode();
            }

            @Override
            public boolean equals(Object obj) {
                return listener.asBinder().equals(((LocalListener) obj).listener.asBinder());
            }

            @Override
            public String toString() {
                return "LocalListener{listenerType=" + listenerType
                        + " listener=" + listener.asBinder() + " clsName=" + clsName + '}';
            }
        }
    }
}
