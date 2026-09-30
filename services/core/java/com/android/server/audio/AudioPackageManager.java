// Copyright 2026 Picomisu contributors
// SPDX-License-Identifier: Apache-2.0

package com.android.server.audio;

import android.app.ActivityManager;
import android.os.RemoteException;
import android.util.Log;

import java.util.List;

/**
 * Resolves the package name of an audio client process. Reconstructed from the PICO OS 5.13.7
 * factory services; asks the Smartisan sysmonitor extension of the activity manager first
 * ({@link ActivityManager#getMonitorEx()}) and falls back to the running app processes.
 */
public class AudioPackageManager {
    private static final String TAG = "AudioPackageManager";

    private AudioPackageManager() {
    }

    static String getPackageName(int uid, int pid) {
        String packageName = "";
        try {
            packageName = ActivityManager.getMonitorEx().getPackageName(pid);
            if (packageName != null && !packageName.isEmpty()) {
                return packageName;
            }
            List<ActivityManager.RunningAppProcessInfo> processInfos =
                    ActivityManager.getService().getRunningAppProcesses();
            if (processInfos == null) {
                Log.w(TAG, "getProcessName failed for uid/pid " + uid + "/" + pid);
                return packageName;
            }
            for (ActivityManager.RunningAppProcessInfo info : processInfos) {
                if (info.pid == pid) {
                    if (info.pkgList != null && info.pkgList.length == 1) {
                        packageName = info.pkgList[0];
                        break;
                    }
                    int colonIndex = info.processName.lastIndexOf(":");
                    if (colonIndex != -1) {
                        packageName = info.processName.substring(0, colonIndex);
                    } else {
                        packageName = info.processName;
                    }
                    break;
                }
            }
        } catch (RemoteException e) {
        }
        return packageName;
    }
}
