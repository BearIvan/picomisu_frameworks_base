// Copyright 2026 Picomisu contributors
// SPDX-License-Identifier: Apache-2.0
package com.android.server.wm;

import android.content.ComponentName;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.ServiceManager;
import android.os.SystemClock;
import android.util.Slog;

import com.android.server.SysDataSyncServiceManager;
import com.android.server.am.AppPersistentConnection;
import com.android.server.api.ApiLayerService;
import com.pvr.IPvrManagerService;

import java.util.ArrayList;
import java.util.List;

/**
 * PICO activity task manager service extension (factory PICO OS 5.13.7
 * com.android.server.wm.ExtActivityTaskManagerServiceImpl): API layer registration, the
 * default-display orientation gate, activity resuming messages to pvr_manager, usage events
 * for sysdata_sync and the persistent service connections requested through the API layer.
 */
public class ExtActivityTaskManagerServiceImpl implements IExtActivityTaskManagerService {
    private static final int CODE_UPDATE_PERSISTENT_CONNECTION = 10001;
    private static final String DESCRIPTOR = "android.app.IActivityTaskManager";
    private static final String TAG = "ActivityTaskManager";
    private static Handler sH;
    private static final HandlerThread sWorkerThread = new HandlerThread("Ext-ATMS");
    private ActivityTaskManagerService mBase;
    private final List<AppPersistentConnection> mPersistentConnections = new ArrayList<>();
    private IPvrManagerService mPvrManagerService;

    static {
        sWorkerThread.start();
        sH = new Handler(sWorkerThread.getLooper()) {
        };
    }

    public ExtActivityTaskManagerServiceImpl(ActivityTaskManagerService base) {
        mBase = base;
    }

    /** ActivityTaskManagerService constructor. */
    @Override
    public void init() {
        ApiLayerService.getInstance().setActivityTaskManagerService(mBase.mContext, mBase);
    }

    @Override
    public void onSystemReady() {
        mBase.getActivityStartController().getExt().onSystemReady();
    }

    /** ActivityTaskManagerService.setRequestedOrientation: VR apps on display 0 are ignored. */
    @Override
    public boolean isBelongsToDefaultDisplay(ActivityRecord r, int requestedOrientation) {
        if (r.getDisplayId() == 0) {
            Slog.i(TAG, "Prohibit VR app calling this interface, r : " + r
                    + ", requestedOrientation : " + requestedOrientation);
            return true;
        }
        return false;
    }

    /** ActivityTaskManagerService.finishActivity: tells pvr_manager what resumes next. */
    @Override
    public void sendResumingActivityMsg(ActivityRecord r) {
        if (mPvrManagerService == null || mPvrManagerService.asBinder() == null
                || !mPvrManagerService.asBinder().isBinderAlive()) {
            if (ServiceManager.getService("pvr_manager") != null) {
                mPvrManagerService = IPvrManagerService.Stub.asInterface(
                        ServiceManager.getService("pvr_manager"));
            } else {
                Slog.w(TAG, "pvr_manager has not been added to ServiceManager,do nothing.");
            }
        }
        if (mPvrManagerService != null) {
            try {
                ActivityRecord now = r.getActivityStack().getTopActivity();
                mPvrManagerService.sendPvrMessages("activity_status",
                        "activityResuming:" + now.packageName + "," + now.info.name);
            } catch (Exception e) {
                Slog.e(TAG, "mPvrManagerService sendPvrMessages error");
            }
        }
    }

    /** Resume, pause, stop and destroy usage events also go to sysdata_sync. */
    @Override
    public void updateActivityUsageStats(ActivityRecord activity, int event) {
        if (event == 2 || event == 1 || event == 24 || event == 16 || event == 15) {
            reportUsageEvent(event, activity);
        }
    }

    private void reportUsageEvent(int event, ActivityRecord activity) {
        if (activity == null || activity.app == null || activity.appToken == null
                || activity.getActivityInfo() == null) {
            return;
        }
        String className = activity.getActivityInfo().getComponentName().getClassName();
        int instance = activity.appToken.hashCode();
        int pid = activity.app.getPid();
        String pkg = activity.packageName;
        long timestamp = SystemClock.elapsedRealtime();
        SysDataSyncServiceManager.onUsageEvent(event, className, instance, pid, pkg, timestamp);
    }

    /** IApiLayer.updatePersistentServiceConnection: keep a service of an app bound. */
    @Override
    public void updatePersistentConnection(ComponentName componentName, boolean connect) {
        if (componentName == null) {
            return;
        }
        AppPersistentConnection connection = findPersistentConnection(componentName);
        if (connect) {
            if (connection == null) {
                AppPersistentConnection connection2 =
                        new AppPersistentConnection(mBase.mContext, sH, componentName);
                synchronized (mPersistentConnections) {
                    mPersistentConnections.add(connection2);
                }
                connection = connection2;
            }
            connection.bindClient();
            return;
        }
        if (connection != null) {
            synchronized (mPersistentConnections) {
                mPersistentConnections.remove(connection);
            }
            connection.unbindClient();
        }
    }

    private AppPersistentConnection findPersistentConnection(ComponentName componentName) {
        if (componentName == null) {
            return null;
        }
        synchronized (mPersistentConnections) {
            for (AppPersistentConnection conn : mPersistentConnections) {
                if (componentName.equals(conn.getComponentName())) {
                    return conn;
                }
            }
            return null;
        }
    }
}
