// Copyright 2026 Picomisu contributors
// SPDX-License-Identifier: Apache-2.0

package android.os;

import smartisanos.util.FeatLog;

import java.io.FileInputStream;
import java.io.IOException;

/**
 * Smartisan process helpers. Reconstructed from the PICO OS 5.13.7 factory framework; the
 * factory setProcessGroupAll(int, int, int) is not present because it needs the Smartisan
 * native Process.getChildProcessViaGroup(int, int), which the Source framework lacks.
 *
 * @hide
 */
public class ProcessSmtEx {
    private static final String TAG = "ProcessSmtEx";

    public static final int PREFETCH_STATS_NONE = 0;
    public static final int PREFETCH_STATS_LAUNCH = 1;
    public static final int PREFETCH_STATS_EXECUTE = 2;
    public static final int PREFETCH_STATS_DONE = 3;
    public static final int PREFETCH_STATS_FREEZE = 4;

    public static final int THREAD_GROUP_CLUSTER_BIG = 8;
    public static final int THREAD_GROUP_CLUSTER_SUPER = 9;
    public static final int THREAD_GROUP_DEX2OAT = 10;
    public static final int THREAD_GROUP_APP_INSHELL = 11;
    public static final int THREAD_GROUP_SHELL_APP = 12;
    public static final int THREAD_GROUP_VRFOREGROUND = 13;
    public static final int THREAD_GROUP_COMPOSITOR = 14;
    public static final int THREAD_GROUP_BG_3RD_APP = -10;
    public static final int THREAD_GROUP_SP_PREFETCH_VR_APP = -15;

    protected static boolean mIsUserDebug = "userdebug".equals(SystemProperties.get(
            "ro.build.type"));
    public static boolean isDebugApp = true;
    private static byte[] sBuffer = new byte[2048];
    private static String sDex2oatCmd = "/dex2oat";

    public static boolean getDebug() {
        return mIsUserDebug;
    }

    public static void setDebug(boolean debug) {
        mIsUserDebug = debug;
    }

    public static boolean isDebugApp() {
        return isDebugApp;
    }

    public static void setIsDebugApp(boolean isDebug) {
        FeatLog.d(TAG, "FEAT_LOG_CONTROL", 50, "isDebug = " + isDebug);
        isDebugApp = isDebug;
    }

    public static String getProcCmdLine(int pid, byte[] buffer) {
        String cmdline = null;
        FileInputStream is = null;
        if (buffer == null) {
            buffer = new byte[2048];
        }
        try {
            is = new FileInputStream("/proc/" + pid + "/comm");
            int count = is.read(buffer);
            if (count > 0) {
                cmdline = new String(buffer, 0, count - 1);
            }
        } catch (IOException e) {
        } finally {
            if (is != null) {
                try {
                    is.close();
                } catch (IOException e) {
                }
            }
        }
        return cmdline;
    }
}
