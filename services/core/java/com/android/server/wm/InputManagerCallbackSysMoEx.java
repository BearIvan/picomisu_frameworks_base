// Copyright 2026 Picomisu contributors
// SPDX-License-Identifier: Apache-2.0

package com.android.server.wm;

import android.os.IBinder;
import com.android.server.am.SysMonitorSvcBridge;

/**
 * Reconstructed from the PICO OS 5.13.7 factory services: InputManagerCallback.notifyANR passes
 * the input monitor ANRs (reason "Monitor ...") to the sysmonitor ANR monitor.
 *
 * @hide
 */
public class InputManagerCallbackSysMoEx {
    public static InputManagerCallbackSysMoEx getInstance() {
        return INSTANCE.instance;
    }

    private static class INSTANCE {
        private static InputManagerCallbackSysMoEx instance = new InputManagerCallbackSysMoEx();

        private INSTANCE() {
        }
    }

    private InputManagerCallbackSysMoEx() {
    }

    public void monitorAnr(WindowManagerService service, IBinder token, String reason) {
        AppWindowToken appWindowToken = null;
        WindowState windowState = null;
        synchronized (service.mGlobalLock) {
            if (token != null) {
                windowState = service.windowForClientLocked((Session) null, token, false);
                if (windowState != null) {
                    appWindowToken = windowState.mAppToken;
                }
            }
        }
        int pid = -1;
        if (appWindowToken != null && appWindowToken.appToken != null) {
            pid = windowState != null ? windowState.mSession.mPid : -1;
            ActivityRecord record = appWindowToken.mActivityRecord;
            if (pid == -1 && record != null && record.app != null) {
                pid = record.app.getPid();
            }
        } else if (windowState != null) {
            pid = windowState.mSession.mPid;
        }
        if (pid > 0) {
            SysMonitorSvcBridge.getFactory().getAnrMonitor().monitorInput(pid, reason);
        }
    }
}
