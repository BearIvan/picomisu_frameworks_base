// Copyright 2026 Picomisu contributors
// SPDX-License-Identifier: Apache-2.0

package com.android.server.am;

import android.app.ActivityManagerInternalSmtBase;
import android.app.AppGlobals;
import android.app.ApplicationErrorReport;
import android.app.IActivityLifeCycleObserver;
import android.app.IActivityManagerSmtEx;
import android.app.IAppStartEventObserver;
import android.app.IMemClient;
import android.app.ISysClient;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.IPackageDataObserver;
import android.content.pm.IPackageManager;
import android.content.pm.PackageInstaller;
import android.content.pm.ResolveInfo;
import android.os.AsyncTask;
import android.os.Binder;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.FileUtils;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.Parcel;
import android.os.ParcelFileDescriptor;
import android.os.PowerManager;
import android.os.Process;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.StatFs;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.util.ArrayMap;
import android.util.ArraySet;
import android.util.AtomicFile;
import android.util.Slog;
import android.util.Xml;
import com.android.internal.app.IBatteryStats;
import com.android.internal.app.ProcessMap;
import com.android.internal.os.BackgroundThread;
import com.android.internal.util.FastXmlSerializer;
import com.android.internal.util.MemInfoReader;
import com.android.server.IActivityManagerOptEx;
import com.android.server.ISysPerfMonitorService;
import com.android.server.ITransferController;
import com.android.server.LocalServices;
import com.android.server.ServiceThread;
import com.android.server.SysOptBridge;
import com.android.server.SystemServiceManager;
import com.android.server.TransferInternal;
import com.android.server.am.ActivityManagerService.PidMap;
import com.android.server.job.controllers.JobStatus;
import com.android.server.notification.NotificationShellCmd;
import com.android.server.pm.Settings;
import com.android.server.wm.ActivityRecord;
import com.android.server.wm.ActivityTaskManagerInternal;
import com.android.server.wm.ActivityTaskManagerService;
import com.android.server.wm.WindowProcessController;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.ToIntFunction;
import org.xmlpull.v1.XmlPullParser;
import smartisanos.api.ApplicationInfoSmt;
import smartisanos.os.BinderCallCacheAgent;
import smartisanos.util.FeatLog;

/**
 * Reconstructed from the PICO OS 5.13.7 factory services.
 *
 * @hide
 */
public class ActivityManagerServiceSmtBase {
    static final int BLOCK_LOGO_STAGE_HOME_CRASH = 8;
    static final int CHECK_HOME_APP_CRASH_MIN_COUNT = 6;
    static final long CHECK_HOME_APP_CRASH_TIME_MS = 60000;
    private static final long CHECK_TASK_DEFAULT_TIME = 1000000;
    private static final long CRITICAL_APP_CRASH_COUNT = 5;
    private static final long CRITICAL_APP_CRASH_TIME_MS = 50000;
    static final String CUSTOM_ERROR_TYPE_FROZEN_OBJECT = "process_frozen";
    static final String FROZEN_OBJECT_TAG = "FrozenObject";
    static final int HARD_CLEAN_TIMEOUT = 1002;
    static final int KILL_APP_SET_MSG = 2004;
    private static final long MSG_CHECK_STABD_APP_START_MIN_TIME_MS = 15000;
    private static final int MSG_NOTIFY_CLEAR_APP_DATA = 9;
    private static final int MSG_NOTIFY_STABD_CRITICAL_APP_RESTORE = 10;
    private static final long MSG_NOTIFY_STABD_CRITICAL_APP_RESTORE_TIMEOUT = 60000;
    private static final int MSG_NOTIFY_UNINSTALL_APP = 8;
    protected static final String TAG = "ActivityManagerService";
    static final String TAG_FREEZE = "ActivityManagerService";
    static final int UPDATE_HAS_ONGOING_NOTI = 303;
    static final int UPDATE_OOM_MSG = 2001;
    static final int UPDATE_OOM_TIME = 50;
    protected static IActivityManagerOptEx mSmtOptEx = null;
    private static ActivityManagerServiceSysMoEx.CpuStateProvider sCpuStateProvider = null;
    static final int sSystemMask = 129;
    protected static UidCpuUsageProvider sUidCpuUsageProvider;
    private final int NOTIFY_DONE;
    private final int NOTIFY_INVALID;
    private final int NOTIFY_PARCEL_DATA;
    private final int NOTIFY_STAT_BUFF;
    private IKillingStats killStats;
    protected ActivityManagerService mActivityManagerService;
    boolean mBDReceiverStatsEnabled;
    IBinderStat mBinderStat;
    long mCachedPss;
    protected final AtomicFile mChainBootBlackListFile;
    private Runnable mCheckForceCpusetProcTask;
    protected boolean mCleaningProcesses;
    ProcessRecord mFocusedApp;
    private HashMap<Integer, ForceCpusetProc> mForceCpusetProcs;
    public boolean mHardClean;
    private int mHomeAppCrashCount;
    private long mHomeAppCrashedTime;
    private IActivityManagerSmtEx mIActivityManagerSmtEx;
    private boolean mInitMonitorCriticalAppData;
    String mLaunchingProcessName;
    ArrayMap<String, AppClearAndStatData> mMapAppDiedStatData;
    public IMemoryProcessController mMemProcessController;
    private Handler mMonitorAppRestoreHandler;
    private HandlerThread mMonitorAppRestoreThread;
    protected long mNextUpdateOomTime;
    List<Integer> mPendingFreezePids;
    private boolean mPostedCheckCpusetTask;
    public HashMap<Integer, String> mPrefetchApps;
    protected ActivityManagerServiceSysMoEx.CpuStateObserver mPrefetchCpuObServer;
    ActivityManagerService.PidMap mPrefetchPidsSelf;
    IProcessIntercept mProcessIntercept;
    HashMap mProcessMems;
    int mSetupWizardState;
    public WindowProcessController mSmartisanHomeProcess;
    public int mStrictModeFlags;
    long mTotalEGL;
    long mTotalPss;
    ITransferController mTransferService;
    public static final boolean IS_USER_BUILD = "user".equals(Build.TYPE);
    public static final String DEX_FILE_OPT = "persist.sys.dexfile.opt";
    public static int isDexFileOptOn = SystemProperties.getInt(DEX_FILE_OPT, 0);
    private static ArraySet<String> mOverrideClazzCrashProcs = new ArraySet<>(4);
    private static ArraySet<String> mOverrideClazzsFiles = new ArraySet<>(4);
    private static String mPackageCache = null;

    public static class UidCpuInfos {
        public long checkTime;
        ArrayList<UidCpuInfo> infos;
    }

    public interface UidCpuUsageObserver {
        void onForeUidCpuInfo(int i, int i2, long j);

        void onUidCpuInfo(ArrayList<UidCpuInfo> arrayList, long j, long j2);
    }

    static {
        mOverrideClazzsFiles.add("ControllerClient.java");
        mOverrideClazzsFiles.add("CvOTACb.java");
        mOverrideClazzsFiles.add("CvStateCb.java");
        mOverrideClazzsFiles.add("CvStateCb.java");
    }

    public void cropHprofDone(String path, boolean delete) {
        SysOptBridge.getFactory().getMemMonitor().cropHprofDone(path, delete);
    }

    public void checkHprof() {
        SysOptBridge.getFactory().getMemMonitor().checkHprof();
    }

    public void createHprof(int pid, String processName, IMemClient client, long dalvikAlloc, long dalvikMax) {
        SysOptBridge.getFactory().getMemMonitor().createHprof(pid, processName, client, dalvikAlloc, dalvikMax);
    }

    enum reportEventCode {
        UNKNOWN(0),
        LOADING(ActivityTaskManagerService.KEY_DISPATCHING_TIMEOUT_MS),
        BLACKSCREEN(5001),
        TEARSCREEN_VSYNC(5010),
        TEARSCREEN_COST_ABNORMAL(5010),
        TEARSCREEN_PARAM(5010);

        private int value;

        public int getValue() {
            return this.value;
        }

        reportEventCode(int code) {
            this.value = code;
        }
    }

    void collectCachedPss(ProcessRecord app) {
        long egl;
        long gl;
        long pss;
        String processName = app.processName;
        long pid = app.getSmtEx().pid;
        if (app.getSmtEx().memoryInfo != null) {
            egl = app.getSmtEx().memoryInfo.getOtherPss(14);
            gl = app.getSmtEx().memoryInfo.getOtherPss(15);
            pss = app.getSmtEx().memoryInfo.getTotalPss();
        } else {
            egl = app.getSmtEx().mEGL;
            gl = app.getSmtEx().mGL;
            pss = app.getSmtEx().mPss;
        }
        int oomAdj = app.getSetAdjWithServices();
        if (oomAdj >= 900) {
            this.mCachedPss += pss;
        }
        this.mTotalPss += pss;
        this.mTotalEGL += egl;
        long[] values = {pid, pss, egl, gl, oomAdj};
        this.mProcessMems.put(processName, values);
    }

    void collectCachedPss(String processName, int pid, long pss, long egl, long gl, int oomAdj, boolean javaProcess) {
        if (javaProcess && oomAdj >= 900) {
            this.mCachedPss += pss;
        }
        this.mTotalPss += pss;
        this.mTotalEGL += egl;
        long[] values = {pid, pss, egl, gl, oomAdj};
        this.mProcessMems.put(processName, values);
    }

    void resetProcStatsCollectData() {
        this.mTotalPss = 0L;
        this.mCachedPss = 0L;
        this.mTotalEGL = 0L;
        this.mProcessMems.clear();
    }

    public ApplicationInfo getTopApplication() {
        return this.mActivityManagerService.mActivityTaskManager.getSmtEx().getTopApplication();
    }

    public IProcessIntercept getProcessInterceptInstance() {
        return this.mProcessIntercept;
    }

    public ApplicationInfo getTopApp() {
        ApplicationInfo applicationInfo;
        synchronized (this.mActivityManagerService) {
            try {
                ActivityManagerService.boostPriorityForLockedSection();
                ActivityTaskManagerInternal atm = this.mActivityManagerService.mAtmInternal;
                WindowProcessController wpc = atm != null ? atm.getTopApp() : null;
                applicationInfo = wpc != null ? wpc.mInfo : null;
            } finally {
                ActivityManagerService.resetPriorityAfterLockedSection();
            }
        }
        return applicationInfo;
    }

    protected void readChainBootBlackList() {
        int type;
        String actionName;
        File file = this.mChainBootBlackListFile.getBaseFile();
        if (!file.exists()) {
            try {
                file.createNewFile();
                return;
            } catch (IOException e) {
                return;
            }
        }
        try {
            try {
                FileInputStream fis = this.mChainBootBlackListFile.openRead();
                try {
                    try {
                        if (ActivityManagerDebugConfigSmtEx.DEBUG_WIFI_UPLOAD) {
                            Slog.i("ActivityManagerService", "readWarnedWifiPackages: start parse");
                        }
                        XmlPullParser parser = Xml.newPullParser();
                        parser.setInput(fis, null);
                        do {
                            type = parser.next();
                            if (type == 2) {
                                break;
                            }
                        } while (type != 1);
                        if (type != 2) {
                            Slog.w("ActivityManagerService", "no start tag found for warning");
                            try {
                                fis.close();
                                return;
                            } catch (IOException e2) {
                                return;
                            }
                        }
                        int outerDepth = parser.getDepth();
                        while (true) {
                            int type2 = parser.next();
                            if (type2 == 1 || (type2 == 3 && parser.getDepth() <= outerDepth)) {
                                break;
                            }
                            if (type2 != 3 && type2 != 4) {
                                String name = parser.getName();
                                if (ActivityManagerDebugConfigSmtEx.DEBUG_CHAINBOOT_BLACKLIST) {
                                    Slog.i("ActivityManagerService", "readChainBootBlackList: parse name=" + name);
                                }
                                synchronized (this.mActivityManagerService) {
                                    try {
                                        ActivityManagerService.boostPriorityForLockedSection();
                                        if ("class".equals(name)) {
                                            String componentName = parser.getAttributeValue(null, Settings.ATTR_NAME);
                                            if (componentName != null) {
                                                if (ActivityManagerDebugConfigSmtEx.DEBUG_CHAINBOOT_BLACKLIST) {
                                                    Slog.i("ActivityManagerService", "readChainBootBlackList: parse package=" + componentName);
                                                }
                                                this.mProcessIntercept.getPushServiceNames().add(componentName);
                                            }
                                        } else if ("action".equals(name) && (actionName = parser.getAttributeValue(null, Settings.ATTR_NAME)) != null) {
                                            if (ActivityManagerDebugConfigSmtEx.DEBUG_CHAINBOOT_BLACKLIST) {
                                                Slog.i("ActivityManagerService", "readChainBootBlackList: parse action=" + actionName);
                                            }
                                            this.mProcessIntercept.getPushServiceActions().add(actionName);
                                        }
                                    } catch (Throwable th) {
                                        ActivityManagerService.resetPriorityAfterLockedSection();
                                        throw th;
                                    }
                                }
                                ActivityManagerService.resetPriorityAfterLockedSection();
                            }
                        }
                        fis.close();
                    } catch (Throwable th2) {
                        try {
                            fis.close();
                        } catch (IOException e3) {
                        }
                        throw th2;
                    }
                } catch (Exception e4) {
                    Slog.w("ActivityManagerService", "Failed parsing " + e4);
                    fis.close();
                }
            } catch (IOException e5) {
            }
        } catch (FileNotFoundException e6) {
            if (ActivityManagerDebugConfigSmtEx.DEBUG_WIFI_UPLOAD) {
                Slog.i("ActivityManagerService", "readWarnedWifiPackages file not found");
            }
        }
    }

    protected ActivityManagerServiceSmtBase(ActivityManagerService ams) {
        this.mCachedPss = 0L;
        this.mTotalPss = 0L;
        this.mTotalEGL = 0L;
        this.mProcessMems = new HashMap();
        this.mSetupWizardState = -1;
        this.mStrictModeFlags = 0;
        this.mBinderStat = null;
        this.NOTIFY_INVALID = -1;
        this.NOTIFY_STAT_BUFF = 1;
        this.NOTIFY_PARCEL_DATA = 2;
        this.NOTIFY_DONE = 3;
        this.mLaunchingProcessName = null;
        this.killStats = SysOptBridge.getFactory().getKillingStats();
        this.mMapAppDiedStatData = new ArrayMap<>();
        this.mInitMonitorCriticalAppData = false;
        this.mNextUpdateOomTime = JobStatus.NO_LATEST_RUNTIME;
        this.mHardClean = false;
        this.mPendingFreezePids = new ArrayList();
        this.mPrefetchCpuObServer = new ActivityManagerServiceSysMoEx.CpuStateObserver() {
            ArrayList<String> apps_l = new ArrayList<>();

            @Override
            public void onCpuState(ActivityManagerServiceSysMoEx.CpuStateObserver.CPU_USAGE_STATE state, long timestamp) {
                if (state == ActivityManagerServiceSysMoEx.CpuStateObserver.CPU_USAGE_STATE.CPU_NORMAL) {
                    SysOptBridge.getFactory().getSysPrefetchService().startPrefetchApp();
                }
            }

            @Override
            public ActivityManagerServiceSysMoEx.CpuStateObserver.NOTIFY_FREQUENCY getNotifyRequest() {
                return ActivityManagerServiceSysMoEx.CpuStateObserver.NOTIFY_FREQUENCY.EVERY_TIME;
            }
        };
        this.mForceCpusetProcs = new HashMap<>();
        this.mPostedCheckCpusetTask = false;
        this.mCheckForceCpusetProcTask = new Runnable() {
            @Override
            public void run() {
                synchronized (ActivityManagerServiceSmtBase.this.mForceCpusetProcs) {
                    if (ActivityManagerServiceSmtBase.this.mForceCpusetProcs.size() > 0) {
                        ActivityManagerServiceSmtBase.this.mPostedCheckCpusetTask = false;
                        long messageDelay = ActivityManagerServiceSmtBase.CHECK_TASK_DEFAULT_TIME;
                        List<Integer> needRemove = new ArrayList<>();
                        for (Integer key : ActivityManagerServiceSmtBase.this.mForceCpusetProcs.keySet()) {
                            ForceCpusetProc proc = (ForceCpusetProc) ActivityManagerServiceSmtBase.this.mForceCpusetProcs.get(key);
                            long current = SystemClock.uptimeMillis();
                            if (proc.resetForceCpusetProcIfTimeOut(current)) {
                                needRemove.add(key);
                            } else if (proc.timeOut > 0 && proc.timeOut < messageDelay) {
                                messageDelay = proc.timeOut;
                            }
                        }
                        for (Integer pid : needRemove) {
                            ActivityManagerServiceSmtBase.this.mForceCpusetProcs.remove(pid);
                            FeatLog.i("SmtResourceControl", "FEAT_PERF_RES_CONTROL", 40, "remove force cpuset proc :" + pid + " for timeout");
                        }
                        if (ActivityManagerServiceSmtBase.this.mForceCpusetProcs.size() > 0) {
                            ActivityManagerServiceSmtBase.this.sendCheckForceCpusetProcTask(messageDelay);
                            FeatLog.i("SmtResourceControl", "FEAT_PERF_RES_CONTROL", 50, "send check task for has more force cpuset process.");
                        }
                    }
                }
            }
        };
        Objects.requireNonNull(this);
        this.mIActivityManagerSmtEx = new IActivityManagerSmtExBase();
        this.mPrefetchApps = new HashMap<>();
        this.mActivityManagerService = ams;
        ActivityManagerService activityManagerService = this.mActivityManagerService;
        Objects.requireNonNull(activityManagerService);
        this.mPrefetchPidsSelf = activityManagerService.new PidMap();
        File systemDir = SystemServiceManager.ensureSystemDir();
        this.mChainBootBlackListFile = new AtomicFile(new File(systemDir, "chainboot.xml"));
        this.mProcessIntercept = SysOptBridge.getFactory().getProcessIntercept();
        mSmtOptEx = SysOptBridge.getFactory().getActivityManager(ams);
        this.mMemProcessController = SysOptBridge.getFactory().getMemoryProcessController();
        this.mTransferService = SysMonitorSvcBridge.getFactory().getTransferController();
        this.mBinderStat = SysOptBridge.getFactory().getBinderStat();
        this.mStrictModeFlags = SystemProperties.getInt("persist.sys.strictmode.flags", 0);
        enableAtrace();
        readCustomFileConfig();
    }

    protected ActivityManagerServiceSmtBase(ActivityManagerService ams, ActivityManagerService.Injector injector, ServiceThread handlerThread) {
        this.mCachedPss = 0L;
        this.mTotalPss = 0L;
        this.mTotalEGL = 0L;
        this.mProcessMems = new HashMap();
        this.mSetupWizardState = -1;
        this.mStrictModeFlags = 0;
        this.mBinderStat = null;
        this.NOTIFY_INVALID = -1;
        this.NOTIFY_STAT_BUFF = 1;
        this.NOTIFY_PARCEL_DATA = 2;
        this.NOTIFY_DONE = 3;
        this.mLaunchingProcessName = null;
        this.killStats = SysOptBridge.getFactory().getKillingStats();
        this.mMapAppDiedStatData = new ArrayMap<>();
        this.mInitMonitorCriticalAppData = false;
        this.mNextUpdateOomTime = JobStatus.NO_LATEST_RUNTIME;
        this.mHardClean = false;
        this.mPendingFreezePids = new ArrayList();
        this.mPrefetchCpuObServer = new ActivityManagerServiceSysMoEx.CpuStateObserver() {
            ArrayList<String> apps_l = new ArrayList<>();

            @Override
            public void onCpuState(ActivityManagerServiceSysMoEx.CpuStateObserver.CPU_USAGE_STATE state, long timestamp) {
                if (state == ActivityManagerServiceSysMoEx.CpuStateObserver.CPU_USAGE_STATE.CPU_NORMAL) {
                    SysOptBridge.getFactory().getSysPrefetchService().startPrefetchApp();
                }
            }

            @Override
            public ActivityManagerServiceSysMoEx.CpuStateObserver.NOTIFY_FREQUENCY getNotifyRequest() {
                return ActivityManagerServiceSysMoEx.CpuStateObserver.NOTIFY_FREQUENCY.EVERY_TIME;
            }
        };
        this.mForceCpusetProcs = new HashMap<>();
        this.mPostedCheckCpusetTask = false;
        this.mCheckForceCpusetProcTask = new Runnable() {
            @Override
            public void run() {
                synchronized (ActivityManagerServiceSmtBase.this.mForceCpusetProcs) {
                    if (ActivityManagerServiceSmtBase.this.mForceCpusetProcs.size() > 0) {
                        ActivityManagerServiceSmtBase.this.mPostedCheckCpusetTask = false;
                        long messageDelay = ActivityManagerServiceSmtBase.CHECK_TASK_DEFAULT_TIME;
                        List<Integer> needRemove = new ArrayList<>();
                        for (Integer key : ActivityManagerServiceSmtBase.this.mForceCpusetProcs.keySet()) {
                            ForceCpusetProc proc = (ForceCpusetProc) ActivityManagerServiceSmtBase.this.mForceCpusetProcs.get(key);
                            long current = SystemClock.uptimeMillis();
                            if (proc.resetForceCpusetProcIfTimeOut(current)) {
                                needRemove.add(key);
                            } else if (proc.timeOut > 0 && proc.timeOut < messageDelay) {
                                messageDelay = proc.timeOut;
                            }
                        }
                        for (Integer pid : needRemove) {
                            ActivityManagerServiceSmtBase.this.mForceCpusetProcs.remove(pid);
                            FeatLog.i("SmtResourceControl", "FEAT_PERF_RES_CONTROL", 40, "remove force cpuset proc :" + pid + " for timeout");
                        }
                        if (ActivityManagerServiceSmtBase.this.mForceCpusetProcs.size() > 0) {
                            ActivityManagerServiceSmtBase.this.sendCheckForceCpusetProcTask(messageDelay);
                            FeatLog.i("SmtResourceControl", "FEAT_PERF_RES_CONTROL", 50, "send check task for has more force cpuset process.");
                        }
                    }
                }
            }
        };
        Objects.requireNonNull(this);
        this.mIActivityManagerSmtEx = new IActivityManagerSmtExBase();
        this.mPrefetchApps = new HashMap<>();
        this.mActivityManagerService = ams;
        this.mChainBootBlackListFile = null;
        this.mMemProcessController = SysOptBridge.getFactory().getMemoryProcessController();
        mSmtOptEx = SysOptBridge.getFactory().getActivityManager(ams);
        enableAtrace();
        readCustomFileConfig();
    }

    public boolean onTransactSmtEx(int i, Parcel parcel, Parcel parcel2, int i2) {
        ProcessRecord processRecordTraverse;
        ProcessRecord processRecord;
        Intent launchIntentForPackage;
        ActivityInfo activityInfoResolveActivity;
        if (i == 1021) {
            parcel.enforceInterface("android.app.IActivityManager");
            SysOptBridge.getFactory().getSmartService().deleteMonitorDailyFiles(parcel.readString(), true);
            return true;
        }
        if (i == 20000) {
            parcel.enforceInterface("android.app.IActivityManager");
            SysOptBridge.getFactory().getSmartService().monitorJniInfoSwitch(parcel.readInt(), parcel.readInt());
            return true;
        }
        if (i == 26000) {
            parcel.enforceInterface("android.app.IActivityManager");
            String string = parcel.readString();
            int i3 = parcel.readInt();
            int i4 = parcel.readInt();
            String string2 = parcel.readString();
            String string3 = parcel.readString();
            String string4 = parcel.readString();
            int value = reportEventCode.valueOf(string).getValue();
            try {
                if (!string.startsWith("TEARSCREEN")) {
                    Slog.i("ActivityManagerService", "errorPrefix=" + string);
                    SysMonitorSvcBridge.getFactory().getDumpUtils().reportEvent(value, 2, 2.0f, i3, string, 1, new long[]{0});
                }
                SysMonitorSvcBridge.getFactory().getDumpUtils().doDump(this.mActivityManagerService.mContext, string, i3, i4, string2, string3, string4);
            } catch (Exception e) {
                e.printStackTrace();
            }
            return true;
        }
        if (i == 1030) {
            parcel.enforceInterface("android.app.IActivityManager");
            final int i5 = parcel.readInt();
            boolean z = parcel.readInt() == 0;
            int i6 = parcel.readInt();
            synchronized (this.mActivityManagerService) {
                try {
                    ActivityManagerService.boostPriorityForLockedSection();
                    if (z) {
                        synchronized (this.mActivityManagerService.mPidsSelfLocked) {
                            processRecord = this.mActivityManagerService.mPidsSelfLocked.get(i5);
                        }
                        processRecordTraverse = processRecord;
                    } else {
                        processRecordTraverse = SysOptBridge.getFactory().getApplicationFreezer().traverse(new IApplicationFreezer.TraverseCallback() {
                            @Override
                            public final ProcessRecord onProcess(ProcessRecord processRecord2) {
                                return ActivityManagerServiceSmtBase.lambda$onTransactSmtEx$0(i5, processRecord2);
                            }
                        });
                    }
                    if (processRecordTraverse != null) {
                        if (ApplicationInfoSmt.isIMApp(processRecordTraverse.info)) {
                            processRecordTraverse.getSmtEx().killType = 3;
                        } else {
                            processRecordTraverse.getSmtEx().killType = 2;
                        }
                        Slog.d("ActivityManagerService", "freeze=" + z + ", app=" + processRecordTraverse + ", killType=" + processRecordTraverse.getSmtEx().killType);
                        long jClearCallingIdentity = Binder.clearCallingIdentity();
                        try {
                            if (z) {
                                if (i6 == IApplicationFreezer.Mode.LIGHTNING.ordinal()) {
                                    SysOptBridge.getFactory().getApplicationFreezer().freezeProcessLocked(processRecordTraverse, true, IApplicationFreezer.FreezeReason.NO_CONDITION, IApplicationFreezer.Mode.LIGHTNING);
                                } else if (i6 == IApplicationFreezer.Mode.LITE.ordinal()) {
                                    SysOptBridge.getFactory().getApplicationFreezer().freezeProcessLocked(processRecordTraverse, true, IApplicationFreezer.FreezeReason.NO_CONDITION, IApplicationFreezer.Mode.LITE);
                                } else {
                                    SysOptBridge.getFactory().getApplicationFreezer().freezeProcessLocked(processRecordTraverse, true, IApplicationFreezer.FreezeReason.NO_CONDITION);
                                }
                            } else {
                                SysOptBridge.getFactory().getApplicationFreezer().unfreezeProcessLocked(processRecordTraverse, true, IApplicationFreezer.UnfreezeReason.NEED_NO_CONDITION);
                            }
                            Binder.restoreCallingIdentity(jClearCallingIdentity);
                        } catch (Throwable th) {
                            Binder.restoreCallingIdentity(jClearCallingIdentity);
                            throw th;
                        }
                    }
                } catch (Throwable th2) {
                    ActivityManagerService.resetPriorityAfterLockedSection();
                    throw th2;
                }
            }
            ActivityManagerService.resetPriorityAfterLockedSection();
            return true;
        }
        if (i == 1031) {
            parcel.enforceInterface("android.app.IActivityManager");
            String string5 = parcel.readString();
            if (SysOptBridge.getFactory().getPrefetchManager().isAllowStartPretch(string5)) {
                SysOptBridge.getFactory().getPrefetchManager().startPrefetchApp(string5);
            }
            return true;
        }
        if (i == 7668) {
            parcel.enforceInterface("android.app.IActivityManager");
            int i7 = parcel.readInt();
            SystemProperties.set(ISysPerfMonitorService.PROPERTY_NAME, String.valueOf(i7));
            if (SysMonitorSvcBridge.getFactory().getSysPerfMonitorService().getMonitorControlOpt() != i7) {
                SysMonitorSvcBridge.getFactory().getSysPerfMonitorService().setMonitorControlOpt(i7);
                ((TransferInternal) LocalServices.getService(TransferInternal.class)).notifyPropChanged(i7, -1L);
                if ((SysMonitorSvcBridge.getFactory().getSysPerfMonitorService().getMonitorControlOpt() & 16) != 0) {
                    SysOptBridge.getFactory().getGameBalanceService().setGameBalanceOpen(true);
                } else {
                    SysOptBridge.getFactory().getGameBalanceService().setGameBalanceOpen(false);
                }
            }
            return true;
        }
        if (i == 7669) {
            parcel.enforceInterface("android.app.IActivityManager");
            int i8 = parcel.readInt();
            int i9 = parcel.readInt();
            SystemProperties.set("persist.sys.smartprefetch", String.valueOf(i8 != 0));
            SysOptBridge.getFactory().getSysPrefetchService().setDoPrefetch(i8 != 0);
            SysOptBridge.getFactory().getSysPrefetchService().setPrefetchProcessMaxSize(i9);
            return true;
        }
        if (i == 8010) {
            try {
                ParcelFileDescriptor parcelFileDescriptorOpen = ParcelFileDescriptor.open(new File(this.mBinderStat.binderTransactStat(parcel.readInt(), parcel.readInt(), parcel.readInt())), 805306368);
                parcel2.writeFileDescriptor(parcelFileDescriptorOpen.getFileDescriptor());
                parcelFileDescriptorOpen.close();
            } catch (Exception e2) {
                Slog.e("ActivityManagerService", "binder stat create FileDescriptor", e2);
            }
            return true;
        }
        if (i == 8011) {
            try {
                if (parcel.readInt() == 2) {
                    this.mBinderStat.binderTransactStat(parcel.readFileDescriptor());
                }
            } catch (Exception e3) {
                Slog.e("ActivityManagerService", "bindet stat save data", e3);
            }
            return true;
        }
        if (i == 25000) {
            parcel.enforceInterface("android.app.IActivityManager");
            setSmtExtraInfo(parcel.readInt(), parcel.readString());
            return true;
        }
        if (i == 25001) {
            parcel.enforceInterface("android.app.IActivityManager");
            ProcExtraInfoSmtBase.getInstance().setLastWord(parcel.readInt(), parcel.readString());
            return true;
        }
        if (i == 27000) {
            parcel.enforceInterface("android.app.IActivityManager");
            int i10 = parcel.readInt();
            Slog.i("ActivityManagerService", "dumping for PerfettoForce autoDumpTimeout=" + i10);
            try {
                SystemProperties.set("debug.sys.monitor.autodumptimeout", String.valueOf(i10));
                SysMonitorSvcBridge.getFactory().getTransferController().startPerfettoForce(1);
            } catch (Exception e4) {
                e4.printStackTrace();
            }
            return true;
        }
        if (i != 27001) {
            switch (i) {
                case NotificationShellCmd.NOTIFICATION_ID /* 2020 */:
                    this.mBDReceiverStatsEnabled = !this.mBDReceiverStatsEnabled;
                    Slog.d("Broadcast Receiver Statistics ", "mBDReceiverStatsEnabled = " + this.mBDReceiverStatsEnabled);
                    BroadcastQueueSmtBase.setBDReceiverStatsEnabled(this.mBDReceiverStatsEnabled);
                    if (!this.mBDReceiverStatsEnabled) {
                        for (BroadcastQueue broadcastQueue : this.mActivityManagerService.mBroadcastQueues) {
                            broadcastQueue.getSmtEx().outputBDReceiverStatistics();
                            broadcastQueue.getSmtEx().clearStats();
                        }
                    }
                    return true;
                case 2021:
                    for (BroadcastQueue broadcastQueue2 : this.mActivityManagerService.mBroadcastQueues) {
                        broadcastQueue2.getSmtEx().outputBDReceiverStatistics();
                    }
                    return true;
                case 2022:
                    boolean enable = this.mMemProcessController.getEnable();
                    this.mMemProcessController.setEnable(!enable);
                    StringBuilder sb = new StringBuilder();
                    sb.append("enabel = ");
                    sb.append(!enable);
                    Slog.d("memory process controller", sb.toString());
                    return true;
                default:
                    switch (i) {
                        case 3000:
                            SysOptBridge.getFactory().getProcessIntercept().switchInterceptLog();
                            return true;
                        case IProcessIntercept.SHOW_PROVIDER_INTERCEPT_LOG /* 3001 */:
                            SysOptBridge.getFactory().getProcessIntercept().switchProviderInterceptLog();
                            return true;
                        case IProcessIntercept.SHOW_SERVICES_INTERCEPT_LOG /* 3002 */:
                            SysOptBridge.getFactory().getProcessIntercept().switchServicesInterceptLog();
                            return true;
                        case IProcessIntercept.SHOW_BROADCAST_INTERCEPT_LOG /* 3003 */:
                            SysOptBridge.getFactory().getProcessIntercept().switchBroadcastInterceptLog();
                            return true;
                        default:
                            switch (i) {
                                case 7700:
                                    SysOptBridge.getFactory().getSmartService().preDex2oat();
                                    return true;
                                case 7701:
                                    parcel.enforceInterface("android.app.IActivityManager");
                                    int i11 = parcel.readInt();
                                    SystemProperties.set("persist.sys.gamebalance", String.valueOf(i11 != 0));
                                    SysOptBridge.getFactory().getGameBalanceService().setGameBalanceOpen(i11 != 0);
                                    SysOptBridge.getFactory().getGameBalanceService().setDebugGameBalance((i11 & 2) != 0);
                                    return true;
                                case 7702:
                                    parcel.enforceInterface("android.app.IActivityManager");
                                    SysOptBridge.getFactory().getSmartService().controlThermal(parcel.readInt());
                                    return true;
                                case 7703:
                                    parcel.enforceInterface("android.app.IActivityManager");
                                    String string6 = parcel.readString();
                                    Parcel parcelObtain = Parcel.obtain();
                                    parcelObtain.writeString(string6);
                                    Slog.w("ActivityManagerService", "request systrace of Jank path:" + string6);
                                    if (SysMonitorSvcBridge.getFactory().getSysPerfMonitorService().allowUserAtrace()) {
                                        SysMonitorSvcBridge.getFactory().getSysPerfMonitorService().transact(103, parcelObtain, null, 1);
                                    }
                                    return true;
                                case 7704:
                                    parcel.enforceInterface("android.app.IActivityManager");
                                    int i12 = parcel.readInt();
                                    SystemProperties.set(ISysPerfMonitorService.DEBUG_PROPERTY_NAME, String.valueOf(i12));
                                    if (SysMonitorSvcBridge.getFactory().getSysPerfMonitorService().getMonitorDebugOpt() != i12) {
                                        SysMonitorSvcBridge.getFactory().getSysPerfMonitorService().setMonitorControlOpt(i12);
                                    }
                                    return true;
                                case 7705:
                                    parcel.enforceInterface("android.app.IActivityManager");
                                    SmartisanAm.SmartisanAmUtils.getInstance().openAppMainThreadLooperTrace(parcel.readString());
                                    return true;
                                case 7706:
                                    parcel.enforceInterface("android.app.IActivityManager");
                                    int i13 = parcel.readInt();
                                    if (SysMonitorSvcBridge.getFactory().getSysPerfMonitorService().allowUserAtrace()) {
                                        if (i13 == 0) {
                                            SysMonitorSvcBridge.getFactory().getSysPerfMonitorService().transact(102, new int[0]);
                                        } else {
                                            SysMonitorSvcBridge.getFactory().getSysPerfMonitorService().transact(101, new int[0]);
                                        }
                                    }
                                    return true;
                                case 7707:
                                    parcel.enforceInterface("android.app.IActivityManager");
                                    String string7 = parcel.readString();
                                    int i14 = parcel.readInt();
                                    int i15 = parcel.readInt();
                                    if (i14 == 0) {
                                        SysMonitorSvcBridge.getFactory().getSysPerfMonitorService().setUserDumpAtraceType(0);
                                        SysMonitorSvcBridge.getFactory().getSysPerfMonitorService().setUserDumpAtraceWindow("");
                                    } else {
                                        SysMonitorSvcBridge.getFactory().getSysPerfMonitorService().setUserDumpAtraceWindow(string7);
                                    }
                                    if (string7 != null && (launchIntentForPackage = this.mActivityManagerService.mContext.getPackageManager().getLaunchIntentForPackage(string7)) != null && (activityInfoResolveActivity = this.mActivityManagerService.mStackSupervisor.resolveActivity(launchIntentForPackage, null, 0, null, 0, 0)) != null && activityInfoResolveActivity.applicationInfo != null && activityInfoResolveActivity.applicationInfo.getSmtEx().isGameAppSmt()) {
                                        i15 = 1;
                                    }
                                    if (i15 == -1) {
                                        SysMonitorSvcBridge.getFactory().getSysPerfMonitorService().setUserDumpAtraceType(4);
                                    } else if (i15 == 1) {
                                        SysMonitorSvcBridge.getFactory().getSysPerfMonitorService().setUserDumpAtraceType(8);
                                    } else if (i15 == 2) {
                                        SysMonitorSvcBridge.getFactory().getSysPerfMonitorService().setUserDumpAtraceType(16);
                                    } else if (i15 == 3) {
                                        SysMonitorSvcBridge.getFactory().getSysPerfMonitorService().setUserDumpAtraceType(34);
                                    } else if (i15 == 4) {
                                        SysMonitorSvcBridge.getFactory().getSysPerfMonitorService().setUserDumpAtraceType(2);
                                    }
                                    return true;
                                case 7708:
                                    parcel.enforceInterface("android.app.IActivityManager");
                                    String string8 = parcel.readString();
                                    int i16 = parcel.readInt();
                                    ArrayList arrayList = new ArrayList();
                                    arrayList.add(string8);
                                    SysOptBridge.getFactory().getSysPrefetchService().updatePrefetchApps(arrayList, i16);
                                    return true;
                                case 7709:
                                    parcel.enforceInterface("android.app.IActivityManager");
                                    int i17 = parcel.readInt();
                                    SysMonitorSvcBridge.getFactory().getSysPerfMonitorService().transact(105, new int[0]);
                                    SysMonitorSvcBridge.getFactory().getSysPerfMonitorService().transact(108, i17);
                                    return true;
                                case 7710:
                                    parcel.enforceInterface("android.app.IActivityManager");
                                    SysMonitorSvcBridge.getFactory().getSysPerfMonitorService().transact(106, new int[0]);
                                    return true;
                                case 7711:
                                    parcel.enforceInterface("android.app.IActivityManager");
                                    String string9 = parcel.readString();
                                    Parcel parcelObtain2 = Parcel.obtain();
                                    parcelObtain2.writeString(string9);
                                    Slog.w("ActivityManagerService", "request perfettp of path:" + string9);
                                    SysMonitorSvcBridge.getFactory().getSysPerfMonitorService().transact(107, parcelObtain2, null, 1);
                                    return true;
                                case 7712:
                                    parcel.enforceInterface("android.app.IActivityManager");
                                    FeatLog.config(parcel.readBoolean());
                                    return true;
                                case 7713:
                                    parcel.enforceInterface("android.app.IActivityManager");
                                    setProcessRunningCpuset(parcel.readInt(), parcel.readInt(), parcel.readLong(), parcel.readInt() != 0);
                                    return true;
                                case 7714:
                                    parcel.enforceInterface("android.app.IActivityManager");
                                    SysMonitorSvcBridge.getFactory().getSysPerfMonitorService().setSysEventScenesStatus(parcel.readInt(), parcel.readInt());
                                    return true;
                                case 7715:
                                    parcel.enforceInterface("android.app.IActivityManager");
                                    SysMonitorSvcBridge.getFactory().getSysPerfMonitorService().setWriteMonitorFileInterval(parcel.readInt());
                                    return true;
                                case 7716:
                                    parcel.enforceInterface("android.app.IActivityManager");
                                    SysMonitorSvcBridge.getFactory().getSysPerfMonitorService().uploadSystemMonitorData();
                                    SysMonitorSvcBridge.getFactory().getProcessStatsServiceOptEx().saveDataDaily();
                                    return true;
                                case 7717:
                                    parcel.enforceInterface("android.app.IActivityManager");
                                    int i18 = parcel.readInt();
                                    boolean z2 = (i18 & 1) != 0;
                                    boolean z3 = (i18 & 2) != 0;
                                    SysMonitorSvcBridge.getFactory().getTransferController().setDoJankLog(z2);
                                    ProcessRecordSmtBase.mResControlLog = z3;
                                    return true;
                                case 7718:
                                    parcel.enforceInterface("android.app.IActivityManager");
                                    this.mActivityManagerService.mActivityTaskManager.getSmtEx().uiFirstSwitch = parcel.readBoolean();
                                    return true;
                                case 7719:
                                    parcel.enforceInterface("android.app.IActivityManager");
                                    SysMonitorSvcBridge.getFactory().getHandleMemoryLeak().killMemoryLeakProcess(parcel.readString(), parcel.readInt());
                                    return true;
                                case 7720:
                                    parcel.enforceInterface("android.app.IActivityManager");
                                    int i19 = parcel.readInt();
                                    int i20 = parcel.readInt();
                                    SystemProperties.set("persist.pxr.frame_track.enable", String.valueOf(i19));
                                    SystemProperties.set("persist.sys.monitor.mtp", String.valueOf(i20));
                                    return true;
                                default:
                                    return mSmtOptEx.onTransact(i, parcel, parcel2, i2);
                            }
                    }
            }
        }
        parcel.enforceInterface("android.app.IActivityManager");
        Slog.i("ActivityManagerService", "send fakeIntent");
        Slog.i("ActivityManagerService", "send fakeIntent flag = " + parcel.readInt());
        try {
            Intent intent = new Intent();
            intent.setAction("com.android.providers.downloads.ACTION_APPINFO_WHITE_LIST_UPDATE");
            intent.putExtra("is_enable", -1);
            intent.putExtra("path", "/data/syslog/slardar/OptAppInfoWhiteList.xml");
            intent.putExtra("feature", "update_appinfo");
            Slog.i("ActivityManagerService", "fakeIntent");
            this.mActivityManagerService.mContext.sendBroadcastAsUser(intent, UserHandle.ALL);
        } catch (Exception e5) {
            e5.printStackTrace();
        }
        return true;
    }

    static ProcessRecord lambda$onTransactSmtEx$0(int pid, ProcessRecord fapp) {
        if (fapp.getSmtEx().pid == pid) {
            return fapp;
        }
        return null;
    }

    protected ProcessRecord getProcessRecordLock(int pid) {
        for (ProcessRecord r : this.mActivityManagerService.mProcessList.mLruProcesses) {
            if (pid == r.pid || pid == r.getSmtEx().pid) {
                return r;
            }
        }
        return null;
    }

    public void trimSystemMemoryIfNeeded(String processName, ApplicationInfo info) {
        if (processName == mLaunchingProcessName) {
            return;
        }
        mLaunchingProcessName = processName;
        if (info == null || info.getSmtEx() == null) {
            return;
        }
        Map smtProcessMemInfo = info.getSmtEx().smtProcessMemInfo;
        if (smtProcessMemInfo.size() <= 0) {
            return;
        }
        int index = processName.indexOf(':');
        String suffix = index != -1 ? processName.substring(index + 1) : "main";
        Object obj = smtProcessMemInfo.get(suffix);
        int memSize;
        if (obj == null || (memSize = ((Integer) obj).intValue()) == 0) {
            return;
        }
        MemInfoReader memInfo = new MemInfoReader();
        memInfo.getSmtEx().readMemInfoFast();
        int freeMemSize = (int) memInfo.getSmtEx().getFreeSizeFastKb();
        int cachedMemSize = (int) memInfo.getSmtEx().getCachedSizeFastKb();
        if (freeMemSize >= memSize) {
            return;
        }
        int memNeeded = memSize - freeMemSize;
        int minAdj = memNeeded <= cachedMemSize ? 900 : 800;
        int freedMem = 0;
        synchronized (mActivityManagerService.mPidsSelfLocked) {
            int size = mActivityManagerService.mPidsSelfLocked.size();
            ArrayList<ProcessRecord> records = new ArrayList<>();
            for (int i = 0; i < size; i++) {
                mActivityManagerService.mPidsSelfLocked.keyAt(i);
                ProcessRecord proc = mActivityManagerService.mPidsSelfLocked.valueAt(i);
                if (proc != null) {
                    int adj = proc.setAdj;
                    if (adj >= minAdj && !proc.killedByAm) {
                        records.add(proc);
                    }
                }
            }
            ArrayList<ProcessRecord> list = SysOptBridge.getFactory()
                    .getActivityManager(mActivityManagerService).getmPackStats()
                    .sortByAdjAndPackageUsage(records);
            int killCount = 0;
            Iterator<ProcessRecord> it = list.iterator();
            while (it.hasNext()) {
                ProcessRecord pr = it.next();
                if (memNeeded <= 0) {
                    break;
                }
                memNeeded = (int) (memNeeded - pr.lastPss);
                freedMem = (int) (freedMem + pr.lastPss);
                pr.kill("start up", true);
                killCount++;
            }
            SysOptBridge.getFactory().getSmartService().addTrimMemForStart(info.getSmtUid(),
                    minAdj, killCount, freedMem, memNeeded);
        }
    }

    protected boolean isPcModeDisplayId(ProcessRecord r) {
        return false;
    }

    public void addPendingLaunchRecord(ActivityRecord r) {
        synchronized (this.mActivityManagerService.getMonitorEx().mPendingLaunchRecords) {
            if (this.mActivityManagerService.getMonitorEx().mPendingLaunchRecords.size() > 10) {
                Slog.i("ActivityManagerService", "clear all has not complete start activity when pending size > 10");
                this.mActivityManagerService.getMonitorEx().mPendingLaunchRecords.clear();
            }
            this.mActivityManagerService.getMonitorEx().mPendingLaunchRecords.add(r);
        }
    }

    public void systemReady(Context context, Handler handler) {
        synchronized (this.mActivityManagerService) {
            try {
                ActivityManagerService.boostPriorityForLockedSection();
                readChainBootBlackList();
            } catch (Throwable th) {
                ActivityManagerService.resetPriorityAfterLockedSection();
                throw th;
            }
        }
        ActivityManagerService.resetPriorityAfterLockedSection();
        BinderCallCacheAgent.isCalledFromSystemServer = true;
    }

    public void amsSystemReadyEarlyPhase() {
        SysOptBridge.getFactory().getSingle3DApp().init(this.mActivityManagerService);
        SysOptBridge.getFactory().getAddVrPrevious().init(this.mActivityManagerService);
        SysOptBridge.getFactory().getAppStartStatistics().init(this.mActivityManagerService);
    }

    protected void start() {
        Objects.requireNonNull(this);
        LocalServices.addService(ActivityManagerInternalSmtBase.class, new LocalServiceSmtExBase());
    }

    public void reportKillingEvent(String killEvent) {
        this.killStats.reportKillingEvent(killEvent);
    }

    private void valueToLittleEndian(int value, byte[] data, int index) {
        data[index] = (byte) (value & 255);
        data[index + 1] = (byte) ((value >> 8) & 255);
        data[index + 2] = (byte) ((value >> 16) & 255);
        data[index + 3] = (byte) ((value >> 24) & 255);
    }

    private void notifyStabdBlockLogoStatus(int blockStage, int timeOut) {
        FileOutputStream fileOutputStream = null;
        File file = new File("/dev/stabd");
        byte[] data = new byte[8];
        valueToLittleEndian(blockStage, data, 0);
        valueToLittleEndian(timeOut, data, 4);
        try {
            if (file.exists()) {
                try {
                    fileOutputStream = new FileOutputStream(file);
                    fileOutputStream.write(data);
                    fileOutputStream.close();
                } catch (Exception e) {
                    if (fileOutputStream != null) {
                        fileOutputStream.close();
                    }
                } catch (Throwable th) {
                    if (fileOutputStream != null) {
                        try {
                            fileOutputStream.close();
                        } catch (IOException e2) {
                        }
                    }
                    throw th;
                }
            }
        } catch (IOException e3) {
        }
    }

    public void monitorAppDiedLocked(ProcessRecord app) {
        Intent homeIntent = new Intent("android.intent.action.MAIN").addCategory("android.intent.category.HOME");
        ResolveInfo homeInfo = this.mActivityManagerService.mContext.getPackageManager().resolveActivity(homeIntent, 0);
        if (app.processName != null && homeInfo != null && homeInfo.activityInfo != null && app.processName.equals(homeInfo.activityInfo.packageName)) {
            if (this.mHomeAppCrashedTime == 0) {
                this.mHomeAppCrashCount = 1;
                this.mHomeAppCrashedTime = SystemClock.uptimeMillis();
            } else {
                this.mHomeAppCrashCount++;
                if (this.mHomeAppCrashCount >= 6) {
                    this.mHomeAppCrashCount = 0;
                    if (SystemClock.uptimeMillis() - this.mHomeAppCrashedTime <= 60000) {
                        Slog.i("ActivityManagerService", app.processName + " has crashed too many times, notify stabd!!!");
                        notifyStabdBlockLogoStatus(8, 0);
                    }
                    this.mHomeAppCrashedTime = 0L;
                }
            }
        }
        handleCriticalAppDataLocked(app);
    }

    private final class AppClearAndStatData {
        private static final int CLEAR_LEVEL_APP_ALL_DIR = 3;
        private static final int CLEAR_LEVEL_FIRST_DIR = 1;
        private static final int CLEAR_LEVEL_REBOOT_SYSTEM = 4;
        private static final int CLEAR_LEVEL_SECOND_DIR = 2;
        private static final int CLEAR_LEVEL_UNINSTALL_APP = 5;
        boolean clearAppAllData;
        ArrayList<String> firstClearDirList;
        boolean hasReboot;
        String pkgName;
        String propName;
        ArrayList<String> secondClearDirList;
        boolean uninstallApp;
        int crashCount = 0;
        long lastCrashTime = 0;
        int clearLevel = 1;

        public AppClearAndStatData(ArrayList<String> firstClearDir, ArrayList<String> secondClearDir, boolean clearAppAllData, String pkgName, boolean uninstallApp, String propName) {
            this.firstClearDirList = firstClearDir;
            this.secondClearDirList = secondClearDir;
            this.clearAppAllData = clearAppAllData;
            this.pkgName = pkgName;
            this.uninstallApp = uninstallApp;
            this.propName = propName;
            this.hasReboot = SystemProperties.getBoolean(propName, false);
        }

                public void rebootSystem() {
            Slog.i("ActivityManagerService", "reboot system from clear critical app data");
            SystemProperties.set(this.propName, "true");
            long token = Binder.clearCallingIdentity();
            PowerManager pm = (PowerManager) ActivityManagerServiceSmtBase.this.mActivityManagerService.mContext.getSystemService("power");
            pm.reboot("userrequested");
            Binder.restoreCallingIdentity(token);
        }

        private void deleteDirectory(File directory) {
            for (File file : (File[]) Objects.requireNonNull(directory.listFiles())) {
                if (file.isDirectory()) {
                    deleteDirectory(file);
                    File[] subFiles = file.listFiles();
                    if (subFiles != null && subFiles.length == 0) {
                        file.delete();
                    }
                } else {
                    file.delete();
                }
            }
        }

        private void clearAppDirData(ArrayList<String> list) {
            for (int i = 0; i < list.size(); i++) {
                String path = list.get(i);
                File file = new File(path);
                if ((!path.contains("/sdcard/") && !path.contains("/storage/emulated/0/")) || Environment.getExternalStorageState().equals("mounted")) {
                    if (file.isDirectory()) {
                        deleteDirectory(file);
                        file.delete();
                    } else {
                        file.delete();
                    }
                }
            }
        }

        class ClearDataObserver extends IPackageDataObserver.Stub {
            ClearDataObserver() {
            }

            public void onRemoveCompleted(String packageName, boolean succeeded) throws RemoteException {
                Slog.i("ActivityManagerService", "onRemoveCompleted:" + packageName + " succeeded=" + succeeded);
            }
        }

        public void clearAppData(ProcessRecord app) {
            if (this.hasReboot) {
                if (app.info != null && app.info.getCodePath() != null && app.info.getCodePath().startsWith("/data/app/") && this.uninstallApp) {
                    Message uninstallMsg = Message.obtain(ActivityManagerServiceSmtBase.this.mMonitorAppRestoreHandler, 8, this.pkgName);
                    ActivityManagerServiceSmtBase.this.mMonitorAppRestoreHandler.sendMessageDelayed(uninstallMsg, 0L);
                    Message msg = Message.obtain(ActivityManagerServiceSmtBase.this.mMonitorAppRestoreHandler, 10, 5, app.uid, app.processName);
                    ActivityManagerServiceSmtBase.this.mMonitorAppRestoreHandler.removeMessages(10);
                    ActivityManagerServiceSmtBase.this.mMonitorAppRestoreHandler.sendMessageDelayed(msg, 60000L);
                }
                SystemProperties.set(this.propName, "false");
                this.hasReboot = false;
                return;
            }
            if (this.clearLevel == 1) {
                this.clearLevel = 2;
                ArrayList<String> arrayList = this.firstClearDirList;
                if (arrayList != null && arrayList.size() > 0) {
                    clearAppDirData(this.firstClearDirList);
                    Message msg2 = Message.obtain(ActivityManagerServiceSmtBase.this.mMonitorAppRestoreHandler, 10, this.clearLevel - 1, app.uid, app.processName);
                    ActivityManagerServiceSmtBase.this.mMonitorAppRestoreHandler.removeMessages(10);
                    ActivityManagerServiceSmtBase.this.mMonitorAppRestoreHandler.sendMessageDelayed(msg2, 60000L);
                    return;
                }
            }
            if (this.clearLevel == 2) {
                this.clearLevel = 3;
                ArrayList<String> arrayList2 = this.secondClearDirList;
                if (arrayList2 != null && arrayList2.size() > 0) {
                    clearAppDirData(this.secondClearDirList);
                    Message msg3 = Message.obtain(ActivityManagerServiceSmtBase.this.mMonitorAppRestoreHandler, 10, this.clearLevel - 1, app.uid, app.processName);
                    ActivityManagerServiceSmtBase.this.mMonitorAppRestoreHandler.removeMessages(10);
                    ActivityManagerServiceSmtBase.this.mMonitorAppRestoreHandler.sendMessageDelayed(msg3, 60000L);
                    return;
                }
            }
            if (this.clearLevel == 3) {
                this.clearLevel = 4;
                if (this.clearAppAllData) {
                    Message clearMsg = Message.obtain(ActivityManagerServiceSmtBase.this.mMonitorAppRestoreHandler, 9, this.pkgName);
                    ActivityManagerServiceSmtBase.this.mMonitorAppRestoreHandler.sendMessageDelayed(clearMsg, 0L);
                    Message msg4 = Message.obtain(ActivityManagerServiceSmtBase.this.mMonitorAppRestoreHandler, 10, this.clearLevel - 1, app.uid, app.processName);
                    ActivityManagerServiceSmtBase.this.mMonitorAppRestoreHandler.removeMessages(10);
                    ActivityManagerServiceSmtBase.this.mMonitorAppRestoreHandler.sendMessageDelayed(msg4, 60000L);
                    return;
                }
            }
            if (this.clearLevel == 4) {
                BackgroundThread.getHandler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        AppClearAndStatData.this.rebootSystem();
                    }
                }, 0L);
                this.clearLevel = 1;
            }
        }

        public void resetClearLevel() {
            this.clearLevel = 1;
        }

        public String toString() {
            StringBuffer buffer = new StringBuffer();
            buffer.append("crashCount=" + this.crashCount);
            buffer.append(",lastCrashTime=" + this.lastCrashTime);
            buffer.append(",clearLevel=" + this.clearLevel);
            buffer.append(",pkgName=" + this.pkgName);
            buffer.append(",hasReboot=" + this.hasReboot);
            return buffer.toString();
        }
    }

    private class MonitorHandler extends Handler {
        public MonitorHandler(Looper loop) {
            super(loop);
        }

        class ClearDataObserver extends IPackageDataObserver.Stub {
            ClearDataObserver() {
            }

            public void onRemoveCompleted(String packageName, boolean succeeded) throws RemoteException {
                Slog.i("ActivityManagerService", "onRemoveCompleted2:" + packageName + " succeeded=" + succeeded);
            }
        }

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 8:
                    String pkgName = (String) msg.obj;
                    Slog.i("ActivityManagerService", "uninstallApp for " + pkgName);
                    Intent broadcastIntent = new Intent(ActivityManagerServiceSmtBase.this.mActivityManagerService.mContext, getClass());
                    PendingIntent pendingIntent = PendingIntent.getBroadcast(ActivityManagerServiceSmtBase.this.mActivityManagerService.mContext, 0, broadcastIntent, 134217728);
                    PackageInstaller packageInstaller = ActivityManagerServiceSmtBase.this.mActivityManagerService.mContext.getPackageManager().getPackageInstaller();
                    packageInstaller.uninstall(pkgName, pendingIntent.getIntentSender());
                    return;
                case 9:
                    IPackageDataObserver clearDataObserver = new ClearDataObserver();
                    String pkgName2 = (String) msg.obj;
                    Slog.i("ActivityManagerService", "clear app data for " + pkgName2);
                    ActivityManagerServiceSmtBase.this.mActivityManagerService.clearApplicationUserData(pkgName2, false, clearDataObserver, 0);
                    return;
                case 10:
                    synchronized (ActivityManagerServiceSmtBase.this.mActivityManagerService) {
                        try {
                            ActivityManagerService.boostPriorityForLockedSection();
                            ProcessRecord curProc = ActivityManagerServiceSmtBase.this.mActivityManagerService.getProcessRecordLocked((String) msg.obj, msg.arg2, true);
                            if (curProc != null && curProc.processName != null && curProc.processName.equals((String) msg.obj)) {
                                long startTime = curProc.startTime;
                                long nowTime = SystemClock.elapsedRealtime();
                                Slog.i("ActivityManagerService", "MSG_NOTIFY_STABD_CRITICAL_APP_RESTORE:startTime=" + startTime + ",nowTime=" + nowTime);
                                if (nowTime - startTime > ActivityManagerServiceSmtBase.MSG_CHECK_STABD_APP_START_MIN_TIME_MS) {
                                    AppClearAndStatData appData = ActivityManagerServiceSmtBase.this.mMapAppDiedStatData.get(curProc.processName);
                                    if (appData != null) {
                                        appData.resetClearLevel();
                                    }
                                } else {
                                    ActivityManagerService.resetPriorityAfterLockedSection();
                                    return;
                                }
                            }
                            ActivityManagerService.resetPriorityAfterLockedSection();
                            ActivityManagerServiceSmtBase.this.notifyStabdAppRestoreStat((String) msg.obj, msg.arg1);
                            return;
                        } catch (Throwable th) {
                            ActivityManagerService.resetPriorityAfterLockedSection();
                            throw th;
                        }
                    }
                default:
                    return;
            }
        }
    }

        public void notifyStabdAppRestoreStat(String pkgName, int clearLevel) {
        try {
            Slog.i("ActivityManagerService", "notifyStabdAppRestoreStat " + pkgName + " has restored, clearLevel=" + clearLevel);
            IBinder stabProxy = ServiceManager.getService("stabservice");
            if (stabProxy != null) {
                Parcel data = Parcel.obtain();
                data.writeInterfaceToken("android.stab.IBStabService");
                data.writeLong(System.currentTimeMillis() - 60000);
                data.writeLong(SystemClock.uptimeMillis() - 60000);
                data.writeString(pkgName);
                data.writeInt(clearLevel);
                stabProxy.transact(12, data, null, 1);
                data.recycle();
            }
        } catch (RemoteException e) {
        }
    }

    private void handleCriticalAppDataLocked(ProcessRecord app) {
        if (!this.mInitMonitorCriticalAppData) {
            this.mInitMonitorCriticalAppData = true;
            this.mMapAppDiedStatData.clear();
            this.mMonitorAppRestoreThread = new HandlerThread("monitor_sys_app_crash");
            this.mMonitorAppRestoreThread.start();
            this.mMonitorAppRestoreHandler = new MonitorHandler(this.mMonitorAppRestoreThread.getLooper());
            ArrayList<String> vrshellList = new ArrayList<>();
            vrshellList.add("/sdcard/Android/data/com.pvr.vrshell/files/il2cpp/");
            AppClearAndStatData data = new AppClearAndStatData(vrshellList, null, true, "com.pvr.vrshell", true, "persist.sys.vrshell.reboot");
            this.mMapAppDiedStatData.put("com.pvr.vrshell", data);
            ArrayList<String> setList = new ArrayList<>();
            setList.add("/sdcard/Android/data/com.pvr.seethrough.setting/files/il2cpp/");
            AppClearAndStatData setData = new AppClearAndStatData(setList, null, true, "com.pvr.seethrough.setting", true, "persist.sys.seethrough.reboot");
            this.mMapAppDiedStatData.put("com.pvr.seethrough.setting", setData);
        }
        AppClearAndStatData appData = this.mMapAppDiedStatData.get(app.processName);
        if (appData != null) {
            if (appData.lastCrashTime == 0) {
                appData.crashCount = 1;
                appData.lastCrashTime = SystemClock.uptimeMillis();
                return;
            }
            appData.crashCount++;
            if (appData.crashCount >= CRITICAL_APP_CRASH_COUNT) {
                appData.crashCount = 0;
                if (SystemClock.uptimeMillis() - appData.lastCrashTime <= CRITICAL_APP_CRASH_TIME_MS) {
                    Slog.i("ActivityManagerService", app.processName + " has crashed too many times, it will try to restore it by clearing some app data !!!");
                    appData.clearAppData(app);
                }
                appData.lastCrashTime = 0L;
            }
        }
    }

    public static void setCpuStateProvider(ActivityManagerServiceSysMoEx.CpuStateProvider p) {
        sCpuStateProvider = p;
    }

    public static void registerCpuStateObserver(ActivityManagerServiceSysMoEx.CpuStateObserver observer) {
        ActivityManagerServiceSysMoEx.CpuStateProvider cpuStateProvider = sCpuStateProvider;
        if (cpuStateProvider != null) {
            cpuStateProvider.registerCpuStateObserver(observer);
        }
    }

    public static void unregisterCpuStateObserver(ActivityManagerServiceSysMoEx.CpuStateObserver observer) {
        ActivityManagerServiceSysMoEx.CpuStateProvider cpuStateProvider = sCpuStateProvider;
        if (cpuStateProvider != null) {
            cpuStateProvider.unregisterCpuStateObserver(observer);
        }
    }

    public static class UidCpuInfo implements Comparable {
        public static final int KILL_TYPE_FREEZE = 2;
        public static final int KILL_TYPE_FREEZE_WINDOW = 3;
        public static final int KILL_TYPE_KILL = 1;
        public final long beginTime;
        public final int cpuRatio;
        public boolean killingVisible;
        public final int uid;
        public final int uidRecentOrder;
        public final int unifiedRatio;
        public int killType = 1;
        public int possibleKillCount = 0;

        UidCpuInfo(int uid, int uR, int cR, int uO, long b) {
            this.uid = uid;
            this.unifiedRatio = uR;
            this.cpuRatio = cR;
            this.uidRecentOrder = uO;
            this.beginTime = b;
        }

        @Override
        public int compareTo(Object o) {
            return ((UidCpuInfo) o).cpuRatio - this.cpuRatio;
        }

        public static UidCpuInfo getInfoFromList(int uid, ArrayList<UidCpuInfo> list) {
            int N = list.size();
            for (int i = 0; i < N; i++) {
                UidCpuInfo info = list.get(i);
                if (info.uid == uid) {
                    return info;
                }
            }
            return null;
        }

        public String toString() {
            return "UidCpuInfo{ uid=" + this.uid + ", unifiedRatio=" + this.unifiedRatio + ", cpuRatio=" + this.cpuRatio + ", uidRecentOrder=" + this.uidRecentOrder + " beginTime=" + this.beginTime;
        }
    }

    public interface UidCpuUsageProvider {
        default void registerUidCpuUsageObserver(UidCpuUsageObserver observer) {
        }

        default void unregisterUidCpuUsageObserver(UidCpuUsageObserver observer) {
        }
    }

    public static void setUidCpuUsageProvider(UidCpuUsageProvider p) {
        sUidCpuUsageProvider = p;
    }

    public static void registerUidCpuInfoObserver(UidCpuUsageObserver observer) {
        UidCpuUsageProvider uidCpuUsageProvider = sUidCpuUsageProvider;
        if (uidCpuUsageProvider != null) {
            uidCpuUsageProvider.registerUidCpuUsageObserver(observer);
        }
    }

    public static void unregisterUidCpuInfoObserver(UidCpuUsageObserver observer) {
        UidCpuUsageProvider uidCpuUsageProvider = sUidCpuUsageProvider;
        if (uidCpuUsageProvider != null) {
            uidCpuUsageProvider.unregisterUidCpuUsageObserver(observer);
        }
    }

    public static ArrayList<ProcessRecord> getLruProcesses(ActivityManagerService ams) {
        return ams.mProcessList.mLruProcesses;
    }

    public static ProcessMap<ProcessRecord> getProcessNames(ActivityManagerService ams) {
        return ams.mProcessList.mProcessNames;
    }

    public static UidRecord activeUidsGetLocked(ActivityManagerService ams, int uid) {
        int smtUid = -1;
        if (uid < 0) {
            smtUid = uid;
            uid = 1000;
        }
        UidRecord uidRecord = ams.mProcessList.mActiveUids.get(uid);
        if (smtUid != -1) {
            return uidRecord.getSmtEx().getSystemSmtUidRecord(smtUid);
        }
        return uidRecord;
    }

    public static Set<ProcessRecord> getProcsByUidLocked(ActivityManagerService ams, int uid) {
        UidRecord uidRecord;
        if (uid != 1000 && (uidRecord = activeUidsGetLocked(ams, uid)) != null && !uidRecord.getSmtEx().procRecords.isEmpty()) {
            return uidRecord.getSmtEx().procRecords;
        }
        Set<ProcessRecord> procs = new ArraySet<>();
        for (ProcessRecord proc : getLruProcesses(ams)) {
            if (proc.info.uid == uid || proc.info.getSmtUid() == uid) {
                procs.add(proc);
            }
        }
        return procs;
    }

    public static void pidsSelfPutLocked(ActivityManagerService ams, ProcessRecord app) {
        ams.mPidsSelfLocked.put(app, true);
    }

    public static void pidsSelfRemoveLocked(ActivityManagerService ams, ProcessRecord app) {
    }

    public static ProcessRecord pidsSelfGet(ActivityManagerService ams, int pid) {
        ProcessRecord processRecord;
        synchronized (ams.mPidsSelfLocked) {
            processRecord = ams.mPidsSelfLocked.get(pid);
        }
        return processRecord;
    }

    static void handleMessageSmt(Message msg, ActivityManagerService ams) {
        ams.getSmtEx().handleMessageSmt(msg);
        mSmtOptEx.handleMessageOpt(msg);
    }

    protected void handleMessageSmt(Message msg) {
    }

    public boolean isTaskPersist(String packageName, int userId) {
        IPackageManager pm = AppGlobals.getPackageManager();
        try {
            boolean persist = pm.getISmtEx().isTaskPersist(packageName, userId);
            return persist;
        } catch (Exception e) {
            return false;
        }
    }

    public class LocalServiceSmtExBase extends ActivityManagerInternalSmtBase {
        public LocalServiceSmtExBase() {
        }

        public IBatteryStats getBatteryStatsService() {
            return ActivityManagerServiceSmtBase.this.mActivityManagerService.mBatteryStatsService;
        }
    }

    final void scheduleUpdateOomAdj(boolean immediately) {
        scheduleUpdateOomAdj(immediately, 0L);
    }

    final void scheduleUpdateOomAdj(boolean immediately, long delay) {
        if (immediately) {
            synchronized (this.mActivityManagerService) {
                try {
                    ActivityManagerService.boostPriorityForLockedSection();
                    this.mActivityManagerService.mHandler.removeMessages(UPDATE_OOM_MSG);
                    this.mActivityManagerService.updateOomAdjLocked("updateOomAdj_meh");
                    this.mNextUpdateOomTime = JobStatus.NO_LATEST_RUNTIME;
                } finally {
                    ActivityManagerService.resetPriorityAfterLockedSection();
                }
            }
            return;
        }
        long now = SystemClock.uptimeMillis();
        long min_delay = Math.max(delay, 50L);
        long requestTime = now + min_delay;
        long j = this.mNextUpdateOomTime;
        if (requestTime > j) {
            return;
        }
        if (j < JobStatus.NO_LATEST_RUNTIME) {
            this.mActivityManagerService.mHandler.removeMessages(UPDATE_OOM_MSG);
        }
        this.mNextUpdateOomTime = requestTime;
        this.mActivityManagerService.mHandler.sendMessageDelayed(this.mActivityManagerService.mHandler.obtainMessage(UPDATE_OOM_MSG), min_delay);
    }

    public void setSystemProcess() {
        this.mTransferService.publish(this.mActivityManagerService.mContext, false);
    }

    public void asyncStartPrefetch(final ArrayList<String> apps_l) {
        AsyncTask.THREAD_POOL_EXECUTOR.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    if (apps_l.size() > 0) {
                        Bundle bParams = new Bundle();
                        bParams.putStringArrayList("start_empty_apps", apps_l);
                        ActivityManagerServiceSmtBase.this.mActivityManagerService.startActivityAsUserEmpty(bParams);
                    }
                } catch (Exception e) {
                    Slog.w("ActivityManagerService", "Failed start prefetch app " + apps_l, e);
                }
            }
        });
    }

    public void doFreezeForPendingApp() {
        synchronized (this.mPendingFreezePids) {
            if (this.mPendingFreezePids.size() > 0) {
                int pid = this.mPendingFreezePids.remove(0).intValue();
                synchronized (this.mActivityManagerService) {
                    try {
                        ActivityManagerService.boostPriorityForLockedSection();
                        freezePrefetchAppLocked(pid);
                    } catch (Throwable th) {
                        ActivityManagerService.resetPriorityAfterLockedSection();
                        throw th;
                    }
                }
                ActivityManagerService.resetPriorityAfterLockedSection();
            }
        }
    }

    public void resumeDelayFreezingAppActivity(int pid) {
        ProcessRecord app = null;
        ActivityManagerService activityManagerService = this.mActivityManagerService;
        if (pid != ActivityManagerService.MY_PID && pid >= 0) {
            synchronized (this.mActivityManagerService.mPidsSelfLocked) {
                app = this.mActivityManagerService.mPidsSelfLocked.get(pid);
            }
        }
        if (app != null && app.getSmtEx().delayFreezing) {
            this.mActivityManagerService.mAtmInternal.resumeTopActivities(false);
        }
    }

    private void freezePrefetchAppLocked(int pid) {
        ProcessRecord processRecord;
        ProcessRecord app = null;
        if (pid != ActivityManagerService.MY_PID && pid >= 0) {
            synchronized (this.mActivityManagerService.mPidsSelfLocked) {
                app = this.mActivityManagerService.mPidsSelfLocked.get(pid);
            }
        }
        if (app != null) {
            if (app.info != null && app.info.getSmtEx().doPrefetch == 4) {
                if (app.getSmtEx().attachCompleted) {
                    if (app.getCurProcState() > 2 && app.setProcState > 2 && (processRecord = this.mFocusedApp) != null && processRecord.info.uid != app.info.uid) {
                        SysOptBridge.getFactory().getApplicationFreezer().freezeProcessLocked(app, true, IApplicationFreezer.FreezeReason.PREFETCH);
                        app.getWindowProcessController().getWPCSmtEx().freezeFromPrefetch = true;
                        SysOptBridge.getFactory().getSysPrefetchService().notifyPrefetched(app.info.packageName, app.uid);
                    }
                    app.info.getSmtEx().doPrefetch = 0;
                } else {
                    synchronized (this.mPendingFreezePids) {
                        this.mPendingFreezePids.add(Integer.valueOf(pid));
                    }
                }
            }
            app.getSmtEx().delayFreezing = false;
        }
        if (this.mPendingFreezePids.size() > 0) {
            SysOptBridge.getFactory().getSysPrefetchService().sendPendingFreezePrefetchMsg();
        }
    }

    public int getPrefetchSize() {
        return this.mActivityManagerService.mProcessList.getSmtEx().mPrefetchProcess.size();
    }

    public void doFreezeForCurrentApp(int pid) {
        synchronized (this.mActivityManagerService) {
            try {
                ActivityManagerService.boostPriorityForLockedSection();
                freezePrefetchAppLocked(pid);
            } finally {
                ActivityManagerService.resetPriorityAfterLockedSection();
            }
        }
    }

    public class ForceCpusetProc {
        int curProcessGroup;
        int pid;
        long settingTime;
        long timeOut;

        public ForceCpusetProc(int pid, int processGroup, long timeOut) {
            this.pid = pid;
            this.curProcessGroup = processGroup;
            this.timeOut = timeOut;
        }

        public void forceSetCpuset() {
            this.settingTime = SystemClock.uptimeMillis();
            try {
                Process.setProcessGroup(this.pid, this.curProcessGroup);
            } catch (Exception e) {
                Slog.w("ActivityManagerService", "Failed setting process group of " + this.pid + " to " + this.curProcessGroup, e);
            }
        }

        public boolean resetForceCpusetProcIfTimeOut(long currentTime) {
            long j = this.timeOut;
            if (j <= 0 || currentTime - this.settingTime <= j) {
                return false;
            }
            this.curProcessGroup = -1;
            forceSetCpuset();
            return true;
        }
    }

        public void sendCheckForceCpusetProcTask(long delayTime) {
        synchronized (this.mForceCpusetProcs) {
            if (!this.mPostedCheckCpusetTask) {
                BackgroundThread.getHandler().postDelayed(this.mCheckForceCpusetProcTask, delayTime);
                this.mPostedCheckCpusetTask = true;
            }
        }
    }

    private void addNewForceCpusetProc(int pid, int processGroup, long timeOut) {
        synchronized (this.mForceCpusetProcs) {
            ForceCpusetProc proc = new ForceCpusetProc(pid, processGroup, timeOut);
            if (timeOut > 0) {
                if (timeOut > CHECK_TASK_DEFAULT_TIME) {
                    timeOut = CHECK_TASK_DEFAULT_TIME;
                }
                sendCheckForceCpusetProcTask(timeOut);
                FeatLog.i("SmtResourceControl", "FEAT_PERF_RES_CONTROL", 30, "send check task for add new cpuset proc.");
            }
            proc.forceSetCpuset();
            this.mForceCpusetProcs.put(Integer.valueOf(pid), proc);
            FeatLog.i("SmtResourceControl", "FEAT_PERF_RES_CONTROL", 30, "add new force cpuset proc :" + pid + "  processGroup : " + processGroup + "  timeout" + timeOut);
        }
    }

    private void removeForceCpusetProc(int pid) {
        synchronized (this.mForceCpusetProcs) {
            ForceCpusetProc proc = this.mForceCpusetProcs.remove(Integer.valueOf(pid));
            if (proc != null) {
                proc.curProcessGroup = -1;
                proc.forceSetCpuset();
                FeatLog.i("SmtResourceControl", "FEAT_PERF_RES_CONTROL", 40, "remove force cpuset proc :" + pid);
            }
        }
    }

    public void updateProcessFinalCpusetLevel(int pid, int cpusetLevel, int scenes, long timeOut, boolean force) {
        int cpusetLevel2 = (cpusetLevel < 0 || cpusetLevel >= 6) ? 0 : cpusetLevel;
        synchronized (this.mActivityManagerService) {
            try {
                ActivityManagerService.boostPriorityForLockedSection();
                ProcessRecord app = null;
                for (int i = this.mActivityManagerService.mProcessList.mLruProcesses.size() - 1; i >= 0; i--) {
                    ProcessRecord proc = this.mActivityManagerService.mProcessList.mLruProcesses.get(i);
                    if (proc.pid == pid) {
                        app = proc;
                        FeatLog.i("SmtResourceControl", "FEAT_PERF_RES_CONTROL", 20, "find process :" + proc);
                        break;
                    }
                }
                if (app != null) {
                    app.getSmtEx().updateFinalCpusetLevel(cpusetLevel2, scenes, timeOut);
                    FeatLog.i("SmtResourceControl", "FEAT_PERF_RES_CONTROL", 20, "update lru process  :" + app + "  final cpuset level: " + cpusetLevel2 + "  timeout" + timeOut + "   scenes : " + scenes);
                }
                if (force) {
                    int processGroup = -1;
                    if (cpusetLevel2 == 1) {
                        processGroup = 5;
                    } else if (cpusetLevel2 == 3) {
                        processGroup = 8;
                    } else if (cpusetLevel2 == 4) {
                        processGroup = 9;
                    } else if (cpusetLevel2 == 5) {
                        processGroup = 14;
                    }
                    if (cpusetLevel2 == 0) {
                        removeForceCpusetProc(pid);
                    } else {
                        addNewForceCpusetProc(pid, processGroup, timeOut);
                        FeatLog.i("SmtResourceControl", "FEAT_PERF_RES_CONTROL", 20, "update force process  :" + app + "  final cpuset level: " + cpusetLevel2 + "  timeout" + timeOut + "   scenes : " + scenes);
                    }
                }
            } finally {
                ActivityManagerService.resetPriorityAfterLockedSection();
            }
        }
    }

    public void setProcessRunningCpuset(int pid, int cpusetLevel, long timeOut, boolean force) {
        SysOptBridge.getFactory().getSmtResourceControl().setProcessRunningCpuset(pid, cpusetLevel, timeOut, force);
    }

    boolean checkApplicationPrefetchStatus(boolean notPrefetch, ActivityInfo aInfo, String app_str) {
        if (!notPrefetch && SysOptBridge.getFactory().getSysPrefetchService().isDoPrefetch() && aInfo.applicationInfo != null) {
            if (!aInfo.applicationInfo.getSmtEx().goodToStartInBG(SystemClock.elapsedRealtime())) {
                return false;
            }
            if (aInfo.applicationInfo.isSystemApp()) {
                SysOptBridge.getFactory().getSysPrefetchService().addSystemAppNoPrefetch(app_str);
                return false;
            }
            ProcessRecord prefetchProcess = (ProcessRecord) this.mActivityManagerService.mProcessList.getSmtEx().mPrefetchProcess.get(app_str, aInfo.applicationInfo.uid);
            if (prefetchProcess == null && (prefetchProcess = (ProcessRecord) this.mActivityManagerService.mProcessList.mProcessNames.get(app_str, aInfo.applicationInfo.uid)) == null) {
                prefetchProcess = SysOptBridge.getFactory().getApplicationFreezer().get(app_str, aInfo.applicationInfo.uid);
            }
            if (prefetchProcess != null) {
                return false;
            }
            if (!mSmtOptEx.isFreezeEnable()) {
                aInfo.applicationInfo.getSmtEx().doPrefetch = 1;
            } else {
                aInfo.applicationInfo.getSmtEx().doPrefetch = 4;
            }
            FeatLog.d("ActivityManagerService", "FEAT_PERF_PREFETCH", 0, "system_server: start prefetch process = " + app_str + ", doPrefetch = " + aInfo.applicationInfo.getSmtEx().doPrefetch);
        }
        return true;
    }

    boolean attachApplicationCheckPrefetch(ProcessRecord app) {
        if (app.info.getSmtEx().doPrefetch == 2) {
            app.verifiedAdj = 900;
            app.curAdj = 900;
            app.setAdj = 900;
            ProcessList.setOomAdj(app.pid, app.uid, app.curAdj);
            SysOptBridge.getFactory().getSysPrefetchService().notifyPrefetched(app.info.packageName, app.uid);
            FeatLog.d("ActivityManagerService", "FEAT_PERF_PREFETCH", 20, "system_server: process = " + app.processName + "    prefetch attach application done!");
            return true;
        }
        return false;
    }

    void needUpdateLruForPrefetchApp(ProcessRecord app) {
        SysMonitorSvcBridge.getFactory().getSysPerfMonitorService().updateUidVersion(app.uid, app.info.longVersionCode);
        if (app.info.getSmtEx().doPrefetch == 3) {
            FeatLog.d("ActivityManagerService", "FEAT_PERF_PREFETCH", 40, "system_server: update LRU for prefetch process = " + app.processName);
            this.mActivityManagerService.updateLruProcessLocked(app, false, null);
            long jUptimeMillis = SystemClock.uptimeMillis();
            app.lastLowMemory = jUptimeMillis;
            app.lastRequestedGc = jUptimeMillis;
        }
    }

    void attachApplicationEnd(ProcessRecord app) {
        app.getSmtEx().attachCompleted = true;
        if (app.info.getSmtEx().doPrefetch != 4) {
            if (app.info.getSmtEx().doPrefetch == 3) {
                SysOptBridge.getFactory().getSysPrefetchService().notifyPrefetchSuccess(app.info.packageName, app.uid);
                FeatLog.d("ActivityManagerService", "FEAT_PERF_PREFETCH", 50, "system_server: complete attach application for process = " + app.processName);
            }
            app.info.getSmtEx().doPrefetch = 0;
        }
        app.getSmtEx().isStartDuringPrefetch = false;
    }

    public IActivityManagerSmtEx getISmtEx() {
        return this.mIActivityManagerSmtEx;
    }

    protected class IActivityManagerSmtExBase extends IActivityManagerSmtEx.Stub {
        protected IActivityManagerSmtExBase() {
        }

        public void setAppSlowMainOperations(List<String> slowOperations, int index) {
            ActivityManagerServiceSmtBase.this.setAppSlowMainOperations(slowOperations, index);
        }

        public long getRomFreeMemoryKb() {
            ActivityManagerServiceSmtBase activityManagerServiceSmtBase = ActivityManagerServiceSmtBase.this;
            return ActivityManagerServiceSmtBase.getRomFreeMemoryKb();
        }

        public String getSmtExtraInfo(int pid) {
            return ActivityManagerServiceSmtBase.this.getSmtExtraInfo(pid);
        }

        public void setSmtExtraInfo(int pid, String info) {
            ActivityManagerServiceSmtBase.this.setSmtExtraInfo(pid, info);
        }

        public void forceStopPackageSmart(String packageName, int userId, int taskId, int cleanLevel) {
            ActivityManagerServiceSmtBase.this.forceStopPackageSmart(packageName, userId, taskId, cleanLevel);
        }

        public String getLastword(int pid) {
            return ActivityManagerServiceSmtBase.this.getLastword(pid);
        }

        public List<String> getPrefetchApps() {
            return ActivityManagerServiceSmtBase.this.getPrefetchApps();
        }

        public int[] getPrefetchPids() {
            return ActivityManagerServiceSmtBase.this.getPrefetchPids();
        }

        public void freezePrefetchApp() {
            ActivityManagerServiceSmtBase.this.freezePrefetchApp();
        }

        public void registerSysClient(ISysClient client) {
            ActivityManagerServiceSmtBase.this.registerSysClient(client);
        }

        public void setProcessRunningCpuset(int pid, int cpusetLevel, long timeOut, boolean force) {
            ActivityManagerServiceSmtBase.this.setProcessRunningCpuset(pid, cpusetLevel, timeOut, force);
        }

        public boolean getUidFrozen(int uid) {
            return ActivityManagerServiceSmtBase.this.getUidFrozen(uid);
        }

        public ApplicationInfo getTopApplication() {
            return ActivityManagerServiceSmtBase.this.getTopApplication();
        }

        public void createHprof(int pid, String processName, IMemClient client, long dalvikAlloc, long dalvikMax) {
            ActivityManagerServiceSmtBase.this.createHprof(pid, processName, client, dalvikAlloc, dalvikMax);
        }

        public void cropHprofDone(String path, boolean delete) {
            ActivityManagerServiceSmtBase.this.cropHprofDone(path, delete);
        }

        public void checkHprof() {
            ActivityManagerServiceSmtBase.this.checkHprof();
        }

        public int getStrictModeFlags() {
            return ActivityManagerServiceSmtBase.this.mStrictModeFlags;
        }

        public void registerActivityLifeCycleObserver(IActivityLifeCycleObserver observer) {
            ActivityManagerServiceSmtBase.this.mActivityManagerService.enforceCallingPermission("com.smartisanos.permission.observe.activity.lifecycle", "registerActivityLifeCycleObserver");
            SysOptBridge.getFactory().getActivityManager(ActivityManagerServiceSmtBase.this.mActivityManagerService).registerActivityLifeCycleObserver(observer);
        }

        public void unregisterActivityLifeCycleObserver(IActivityLifeCycleObserver observer) {
            ActivityManagerServiceSmtBase.this.mActivityManagerService.enforceCallingPermission("com.smartisanos.permission.observe.activity.lifecycle", "unregisterActivityLifeCycleObserver");
            SysOptBridge.getFactory().getActivityManager(ActivityManagerServiceSmtBase.this.mActivityManagerService).unregisterActivityLifeCycleObserver(observer);
        }

        public void registerAppStartEventObserver(IAppStartEventObserver observer) {
            ActivityManagerServiceSmtBase.this.mActivityManagerService.enforceCallingPermission("com.smartisanos.permission.observe.app.startevent", "registerAppStartEventObserver");
            SysOptBridge.getFactory().getActivityManager(ActivityManagerServiceSmtBase.this.mActivityManagerService).registerAppStartEventObserver(observer);
        }

        public void unregisterAppStartEventObserver(IAppStartEventObserver observer) {
            ActivityManagerServiceSmtBase.this.mActivityManagerService.enforceCallingPermission("com.smartisanos.permission.observe.app.startevent", "unregisterAppStartEventObserver");
            SysOptBridge.getFactory().getActivityManager(ActivityManagerServiceSmtBase.this.mActivityManagerService).unregisterAppStartEventObserver(observer);
        }

        public void frozenObjectFromNative(ApplicationErrorReport.ParcelableCrashInfo crashInfo) {
            String processName;
            int callingPid = Binder.getCallingPid();
            int callingUid = Binder.getCallingUid();
            long origId = Binder.clearCallingIdentity();
            ProcessRecord r = null;
            if (callingPid == ActivityManagerService.MY_PID) {
                processName = "system_server";
            } else {
                synchronized (ActivityManagerServiceSmtBase.this.mActivityManagerService.mPidsSelfLocked) {
                    r = ActivityManagerServiceSmtBase.this.mActivityManagerService.mPidsSelfLocked.get(callingPid);
                }
                processName = r == null ? "unknown" : r.processName;
            }
            Slog.i(ActivityManagerServiceSmtBase.FROZEN_OBJECT_TAG, "target process is frozen, client name:" + processName + " pid:" + callingPid + " uid:" + callingUid);
            ActivityManagerServiceSmtBase.this.mActivityManagerService.addErrorToDropBox("customerror", r, processName, null, null, null, ActivityManagerServiceSmtBase.FROZEN_OBJECT_TAG, null, null, crashInfo, ActivityManagerServiceSmtBase.CUSTOM_ERROR_TYPE_FROZEN_OBJECT);
            Binder.restoreCallingIdentity(origId);
        }

        public void updatePrefetchApps(List<String> needPrefetchApps, int flag) {
            SysOptBridge.getFactory().getSysPrefetchService().updatePrefetchApps(needPrefetchApps, flag);
            if (SysOptBridge.getFactory().getPrefetchManager().getPrefetchEnable()) {
                SysOptBridge.getFactory().getPrefetchManager().updatePrefetchApp(needPrefetchApps, flag);
            }
            SysOptBridge.getFactory().getMemoryProcessController().updatePrefetchApp(needPrefetchApps);
        }

        public void killMemoryLeakProcess(String processName, int pid) {
            SysOptBridge.getFactory().getHandleMemoryLeak().killMemoryLeakProcess(processName, pid);
        }

        public void backtraceDoneInform(String processName, int pid) {
            SysMonitorSvcBridge.getFactory().getMemoryStrategy().backtraceDoneInform(processName, pid);
        }

        public void clearAllKeepAliveProc() {
            SysOptBridge.getFactory().getMemoryProcessController().clearKeepAliveProcesses();
        }

        public boolean isScreenOn() {
            return ActivityManagerServiceSmtBase.this.isScreenOn();
        }
    }

    public boolean isScreenOn() {
        return this.mActivityManagerService.mWakefulness == 1;
    }

    public void executeMemoryStrategy(String processName, int pid, long pss, int oomAdj) {
        SysMonitorSvcBridge.getFactory().getMemoryStrategy().executeMemoryStrategy(processName, pid, pss, oomAdj);
    }

    public void executeMeminfoMemoryStrategy(MemInfoReader memInfo, HashMap<String, long[]> processMems, long usedPss, long cachedPss, long ionHeapOther) {
        SysMonitorSvcBridge.getFactory().getMemoryStrategy().executeMeminfoMemoryStrategy(memInfo, processMems, usedPss, cachedPss, ionHeapOther);
    }

    public void setSoundProcessMemoryStrategy(boolean flag) {
        SysMonitorSvcBridge.getFactory().getMemoryStrategy().setSoundProcessMemoryStrategy(flag);
    }

    public boolean getUidFrozen(int uid) {
        synchronized (this.mActivityManagerService) {
            try {
                ActivityManagerService.boostPriorityForLockedSection();
                UidRecord uidRecord = this.mActivityManagerService.mProcessList.getUidRecordLocked(uid);
                if (uidRecord == null) {
                    return false;
                }
                return uidRecord.getSmtEx().curFrozenStat != 0;
            } finally {
                ActivityManagerService.resetPriorityAfterLockedSection();
            }
        }
    }

    boolean shouldPutBackground(ProcessRecord app, long nowElapsed) {
        if (!app.info.getSmtEx().goodToOperateProc(nowElapsed, 4)) {
            if (ActivityManagerDebugConfigSmtEx.DEBUG_3RD_BG_APP) {
                Slog.d("ActivityManagerService", "app: " + app + " was perceptible at: " + app.info.getSmtEx().perceptibleTime + ", nowElapsed is: " + nowElapsed);
            }
            return false;
        }
        if (!SysOptBridge.getFactory().getActivityManager(this.mActivityManagerService).getmEnablePeropt()) {
            return false;
        }
        if (app.info.getSmtEx().isLimited != 0 || SysOptBridge.getFactory().getActivityManager(this.mActivityManagerService).getmUidCpuRunner().getCpuBusyCount() >= 5) {
            return !(SysOptBridge.getFactory().getActivityManager(this.mActivityManagerService).getmUidCpuRunner().getCpuBusyCount() == 0 && SysOptBridge.getFactory().getActivityManager(this.mActivityManagerService).getmPackStats().getOrder(app.uid, 100) < 3 && SysOptBridge.getFactory().getActivityManager(this.mActivityManagerService).getmPackStats().isRecent(nowElapsed, 900000L, app.uid)) && (app.info.getSmtEx().smartisanFlag & 2) == 0;
        }
        if (ActivityManagerDebugConfigSmtEx.DEBUG_3RD_BG_APP) {
            Slog.d("ActivityManagerService", "app: " + app + " unlimited: " + app.info.getSmtEx().isLimited);
        }
        return false;
    }

    public void setAppSlowMainOperations(List<String> slowOperations, int index) {
        this.mTransferService.setAppSlowMainOperation(slowOperations, index);
    }

    public void setSmtExtraInfo(int pid, String info) {
        synchronized (this.mActivityManagerService.mPidsSelfLocked) {
            ProcessRecord proc = this.mActivityManagerService.mPidsSelfLocked.get(pid);
            if (proc != null) {
                proc.getSmtEx().setSmtExtraInfo(info);
            }
        }
    }

    public List<String> getPrefetchApps() {
        List<String> prefetchApps;
        synchronized (this.mPrefetchApps) {
            prefetchApps = new ArrayList<>();
            prefetchApps.addAll(this.mPrefetchApps.values());
        }
        return prefetchApps;
    }

    public void addPrefetchApp(int pid, String packageName) {
        synchronized (this.mPrefetchApps) {
            this.mPrefetchApps.put(Integer.valueOf(pid), packageName);
        }
    }

    public void removePrefetchApp(int pid) {
        synchronized (this.mPrefetchApps) {
            this.mPrefetchApps.remove(Integer.valueOf(pid));
        }
    }

    public int[] getPrefetchPids() {
        int[] array;
        synchronized (this.mPrefetchApps) {
            List<Integer> pids = new ArrayList<>();
            pids.addAll(this.mPrefetchApps.keySet());
            array = pids.stream().mapToInt(new ToIntFunction() {
                @Override
                public final int applyAsInt(Object obj) {
                    return Integer.valueOf(((Integer) obj).intValue()).intValue();
                }
            }).toArray();
        }
        return array;
    }

    public void freezePrefetchApp() {
        synchronized (this.mActivityManagerService) {
            try {
                ActivityManagerService.boostPriorityForLockedSection();
                int callingPid = Binder.getCallingPid();
                long origId = Binder.clearCallingIdentity();
                ProcessRecord app = null;
                if (callingPid != ActivityManagerService.MY_PID && callingPid >= 0) {
                    synchronized (this.mActivityManagerService.mPidsSelfLocked) {
                        app = this.mActivityManagerService.mPidsSelfLocked.get(callingPid);
                    }
                }
                if (app != null) {
                    app.getSmtEx().delayFreezing = true;
                    SysOptBridge.getFactory().getSysPrefetchService().sendFreezeCurrentPrefetchMsg(callingPid);
                }
                Binder.restoreCallingIdentity(origId);
            } catch (Throwable th) {
                ActivityManagerService.resetPriorityAfterLockedSection();
                throw th;
            }
        }
        ActivityManagerService.resetPriorityAfterLockedSection();
    }

    public void forceStopPackageSmart(String packageName, int userId, int taskId, int cleanLevel) {
        long callingId = Binder.clearCallingIdentity();
        try {
            this.mActivityManagerService.removeTask(taskId);
            if (0 != 0) {
                Slog.d("ActivityManagerService", "Smart forceStopPackage: packageName=" + packageName + ", userId=" + userId + " taskId=" + taskId + " cleanLevel=" + cleanLevel);
                SysOptBridge.getFactory().getTaskDeepClean().addTdcEvent(1, cleanLevel, packageName, 0);
                this.mActivityManagerService.forceStopPackage(packageName, userId);
            }
        } finally {
            Binder.restoreCallingIdentity(callingId);
        }
    }

    public final void registerSysClient(ISysClient client) {
        int callingPid = Binder.getCallingPid();
        SysMonitorSvcBridge.getFactory().getAnrMonitor().addClient(callingPid, client);
    }

    public String getSmtExtraInfo(int pid) {
        synchronized (this.mActivityManagerService.mPidsSelfLocked) {
            ProcessRecord proc = this.mActivityManagerService.mPidsSelfLocked.get(pid);
            if (proc != null) {
                long bootTime = System.currentTimeMillis() - SystemClock.elapsedRealtime();
                return proc.getSmtEx().getSmtExtraInfo() + "\nstart_time : " + proc.startTime + bootTime;
            }
            return null;
        }
    }

    public String getLastword(int pid) {
        return ProcExtraInfoSmtBase.getInstance().getLastWordOfPid(pid);
    }

    public void asyncSetProcessGroup(final int pid, final int processGroup) {
        AsyncTask.THREAD_POOL_EXECUTOR.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    Process.setProcessGroup(pid, processGroup);
                } catch (Exception e) {
                    Slog.w("ActivityManagerService", "Failed setting process group of " + pid + " to " + processGroup, e);
                }
            }
        });
    }

    public static long getRomFreeMemoryKb() {
        File path = Environment.getDataDirectory();
        StatFs stat = new StatFs(path.getPath());
        long availableBytes = stat.getAvailableBytes();
        return availableBytes / 1024;
    }

    public final boolean platformAllowKillProcess(ProcessRecord app) {
        return true;
    }

    public boolean isTNTPadMode() {
        return false;
    }

    public boolean checkApplication(ProcessRecord app) {
        ArrayList<ProcessRecord> procList = getLruProcesses(this.mActivityManagerService);
        for (ProcessRecord proc : procList) {
            if (proc == app) {
                return true;
            }
        }
        return false;
    }

    public void checkCrashMessage(ApplicationErrorReport.CrashInfo crashInfo, ProcessRecord r) {
        if (r != null) {
            try {
                if (r.info.getSmtEx().mOverrideClassSDK == 1 && mOverrideClazzsFiles.indexOf(crashInfo.throwFileName) >= 0) {
                    mOverrideClazzCrashProcs.add(r.processName);
                    this.mActivityManagerService.getPackageManager().getISmtEx().clearOverrideFlag(r.processName);
                }
            } catch (Exception e) {
                Slog.e("ActivityManagerService", "checkCrashMessage failed", e);
            }
        }
    }

    public static void writeConfigFile() {
        try {
            FileOutputStream fstr = new FileOutputStream("/data/system/overrideSdk.xml");
            BufferedOutputStream str = new BufferedOutputStream(fstr);
            FastXmlSerializer fastXmlSerializer = new FastXmlSerializer();
            fastXmlSerializer.setOutput(str, StandardCharsets.UTF_8.name());
            fastXmlSerializer.startDocument(null, true);
            fastXmlSerializer.setFeature("http://xmlpull.org/v1/doc/features.html#indent-output", true);
            fastXmlSerializer.startTag(null, "packages");
            for (String proc : mOverrideClazzCrashProcs) {
                fastXmlSerializer.startTag(null, Settings.ATTR_PACKAGE);
                fastXmlSerializer.attribute(null, "closeOverrideProc", proc);
                fastXmlSerializer.endTag(null, Settings.ATTR_PACKAGE);
            }
            fastXmlSerializer.endTag(null, "packages");
            fastXmlSerializer.endDocument();
            str.flush();
            FileUtils.sync(fstr);
            str.close();
        } catch (Exception e) {
            if (mOverrideClazzCrashProcs.size() > 0) {
                SystemProperties.set("persist.sys.classoverrider.close", "1");
            }
            Slog.e("ActivityManagerService", "writeConfigFile failed", e);
        }
    }

    public void updateAppInfo(ApplicationInfo appInfo) {
        if (appInfo != null) {
            if (appInfo.getSmtEx().isVrApp || appInfo.getSmtEx().downloadProfileFlag) {
                SysMonitorSvcBridge.getFactory().getSysPerfMonitorService().updateAppInfo(appInfo.packageName);
            }
        }
    }

    public void handleUpload(Context context, String packageName, String errorType) {
        try {
            SysMonitorSvcBridge.getFactory().getUploadUtils().handleUpload(context, packageName, errorType);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void readCustomFileConfig() {
        try {
            SysMonitorSvcBridge.getFactory().getUploadUtils().readCustomFileConfig();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void enableAtrace() {
        if (SystemProperties.getInt("persist.sys.debug.atrace.alwayson", 0) == 1) {
            SysMonitorSvcBridge.getFactory().getSysPerfMonitorService().enableAtrace();
        }
    }
}
