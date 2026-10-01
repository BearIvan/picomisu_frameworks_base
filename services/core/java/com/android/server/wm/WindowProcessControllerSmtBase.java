// Copyright 2026 Picomisu contributors
// SPDX-License-Identifier: Apache-2.0

package com.android.server.wm;

import android.app.IApplicationThread;
import android.content.res.Configuration;
import android.os.IBinder;
import android.util.Slog;
import android.util.SparseArray;
import com.android.internal.util.function.pooled.PooledLambda;
import com.android.server.am.IApplicationFreezer;
import java.util.List;

/**
 * Reconstructed from the PICO OS 5.13.7 factory services.
 *
 * @hide
 */
public class WindowProcessControllerSmtBase extends ConfigurationContainerSmtBase {
    public static final int COLD_LAUNCH_FOR_UNFREEZE = 1;
    public static final int HOT_LAUNCH_FOR_UNFREEZE = 2;
    private static final String TAG_FREEZE = "AMS.Freeze";
    public static final int UFR_DESTROY_ACTIVITY = 8;
    public static final int UFR_NONE = 0;
    public static final int UFR_RESUME_ACTIVITY = 2;
    public static final int UFR_START_ACTIVITY = 1;
    public static final int UFR_UPDATE_VISIBILITY = 4;
    private final String EMPTY_DES;
    private final int UNMARKED_UID;
    Configuration configuration;
    public boolean freezeFromPrefetch;
    int freezingStat;
    public boolean hasBackgroundActivity;
    public WindowProcessController host;
    public boolean hotLaunchStage;
    public boolean isColdLaunchDone;
    public boolean launchFromPrefetchUnfreeze;
    protected WindowProcessListener listener;
    protected ActivityTaskManagerService mAtm;
    public ActivityTaskManagerServiceSmtBase mAtmsmt;
    private Object mMarkedLock;
    public Configuration mSmtFreeFormConfig;
    public int mTopDisplayType;
    public String nPkg;
    public int nUid;
    SparseArray<FrozenPendingEvent> pendingEvents;
    public boolean previous;
    public int realPid;
    protected IApplicationThread thread;
    public int unFreezeForLaunchType;
    public int unfreezeReason;

    public void markNextVrApp(String pkg, int uid) {
        synchronized (this.mMarkedLock) {
            this.nPkg = pkg;
            this.nUid = uid;
        }
    }

    public boolean isAllActivitiesStopped() {
        synchronized (this.mAtm.mGlobalLock) {
            try {
                WindowManagerService.boostPriorityForLockedSection();
                for (ActivityRecord ar : this.host.mActivities) {
                    if (!ar.getActivityRecordSmtEx().isRecordStateAfterStop()) {
                        WindowManagerService.resetPriorityAfterLockedSection();
                        return false;
                    }
                }
                WindowManagerService.resetPriorityAfterLockedSection();
                return true;
            } catch (Throwable th) {
                WindowManagerService.resetPriorityAfterLockedSection();
                throw th;
            }
        }
    }

    public boolean isMarkToForceStop() {
        boolean ret = false;
        synchronized (this.mMarkedLock) {
            if (this.nUid != -1) {
                ret = true;
            }
        }
        return ret;
    }

    public void clearMarkedNextVrApp() {
        synchronized (this.mMarkedLock) {
            this.nUid = -1;
            this.nPkg = "";
        }
    }

    WindowProcessControllerSmtBase(WindowProcessController _host) {
        super(_host);
        this.freezingStat = 0;
        this.unfreezeReason = 0;
        this.mSmtFreeFormConfig = new Configuration();
        this.hasBackgroundActivity = false;
        this.isColdLaunchDone = false;
        this.launchFromPrefetchUnfreeze = false;
        this.hotLaunchStage = false;
        this.unFreezeForLaunchType = 0;
        this.realPid = -1;
        this.pendingEvents = new SparseArray<>();
        this.mAtmsmt = null;
        this.UNMARKED_UID = -1;
        this.EMPTY_DES = "";
        this.mMarkedLock = new Object();
        this.nPkg = "";
        this.nUid = -1;
        this.host = _host;
    }

    public void setOriginPid(int pid) {
        if (pid > 0) {
            this.realPid = pid;
        }
    }

    protected void init(ActivityTaskManagerService atm, WindowProcessListener _listener) {
        this.listener = _listener;
        this.mAtm = atm;
    }

    public void addPendingEventLocked(int pid, FrozenPendingEvent event) {
        this.unfreezeReason |= amToWmUnfreezeReason(event.unfreezeReason);
        synchronized (this.pendingEvents) {
            FrozenPendingEvent fpe = this.pendingEvents.get(composeKey(pid, event));
            if (fpe == null || !fpe.equals(event)) {
                this.pendingEvents.put(composeKey(pid, event), event);
            }
        }
    }

    public static int composeKey(int pid, FrozenPendingEvent event) {
        int caseValue = 0;
        if (event.unfreezeReason == IApplicationFreezer.UnfreezeReason.NEED_START_ACTIVITY) {
            caseValue = 1;
        } else if (event.unfreezeReason == IApplicationFreezer.UnfreezeReason.NEED_RESUME_ACTIVITY) {
            caseValue = 2;
        } else if (event.unfreezeReason == IApplicationFreezer.UnfreezeReason.NEED_UPDATE_VISIBILITY) {
            caseValue = 3;
        } else if (event.unfreezeReason == IApplicationFreezer.UnfreezeReason.NEED_DESTROY_ACTIVITY) {
            caseValue = 4;
        }
        return (pid * 10) + caseValue;
    }

    public void handleFrozenPendingEventsLocked() {
        SparseArray<FrozenPendingEvent> temp;
        this.unfreezeReason = 0;
        synchronized (this.pendingEvents) {
            temp = this.pendingEvents.clone();
            this.pendingEvents.clear();
        }
        for (int i = 0; i < temp.size(); i++) {
            temp.valueAt(i).handle();
        }
    }
    static class AnonymousClass1 {
        static final int[] $SwitchMap$com$android$server$am$IApplicationFreezer$UnfreezeReason = new int[IApplicationFreezer.UnfreezeReason.values().length];

        static {
            try {
                $SwitchMap$com$android$server$am$IApplicationFreezer$UnfreezeReason[IApplicationFreezer.UnfreezeReason.NEED_START_ACTIVITY.ordinal()] = 1;
            } catch (NoSuchFieldError e) {
            }
            try {
                $SwitchMap$com$android$server$am$IApplicationFreezer$UnfreezeReason[IApplicationFreezer.UnfreezeReason.NEED_RESUME_ACTIVITY.ordinal()] = 2;
            } catch (NoSuchFieldError e2) {
            }
            try {
                $SwitchMap$com$android$server$am$IApplicationFreezer$UnfreezeReason[IApplicationFreezer.UnfreezeReason.NEED_UPDATE_VISIBILITY.ordinal()] = 3;
            } catch (NoSuchFieldError e3) {
            }
            try {
                $SwitchMap$com$android$server$am$IApplicationFreezer$UnfreezeReason[IApplicationFreezer.UnfreezeReason.NEED_DESTROY_ACTIVITY.ordinal()] = 4;
            } catch (NoSuchFieldError e4) {
            }
        }
    }

    private static int amToWmUnfreezeReason(IApplicationFreezer.UnfreezeReason reason) {
        int i = AnonymousClass1.$SwitchMap$com$android$server$am$IApplicationFreezer$UnfreezeReason[reason.ordinal()];
        if (i == 1) {
            return 1;
        }
        if (i == 2) {
            return 2;
        }
        if (i == 3) {
            return 4;
        }
        if (i == 4) {
            return 8;
        }
        return 0;
    }

    void unFreezeProcIfNeedLocked(FrozenPendingEvent event) {
        int size;
        if (this.listener == null || this.freezingStat == 0) {
            return;
        }
        synchronized (this.pendingEvents) {
            size = this.pendingEvents.size();
        }
        Slog.d(TAG_FREEZE, "unFreezeProcIfNeedLocked event:" + event + " uid:" + this.host.mUid + " realPid:" + this.realPid + " pendingEvents.size:" + size);
        if (size > 100) {
            for (StackTraceElement ele : Thread.currentThread().getStackTrace()) {
                Slog.e(TAG_FREEZE, "ele:" + ele);
            }
        }
        addPendingEventLocked(this.realPid, event);
        this.mAtm.mH.sendMessageAtFrontOfQueue(PooledLambda.obtainMessage(
                WindowProcessListenerSmtBase::unFreezeProcIfNeed, this.listener.getSmtEx(), event));
    }

    void canclePrefetch() {
        this.mAtm.mH.sendMessageAtFrontOfQueue(PooledLambda.obtainMessage(
                WindowProcessListenerSmtBase::canclePrefetch, this.listener.getSmtEx()));
    }

    void setFreezingStat(int stat) {
        IApplicationThread iApplicationThread;
        this.freezingStat = stat;
        if (stat == 0 && (iApplicationThread = this.thread) != null) {
            this.host.setThread(iApplicationThread);
            this.thread = null;
        } else if (stat == 2 && this.host.getThread() != null) {
            this.thread = this.host.getThread();
            this.host.setThread(null);
        }
    }

    void finishOtherActivities(ActivityRecord ar) {
        if (ar == null) {
            return;
        }
        for (int i = this.host.mActivities.size() - 1; i >= 0; i--) {
            ActivityRecord other = this.host.mActivities.get(i);
            if (ar != other && other.getActivityStack() != null) {
                other.getActivityStack().finishCurrentActivityLocked(other, 0, false, "finishOtherActivities");
            }
        }
    }

    public boolean hasFocusActivity() {
        synchronized (this.mAtm.mGlobalLock) {
            try {
                WindowManagerService.boostPriorityForLockedSection();
                List<IBinder> visibleActivities = this.mAtm.mRootActivityContainer.getTopVisibleActivities();
                if (visibleActivities != null && !visibleActivities.isEmpty()) {
                    IBinder focusedActivityToken = visibleActivities.get(0);
                    for (ActivityRecord activity : this.host.mActivities) {
                        if (activity == ActivityRecord.forTokenLocked(focusedActivityToken)) {
                            WindowManagerService.resetPriorityAfterLockedSection();
                            return true;
                        }
                    }
                    WindowManagerService.resetPriorityAfterLockedSection();
                    return false;
                }
                WindowManagerService.resetPriorityAfterLockedSection();
                return false;
            } catch (Throwable th) {
                WindowManagerService.resetPriorityAfterLockedSection();
                throw th;
            }
        }
    }

    boolean isFreezing() {
        return this.freezingStat != 0;
    }

    boolean cacheFrozenConfiguration(Configuration config) {
        if (isFreezing()) {
            Configuration configuration = this.configuration;
            if (configuration == null) {
                this.configuration = new Configuration(config);
                return true;
            }
            configuration.setTo(config);
            return true;
        }
        return false;
    }

    public long getAppLaunchTime() {
        return this.host.mInfo.getSmtEx().mTransitionStartTimeNs;
    }

    void bringProcessToDefaultLocked() {
        if (this.listener == null) {
            return;
        }
        this.mAtm.mH.sendMessage(PooledLambda.obtainMessage(
                WindowProcessListenerSmtBase::bringProcessToDefaultLocked, this.listener.getSmtEx()));
    }

    public boolean isPreviousVrProcess() {
        if (this.mAtmsmt == null) {
            this.mAtmsmt = this.mAtm.getSmtEx();
        }
        synchronized (this.mAtm.mGlobalLockWithoutBoost) {
            if (this.mAtmsmt == null) {
                return false;
            }
            return this.host == this.mAtmsmt.mPreviousVrProcess;
        }
    }
}
