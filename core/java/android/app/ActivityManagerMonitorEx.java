// Copyright 2026 Picomisu contributors
// SPDX-License-Identifier: Apache-2.0

package android.app;

import android.os.Environment;
import android.os.RemoteException;
import android.os.StatFs;
import android.util.Singleton;

import java.io.File;

/**
 * Smartisan sysmonitor extension of {@link ActivityManager} ({@link ActivityManager#getMonitorEx()}):
 * client of the {@link IActivityManagerSysMoEx} interface of the activity manager.
 * Reconstructed from the PICO OS 5.13.7 factory framework.
 *
 * @hide
 */
public class ActivityManagerMonitorEx {
    private static final Singleton<IActivityManagerSysMoEx> sActivityManagerSmtSingleton =
            new Singleton<IActivityManagerSysMoEx>() {
                @Override
                protected IActivityManagerSysMoEx create() {
                    try {
                        return ActivityManager.getService().getMonitorEx();
                    } catch (RemoteException e) {
                        throw e.rethrowFromSystemServer();
                    }
                }
            };

    private static IActivityManagerSysMoEx getServiceMonitorEx() {
        return sActivityManagerSmtSingleton.get();
    }

    public static long getRomFreeMemoryKb() {
        File path = Environment.getDataDirectory();
        StatFs stat = new StatFs(path.getPath());
        long availableBytes = stat.getAvailableBytes();
        return availableBytes / 1024;
    }

    public void registerSysClient() {
        try {
            getServiceMonitorEx().registerSysClient(SysClient.getInstance());
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public boolean isForegroundProcess(String processName) throws RemoteException {
        return getServiceMonitorEx().isForegroundProcess(processName);
    }

    public String getPackageName(int pid) throws RemoteException {
        return getServiceMonitorEx().getPackageName(pid);
    }
}
