// Copyright 2026 Picomisu contributors
// SPDX-License-Identifier: Apache-2.0

package android.app;

import android.net.ConnectivityManagerSmtEx;
import android.os.Binder;
import android.os.Message;
import android.os.Parcel;
import android.os.ParcelFileDescriptor;
import android.os.Process;
import android.os.ProcessSmtEx;
import android.os.RemoteException;
import android.os.SystemProperties;
import android.os.Trace;
import android.util.Log;
import android.view.WindowManagerGlobal;

import com.android.internal.os.AppIdToPackageMap;
import com.android.internal.os.BinderCallsStats;
import com.android.internal.os.CachedDeviceState;
import com.android.internal.util.FastPrintWriter;

import smartisanos.util.FeatLog;

import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.util.List;

/**
 * Smartisan extension state of an {@link ActivityThread} (its {@code mSmtEx}): prefetched
 * (pre-started) processes, unfreeze handling and the Smartisan {@link IApplicationThread}
 * transactions. Reconstructed from the PICO OS 5.13.7 factory framework.
 *
 * @hide
 */
public class ActivityThreadSmtBase {
    public static final boolean DEBUG_PREFETCH = true;
    public static final int COMPLETE_PREFETCH_BIND_APPLICATION = 1011;
    private static final int NOTIFY_MONITOR_STATS_CHANGED = 1021;
    public static final int DEBUG_APP_SMARTISAN = 1022;
    private static final int ENABLE_BINDER_TRANS = 1023;
    private static final int LOG_BINDER_TRANS = 1024;
    private static final int MAIN_THREAD_TRACE = 1025;
    private static final int MAIN_THREAD_SLOW_START_MONITOR = 1026;
    private static final int GET_MAIN_THREAD_SLOW_OPERATION = 1027;
    private static final int NOTIFY_PROCESS_UNFREEZE = 1029;
    public static final int BINDER_STATS_CODE = 1030;
    public static final int BINDER_STATS_SET_SAMPLING = 1031;
    public static final int PROCESS_UNFREEZE = 3000;

    static final boolean DEBUG_LOG_CONTROL =
            SystemProperties.getBoolean("debug.bytedance.logcontrol.validation", false);

    /** Bind data of a prefetched (pre-started) process, bound when it is really started. */
    ActivityThread.AppBindData mPrefetchData = null;
    ContextImpl mPrefetchAppContext = null;
    boolean mFreezePrefetch = false;
    protected long startTime = 0;
    protected ActivityThread mActivityThread;
    private BinderCallsStats mBinderCallStats;

    public ActivityThreadSmtBase(ActivityThread activityThread) {
        mActivityThread = activityThread;
    }

    /** Smartisan part of {@link ActivityThread.AppBindData}. */
    static final class AppBindDataSmtEx {
        boolean doPrefetch = false;
        long startSeq = 0;
    }

    public void handleBindApplication(boolean isPrefetch) {
        initPrefetch(isPrefetch);
        registerStartPrefetchListener();
    }

    private void updateSwitchState() {
        int state = SystemProperties.getInt("persist.sys.scene.freq", 0);
        mActivityThread.nSetSwitchState(0, state);
    }

    public void onUnFreeze() {
        ConnectivityManagerSmtEx.clearActiveNetworkInfoCache();
        updateSwitchState();
    }

    void handleBindApplicationEnd(ActivityThread.AppBindData data) {
        if (mFreezePrefetch) {
            mFreezePrefetch = false;
            ActivityManager.getSmtEx().freezePrefetchApp();
            FeatLog.d("ActivityThread", "FEAT_PERF_PREFETCH", 50,
                    "client: call freeze on current app = " + data.appInfo.packageName);
        }
        mPrefetchData = null;
        mPrefetchAppContext = null;
    }

    public void handleMessage(Message msg) {
        switch (msg.what) {
            case COMPLETE_PREFETCH_BIND_APPLICATION:
                try {
                    if (mPrefetchData != null) {
                        mPrefetchData.getSmtEx().startSeq = (Long) msg.obj;
                        mActivityThread.handleBindApplication(mPrefetchData);
                    }
                } catch (Exception e) {
                }
                break;
            case PROCESS_UNFREEZE:
                onUnFreeze();
                break;
        }
    }

    void setBinderCallsStats(String procName, int sampling) {
        mBinderCallStats = new BinderCallsStats(new BinderCallsStats.Injector());
        mBinderCallStats.setProcessName(procName);
        CachedDeviceState deviceState = new CachedDeviceState(false, false);
        mBinderCallStats.setDeviceState(deviceState.new Readonly());
        mBinderCallStats.setSamplingInterval(sampling);
        Binder.setObserver(mBinderCallStats);
    }

    /** Smartisan part of the {@link ActivityThread.ApplicationThread} binder. */
    public class ApplicationThreadEx {
        private ActivityThread.ApplicationThread mApplicationThread;

        public ApplicationThreadEx(ActivityThread.ApplicationThread applicationThread) {
            mApplicationThread = applicationThread;
        }

        public boolean onTransactEx(int code, Parcel data, Parcel reply, int flags) {
            switch (code) {
                case NOTIFY_MONITOR_STATS_CHANGED: {
                    boolean open = data.readInt() > 0;
                    WindowManagerGlobal.getInstance().notifyMonitorStatsChanged(open);
                    return true;
                }
                case ENABLE_BINDER_TRANS: {
                    boolean enable = data.readInt() > 0;
                    int mode = data.readInt();
                    return true;
                }
                case LOG_BINDER_TRANS: {
                    int maxUid = data.readInt();
                    int mode = data.readInt();
                    return true;
                }
                case MAIN_THREAD_TRACE: {
                    mActivityThread.mLooper.setTraceTag(Trace.TRACE_TAG_ACTIVITY_MANAGER);
                    Log.d("ActivityThread", "Open activity main looper trace.");
                    return true;
                }
                case MAIN_THREAD_SLOW_START_MONITOR: {
                    mActivityThread.mLooper.getSmtEx().setSlowDispatchThresholdMs(16);
                    return true;
                }
                case GET_MAIN_THREAD_SLOW_OPERATION: {
                    List<String> mainOperations =
                            mActivityThread.mLooper.getSmtEx().getSlowOperations();
                    if (mainOperations.size() > 0) {
                        int index = data.readInt();
                        try {
                            ActivityManager.getSmtEx().setAppSlowMainOperations(mainOperations,
                                    index);
                        } catch (Exception e) {
                        }
                    }
                    return true;
                }
                case DEBUG_APP_SMARTISAN: {
                    int debug = data.readInt();
                    FeatLog.d("ActivityThread", "FEAT_LOG_CONTROL", 0,
                            "onTransact DEBUG_APP_SMARTISAN debug= " + debug);
                    ProcessSmtEx.setIsDebugApp(debug == 1);
                    return true;
                }
                case NOTIFY_PROCESS_UNFREEZE: {
                    mActivityThread.sendMessage(PROCESS_UNFREEZE, null);
                    return true;
                }
                case BINDER_STATS_CODE: {
                    if (mBinderCallStats != null) {
                        FileOutputStream fout = new FileOutputStream(
                                data.readRawFileDescriptor());
                        PrintWriter pw = new FastPrintWriter(fout);
                        mBinderCallStats.dump(pw, AppIdToPackageMap.getSnapshot(), false);
                        pw.println("\n");
                        pw.flush();
                    }
                    return true;
                }
                case BINDER_STATS_SET_SAMPLING: {
                    if (mBinderCallStats != null) {
                        mBinderCallStats.setSamplingInterval(data.readInt());
                    }
                    return true;
                }
                case ActivityThreadMonitorEx.NOTIFY_SWITCH_STATE: {
                    int offset = data.readInt();
                    int enable = data.readInt();
                    mActivityThread.nSetSwitchState(offset, enable);
                    return true;
                }
            }
            return false;
        }

        public void scheduleMethodTrace(int type, long flags, String cmd,
                ParcelFileDescriptor fd) {
        }

        public void configArtTracer(String[] state) {
            SysFwBridge.getFactory().getArtTracerUtils().startArtTracer(state,
                    mActivityThread.mInitialApplication.getApplicationContext());
        }

        public final void completePrefetchBindApplication(long startSeq) {
            FeatLog.d("ActivityThread", "FEAT_PERF_PREFETCH", 30,
                    "c/s: execute complete prefetch bind application for process = "
                    + mPrefetchData.appInfo.packageName);
            mActivityThread.sendMessage(COMPLETE_PREFETCH_BIND_APPLICATION, startSeq);
            if (mPrefetchData != null) {
                final IActivityManager mgr = ActivityManager.getService();
                try {
                    mgr.attachApplication(mActivityThread.getApplicationThread(), startSeq);
                } catch (RemoteException ex) {
                    throw ex.rethrowFromSystemServer();
                }
            }
        }
    }

    private void registerStartPrefetchListener() {
        PrefetchRegister.getInstance().registerStartPrefetchListener(Process.myPid(),
                new PrefetchRegister.PrefetchCallbackLocal() {
                    @Override
                    public void onRealStart(int pid) {
                        onPrefetchRealStart(false);
                    }
                });
    }

    private native void initPrefetch(boolean isPrefetch);

    private native void onPrefetchRealStart(boolean realStart);
}
