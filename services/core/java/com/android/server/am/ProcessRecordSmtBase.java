// Copyright 2026 Picomisu contributors
// SPDX-License-Identifier: Apache-2.0

package com.android.server.am;

import android.app.IApplicationThread;
import android.content.ComponentName;
import android.os.Bundle;
import android.os.Debug;
import android.os.RemoteException;
import android.os.SystemClock;
import android.util.ArraySet;
import android.util.Slog;
import android.util.SmtUidUtil;
import com.android.server.ApplicationFreezerHelperSmt;
import com.android.server.ApplicationFreezerInternalSmt;
import com.android.server.ISmtResourceControl;
import com.android.server.SysOptBridge;
import com.android.server.wm.FrozenPendingEvent;
import com.android.server.wm.WindowProcessListenerSmtBase;
import java.util.ArrayList;
import smartisanos.util.FeatLog;
import smartisanos.util.SmtRingBuffer;

/**
 * Reconstructed from the PICO OS 5.13.7 factory services.
 *
 * @hide
 */
public class ProcessRecordSmtBase implements WindowProcessListenerSmtBase {
    public static final int FREEZING = 1;
    public static final int FROZEN = 2;
    public static final int NONE = 0;
    static final String PSS_TAG = "pss_history";
    public static final int TYPE_ACTIVE_BACKGROUND = 1;
    public static final int TYPE_BOOST_TO_TOP_GROUP = 2;
    public static final int TYPE_NONE = 0;
    public int cantSwap;
    int collectBadAppLevel;
    int freezingStat;
    public long graphicKeepAliveTime;
    public boolean hasOnGoingNotification;
    public boolean isPrefetch;
    public boolean isVRKeepAlive;
    int killType;
    public String killedReason;
    public long lastVoiceTime;
    protected ProcessRecord mProcessRecord;
    int notFreezeReason;
    int pid;
    public long prefetchFrozenTime;
    protected String smtExtraInfo;
    public int stickineFraction;
    public int swapPercent;
    IApplicationThread thread;
    public IApplicationFreezer.UnfreezeReason unFreezeReason;
    public static boolean mResControlLog = false;
    private static int TIME_PSS_COLLECT_INTERVAL_1_MIN = 60000;
    private static int TIME_PSS_COLLECT_INTERVAL_2_MINS = 120000;
    boolean isStartDuringPrefetch = false;
    boolean delayFreezing = false;
    boolean freezeFromPrefetch = false;
    boolean attachCompleted = false;
    int not3rdReasonFlag = -1;
    boolean launchFromPrefetchUnfreeze = false;
    public boolean hotLaunchStage = false;
    int smtUid = 1000;
    int mCompactedType = 0;
    long mLastCompactedTime = 0;
    long memInUfsSize = -1;
    public boolean isKeepAlive = false;
    public boolean isPreviousAlive = false;
    boolean mainProc = false;
    boolean browserGpuProc = false;
    boolean setCgroup = false;
    boolean gpuPriorityPromoted = false;
    public int mFinalLaunchCpusetLevel = 0;
    public long mLaunchCpusetLevelUpdateTime = 0;
    public long mLaunchCpusetTimeOut = ISmtResourceControl.LAUNCH_CPUSET_EFFECTIVE_TIME;
    public int mFinalRunningCpusetLevel = 0;
    public long mRunningCpusetLevelUpdateTime = 0;
    public long mRunningCpusetTimeOut = 3600000;
    boolean adjHasReachedZero = false;
    int pssCount = 0;
    boolean hasUpdated = false;
    SmtRingBuffer<Long> history = new SmtRingBuffer<>(3);
    IApplicationFreezer.Mode freezeMode = IApplicationFreezer.Mode.INVALID;
    int freezeBlockFlags = 0;
    private ArraySet publishingProviders = new ArraySet();
    IFreezeStats freezeStats = SysOptBridge.getFactory().createFreezeStats();
    boolean inSwapfile = false;
    long failedFrozenTime = 0;
    long failedPushTime = 0;
    boolean isColdLaunchDone = false;
    public boolean unFreezeForLaunch = false;
    private ApplicationFreezerInternalSmt.IFrozenCallback mFrozenCallback = new ApplicationFreezerInternalSmt.IFrozenCallback() {
        @Override
        public void onAppFreeze(int pid, int uid) {
        }

        @Override
        public void onAppUnfreeze(int pid, int uid) {
            ProcessRecordSmtBase.this.syncAppData();
        }
    };
    boolean isFreezingWhileServiceBringdown = false;
    ServiceRecord sr = null;
    ServiceRecord firstErrSer = null;
    ComponentName errName = null;
    ComponentName errComp = null;
    ComponentName errClassName = null;
    long firstErrTime = 0;
    int origAdj = -10000;
    int smartisanFlag = 0;
    public long mEGL = 0;
    public long mGL = 0;
    public long mPss = 0;
    Debug.MemoryInfo memoryInfo = null;
    Bundle cacheData = new Bundle();
    private boolean mXrCrashed = false;

    public void setPid(int _pid) {
        this.setCgroup = false;
    }

    public void resetPreviousAlive() {
        this.isPreviousAlive = false;
    }

    public int getSmtUid(int uid, String packageName) {
        if (uid != 1000) {
            return uid;
        }
        if (this.smtUid == 1000) {
            this.smtUid = SmtUidUtil.getSystemUidForPackage(packageName);
        }
        int result = this.smtUid;
        return result;
    }

    public int getSmtUid() {
        int result = this.mProcessRecord.uid;
        if (this.mProcessRecord.info != null) {
            int result2 = getSmtUid(this.mProcessRecord.info.uid, this.mProcessRecord.info.packageName);
            return result2;
        }
        return result;
    }

    public boolean checkCpusetEffectiveTime(long now) {
        long j = this.mRunningCpusetTimeOut;
        if (j >= 0 && now - this.mRunningCpusetLevelUpdateTime >= j && now - this.mLaunchCpusetLevelUpdateTime >= this.mLaunchCpusetTimeOut) {
            return false;
        }
        return true;
    }

    public int computeCurrentSchedulingGroup(int defaultSchedGroup, long now) {
        int i;
        int schedGroup = defaultSchedGroup;
        if (this.mFinalLaunchCpusetLevel != 0 || this.mFinalRunningCpusetLevel != 0) {
            if (checkCpusetEffectiveTime(now)) {
                int i2 = this.mFinalLaunchCpusetLevel;
                if (i2 == 1 || (i = this.mFinalRunningCpusetLevel) == 1) {
                    schedGroup = 3;
                } else if (i2 == 2 || i == 2) {
                    schedGroup = 2;
                } else if (i == 3) {
                    schedGroup = 8;
                } else if (i == 4) {
                    schedGroup = 9;
                } else if (i == 5) {
                    schedGroup = 12;
                }
            } else {
                this.mFinalLaunchCpusetLevel = 0;
            }
            if (mResControlLog) {
                FeatLog.i("SmtResourceControl", "FEAT_PERF_RES_CONTROL", 40, "process  :" + this.mProcessRecord + "  final sched group  : " + schedGroup + "   default group :" + defaultSchedGroup);
            }
        }
        return schedGroup;
    }

    public void updateFinalCpusetLevel(int cpusetLevel, int scenes, long timeOut) {
        if (scenes == 1) {
            this.mFinalLaunchCpusetLevel = cpusetLevel;
            if (cpusetLevel != 0) {
                this.mLaunchCpusetLevelUpdateTime = SystemClock.uptimeMillis();
            }
            this.mLaunchCpusetTimeOut = timeOut;
        } else if (scenes == 2) {
            this.mFinalRunningCpusetLevel = cpusetLevel;
            if (cpusetLevel != 0) {
                this.mRunningCpusetLevelUpdateTime = SystemClock.uptimeMillis();
            }
            this.mRunningCpusetTimeOut = timeOut;
        }
        FeatLog.i("SmtResourceControl", "FEAT_PERF_RES_CONTROL", 30, "process  :" + this.mProcessRecord + "  cpuset has been set to : " + cpusetLevel + "  timeout" + timeOut + "   scenes : " + scenes);
    }

    public ProcessRecordSmtBase(ProcessRecord processRecord) {
        this.mProcessRecord = processRecord;
    }

    boolean isColdLaunchDone() {
        return this.isColdLaunchDone;
    }

    void addPssHistoryLocked(long now, long pss, ProcessRecord proc) {
        proc.mSmtEx.pssCount++;
        this.history.add(Long.valueOf(pss));
        proc.mSmtEx.hasUpdated = true;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder(128);
        sb.append("PRSmt{");
        sb.append(Integer.toHexString(System.identityHashCode(this)));
        sb.append(" p:");
        sb.append(this.pid);
        sb.append(" t:");
        sb.append(this.thread);
        sb.append(" sa:");
        sb.append(this.mProcessRecord.setAdj);
        if (this.mProcessRecord.uidRecord != null) {
            sb.append(" usps:");
            sb.append(this.mProcessRecord.uidRecord.setProcState);
        }
        sb.append(" sps:");
        sb.append(this.mProcessRecord.setProcState);
        sb.append(" kt:");
        sb.append(this.killType);
        sb.append(" fs:");
        sb.append(this.freezingStat);
        sb.append(" inSfile:");
        sb.append(this.inSwapfile);
        sb.append(" freezeBlockFlags:");
        sb.append(this.freezeBlockFlags);
        sb.append(" pendingInstallProviderCount:");
        sb.append(this.publishingProviders.size());
        sb.append('}');
        return sb.toString();
    }

    static boolean allowPssCollect(ProcessRecord pr, boolean memLowered, boolean wholeCollect, long now) {
        if (!memLowered && pr.mSmtEx.pssCount > 5) {
            long timeInterval = TIME_PSS_COLLECT_INTERVAL_2_MINS;
            if (wholeCollect) {
                timeInterval = TIME_PSS_COLLECT_INTERVAL_1_MIN;
            }
            if (pr.mSmtEx.adjHasReachedZero && now < pr.lastPssTime + timeInterval) {
                pr.mSmtEx.adjHasReachedZero = false;
                if (!pr.mSmtEx.hasUpdated) {
                    return false;
                }
                if (isHighSimilarity(pr.mSmtEx.history)) {
                    if (ActivityManagerDebugConfig.DEBUG_PSS) {
                        Slog.d(PSS_TAG, "-----------------------------------------------------------------------------");
                        Slog.d(PSS_TAG, "little change for latest 3 times pss, pr= " + pr.processName + " pss count= " + pr.mSmtEx.pssCount);
                        for (int i = 0; i < pr.mSmtEx.history.size(); i++) {
                            Slog.d(PSS_TAG, "history= " + pr.mSmtEx.history.get(i) + " (" + i + ")");
                        }
                        Slog.d(PSS_TAG, "pss similarity= " + isHighSimilarity(pr.mSmtEx.history));
                        Slog.d(PSS_TAG, "-----------------------------------------------------------------------------");
                    }
                    pr.mSmtEx.hasUpdated = false;
                    return false;
                }
            }
        }
        return true;
    }

    private static boolean isHighSimilarity(SmtRingBuffer<Long> history) {
        int m = history.size();
        if (m <= 0) {
            return false;
        }
        long sum = 0;
        for (int i = 0; i < m; i++) {
            sum += ((Long) history.get(i)).longValue();
        }
        float dAve = sum / (m * 1.0f);
        for (int i2 = 0; i2 < m; i2++) {
            if (Math.abs(((Long) history.get(i2)).longValue() - dAve) / dAve >= 0.05d) {
                return false;
            }
        }
        return true;
    }

    void setFreezing(ServiceRecord s) {
        this.isFreezingWhileServiceBringdown = true;
        this.sr = s;
    }

    void printDebugLog(ComponentName instanceName) {
        if (this.isFreezingWhileServiceBringdown) {
            Slog.e("ActivityManager", "instanceName:" + instanceName + " errName:" + this.errName + " errComp:" + this.errComp + " errClassName:" + this.errClassName + " sr:" + this.sr + " firstErrSer:" + this.firstErrSer + " firstErrTime:" + this.firstErrTime + " now:" + SystemClock.elapsedRealtime());
        }
    }

    void recordFirstErr(ServiceRecord s) {
        if (this.firstErrSer == null) {
            this.firstErrSer = s;
            this.firstErrTime = SystemClock.elapsedRealtime();
        }
    }

    void recordName(ComponentName name, ComponentName comp, ComponentName className) {
        this.errName = name;
        this.errComp = comp;
        this.errClassName = className;
    }

    boolean isFreezing() {
        return this.freezingStat == 1;
    }

    boolean isFrozen() {
        return this.freezingStat == 2;
    }

    boolean inFreezeStat() {
        return this.freezingStat != 0;
    }

    boolean inLightningMode() {
        return this.freezeMode == IApplicationFreezer.Mode.LIGHTNING;
    }

    boolean inFreezeMode() {
        return this.freezeMode != IApplicationFreezer.Mode.INVALID;
    }

    public static void setHostPid(ProcessRecord host, int _pid) {
        host.pid = _pid;
        host.getWindowProcessController().setPid(_pid);
        host.procStatFile = null;
        host.shortStringName = null;
        host.stringName = null;
    }

    void setCurrentFreezingStat(ActivityManagerService service, ProcessRecord host, int stat) {
        this.freezingStat = stat;
        host.shortStringName = null;
        host.stringName = null;
        if (this.freezingStat == 1) {
            ApplicationFreezerHelperSmt.registerFrozenCallbackByPidOnce(host.pid, host.uid, this.mFrozenCallback);
        }
    }

    public static ArrayList<ProcessRecord> getLruProcesses(ActivityManagerService mService) {
        return mService.mProcessList.mLruProcesses;
    }

    public static int getCurProcState(ProcessRecord r) {
        if (r != null) {
            return r.getCurProcState();
        }
        return -1;
    }

    public static boolean isPersistent(ProcessRecord r) {
        if (r != null) {
            return r.isPersistent();
        }
        return false;
    }

    public static boolean hasActivities(ProcessRecord r) {
        if (r != null) {
            return r.hasActivities();
        }
        return false;
    }

    public static boolean isPcProcess(ProcessRecord r) {
        if (r != null) {
            return r.getSmtEx().isPcMode();
        }
        return false;
    }

    int getCurrentFreezingStat() {
        return this.freezingStat;
    }

    boolean isPcMode() {
        return this.mProcessRecord.mWindowProcessController.isPcMode();
    }

    @Override
    public void unFreezeProcIfNeed(FrozenPendingEvent event) {
        synchronized (this.mProcessRecord.mService) {
            try {
                ActivityManagerService.boostPriorityForLockedSection();
                SysOptBridge.getFactory().getApplicationFreezer().unfreezeAppIfNeededLocked(this.mProcessRecord, event.unfreezeReason, null, null);
            } finally {
                ActivityManagerService.resetPriorityAfterLockedSection();
            }
        }
    }

    public void reportKillingEvent(String reason) {
        if (this.mProcessRecord.mService != null) {
            this.mProcessRecord.getSmtEx().killedReason = reason;
            String killEvent = KillingStatsUtils.buildAmKillingEventItem(getCanonicalName(), this.mProcessRecord.uid, this.mProcessRecord.setAdj, this.mProcessRecord.setProcState, reason);
            this.mProcessRecord.mService.getSmtEx().reportKillingEvent(killEvent);
        }
    }

    @Override
    public void canclePrefetch() {
        SysOptBridge.getFactory().getPrefetchManager().canclePrefetch(this.mProcessRecord.info);
    }

    public String getCanonicalName() {
        if (!this.mProcessRecord.isolated) {
            return this.mProcessRecord.processName;
        }
        return this.mProcessRecord.info.packageName + ":" + this.mProcessRecord.processName;
    }

    @Override
    public String getProcessName() {
        return this.mProcessRecord.processName;
    }

    public int getPid() {
        return this.mProcessRecord.pid > 0 ? this.mProcessRecord.pid : this.pid;
    }

    public String getSmtExtraInfo() {
        return this.smtExtraInfo;
    }

    public void setSmtExtraInfo(String info) {
        this.smtExtraInfo = info;
    }

    public void syncAppData() {
        Bundle settings;
        try {
            if (this.mProcessRecord.thread != null && (settings = this.cacheData.getBundle("core_settings")) != null) {
                this.mProcessRecord.thread.setCoreSettings(settings);
                this.cacheData.remove("core_settings");
            }
        } catch (RemoteException e) {
            Slog.e("PRSmt", "sync app data failed", e);
        }
    }

    void resetFreezeBlockFlagsLocked(int flags) {
        if ((flags & 1) != 0) {
            this.publishingProviders.clear();
            this.freezeBlockFlags &= -2;
        }
    }

    void setFreezeBlockFlagsLocked(int flags, Object privData) {
        if ((flags & 1) != 0) {
            this.freezeBlockFlags |= 1;
            this.publishingProviders.add(privData);
        }
    }

    void clearFreezeBlockFlagsLocked(int flags, Object privData) {
        if ((flags & 1) != 0 && this.publishingProviders.remove(privData) && this.publishingProviders.isEmpty()) {
            this.freezeBlockFlags &= -2;
        }
    }

    public void setXRCrashed() {
        this.mXrCrashed = true;
    }

    public boolean getXRCrashed() {
        return this.mXrCrashed;
    }

    @Override
    public void bringProcessToDefaultLocked() {
        synchronized (this.mProcessRecord.mService) {
            try {
                ActivityManagerService.boostPriorityForLockedSection();
                SysOptBridge.getFactory().getActivityManager(this.mProcessRecord.mService).bringProcessToDefaultLocked(this.mProcessRecord.getWindowProcessController());
            } finally {
                ActivityManagerService.resetPriorityAfterLockedSection();
            }
        }
    }
}
