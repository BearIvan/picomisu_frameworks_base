// Copyright 2026 Picomisu contributors
// SPDX-License-Identifier: Apache-2.0

package android.app;

import android.os.Process;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.util.Singleton;
import android.util.SmtSysLog;

import java.util.List;

/**
 * Smartisan extension of {@link ActivityManager} ({@link ActivityManager#getSmtEx()}): client of
 * the {@link IActivityManagerSmtEx} interface of the activity manager and of the Smartisan
 * "prefetch" service ({@link IPrefetchManager}). Reconstructed from the PICO OS 5.13.7 factory
 * framework.
 *
 * @hide
 */
public class ActivityManagerSmtBase {
    public static final String PREFETCH_SERVICE = "prefetch";
    public static final int RECENT_INGORE_ACTIVITY_STACK_VIEW = 16;
    public static final int UID_OBSERVER_SCHEDGROUP = 1 << 16;
    public static final int UID_OBSERVER_FROZEN = 1 << 17;

    /** Called when the prefetched process registered by the caller is really started. */
    public interface StartPrefetchCallback {
        void onRealStart(Object remotePid, boolean realStart);
    }

    private static final Singleton<IActivityManagerSmtEx> sActivityManagerSmtSingleton =
            new Singleton<IActivityManagerSmtEx>() {
                @Override
                protected IActivityManagerSmtEx create() {
                    try {
                        return ActivityManager.getService().getISmtEx();
                    } catch (RemoteException e) {
                        throw e.rethrowFromSystemServer();
                    }
                }
            };

    private static final Singleton<IPrefetchManager> IPrefetchManagerSingleton =
            new Singleton<IPrefetchManager>() {
                @Override
                protected IPrefetchManager create() {
                    return IPrefetchManager.Stub.asInterface(
                            ServiceManager.getService(PREFETCH_SERVICE));
                }
            };

    protected static IActivityManagerSmtEx getServiceSmtEx() {
        return sActivityManagerSmtSingleton.get();
    }

    public void setAppSlowMainOperations(List<String> slowOperations, int index) {
        try {
            getServiceSmtEx().setAppSlowMainOperations(slowOperations, index);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public long getRomFreeMemoryKb() throws RemoteException {
        return getServiceSmtEx().getRomFreeMemoryKb();
    }

    public String getSmtExtraInfo(int pid) {
        try {
            return getServiceSmtEx().getSmtExtraInfo(pid);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        return null;
    }

    public String getLastword(int pid) {
        try {
            return getServiceSmtEx().getLastword(pid);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public void setSmtExtraInfo(String info) {
        int pid = Process.myPid();
        try {
            getServiceSmtEx().setSmtExtraInfo(pid, info);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    public void freezePrefetchApp() {
        try {
            getServiceSmtEx().freezePrefetchApp();
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public void registerSysClient() {
        try {
            getServiceSmtEx().registerSysClient(SysClient.getInstance());
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public void setProcessRunningCpuset(int pid, int cpusetLevel, long timeOut, boolean force) {
        try {
            getServiceSmtEx().setProcessRunningCpuset(pid, cpusetLevel, timeOut, force);
        } catch (RemoteException e) {
            SmtSysLog.fatal("ActivityManagerSmtBase", "set process running cpuset error!", e);
        }
    }

    public boolean getUidFrozen(int uid) {
        try {
            return getServiceSmtEx().getUidFrozen(uid);
        } catch (RemoteException e) {
            return false;
        }
    }

    public void createHprof(int pid, String processName, IMemClient client, long dalvikAlloc,
            long dalvikMax) {
        try {
            getServiceSmtEx().createHprof(pid, processName, client, dalvikAlloc, dalvikMax);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    public void cropHprofDone(String path, boolean delete) {
        try {
            getServiceSmtEx().cropHprofDone(path, delete);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    public void checkHprof() {
        try {
            getServiceSmtEx().checkHprof();
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    public void registerActivityLifeCycleObserver(IActivityLifeCycleObserver observer) {
        try {
            getServiceSmtEx().registerActivityLifeCycleObserver(observer);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public void unregisterActivityLifeCycleObserver(IActivityLifeCycleObserver observer) {
        try {
            getServiceSmtEx().unregisterActivityLifeCycleObserver(observer);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public void registerAppStartEventObserver(IAppStartEventObserver observer) {
        try {
            getServiceSmtEx().registerAppStartEventObserver(observer);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public void unregisterAppStartEventObserver(IAppStartEventObserver observer) {
        try {
            getServiceSmtEx().unregisterAppStartEventObserver(observer);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public void updatePrefetchApps(List<String> packages, int flag) {
        try {
            getServiceSmtEx().updatePrefetchApps(packages, flag);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public void killMemoryLeakProcess(String processName, int pid) {
        try {
            getServiceSmtEx().killMemoryLeakProcess(processName, pid);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public void backtraceDoneInform(String processName, int pid) {
        try {
            getServiceSmtEx().backtraceDoneInform(processName, pid);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public void clearAllBg() {
        try {
            getServiceSmtEx().clearAllKeepAliveProc();
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public boolean isScreenOn() {
        try {
            return getServiceSmtEx().isScreenOn();
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public void forceStopPackageSmart(String packageName, int userId, int taskId,
            int cleanLevel) {
        try {
            getServiceSmtEx().forceStopPackageSmart(packageName, userId, taskId, cleanLevel);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public void frozenObjectFromNative(ApplicationErrorReport.ParcelableCrashInfo crashInfo) {
        try {
            getServiceSmtEx().frozenObjectFromNative(crashInfo);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public boolean isPrefetch(String packageName) {
        try {
            return IPrefetchManagerSingleton.get().isPrefetch(packageName);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public void registerPrefetchObserver(IPrefetchObserver observer) {
        try {
            IPrefetchManagerSingleton.get().registerPrefetchObserver(observer);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public void unregisterPrefetchObserver(IPrefetchObserver observer) {
        try {
            IPrefetchManagerSingleton.get().unregisterPrefetchObserver(observer);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public void registerStartPrefetchListener(int remotePid, final StartPrefetchCallback callBack) {
        try {
            IPrefetchManagerSingleton.get().registerStartPrefetchListener(
                    new IPrefetchCallback.Stub() {
                        @Override
                        public void onRealStart(int remotePid) {
                            callBack.onRealStart(remotePid, true);
                        }
                    });
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }
}
