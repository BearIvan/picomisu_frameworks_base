// Copyright 2026 Picomisu contributors
// SPDX-License-Identifier: Apache-2.0

package com.android.server.wm;

import android.content.Context;
import android.content.res.Configuration;
import android.hardware.display.DisplayManager;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.util.SmtUidUtil;
import android.view.Display;
import com.android.server.IBoostFrameworkOptEx;
import com.android.server.IMultiPlatSvsFactory;
import com.android.server.SysOptBridge;
import com.android.server.am.ProcessRecord;
import smartisanos.util.FeatLog;

/**
 * Reconstructed from the PICO OS 5.13.7 factory services.
 *
 * @hide
 */
public class ActivityRecordSmtBase extends ConfigurationContainerSmtBase {
    protected static final String TAG = "ActivityRecord";
    public String launchedFromPackage;
    public int launchedFromUid;
    public ActivityRecord mActivityRecord;
    private DisplayManager mDisplayManager;
    boolean mForceRemoveWhenProcessGone;
    private boolean mInPowerPolicy;
    public boolean mIsSmartisanHome;
    protected String mStartFrom;
    public int mSysLaunchType;
    protected final Configuration mTmpConfig;
    private int mToolType;
    private WindowProcessControllerSmtBase mWpcSmt;
    protected IActivityRecordOptEx optEx;
    public boolean pauseTimeout;
    public long pauseTimeoutBegin;
    int smtUid;
    protected static String PACKAGE_LOCK_META_DATA_NAME = "smartisanos.PACKAGELOCK_PACKAGENAME";
    static boolean sToolPerfSwitch = SystemProperties.getBoolean("persist.sys.toolperf.switch", true);
    static boolean sIsPowerPolicyOn = false;
    static IBoostFrameworkOptEx mPerf_powerpolicy = null;

    public ActivityRecordSmtBase(ActivityRecord record) {
        super(record);
        this.smtUid = 1000;
        this.mTmpConfig = new Configuration();
        this.mSysLaunchType = 0;
        this.pauseTimeoutBegin = 0L;
        this.pauseTimeout = false;
        this.mInPowerPolicy = false;
        this.mToolType = 0;
        this.launchedFromUid = 0;
        this.launchedFromPackage = null;
        this.mWpcSmt = null;
        this.mActivityRecord = record;
        this.optEx = SysOptBridge.getFactory().createActivityRecordOptEx(record);
    }

    void init() {
        if ("com.smartisanos.launcher/.Launcher".equals(this.mActivityRecord.shortComponentName)) {
            this.mIsSmartisanHome = true;
        }
    }

    public int getSmtUid() {
        int result = this.mActivityRecord.appInfo.uid;
        if (result == 1000) {
            if (this.smtUid == 1000) {
                this.smtUid = SmtUidUtil.getSystemUidForPackage(this.mActivityRecord.appInfo.packageName);
            }
            return this.smtUid;
        }
        return result;
    }

    public boolean isInPowerPolicy() {
        return this.mInPowerPolicy;
    }

    public void reportPauseTimeoutEventIfNeeded(ActivityTaskManagerService atms) {
        Display dp;
        if (this.pauseTimeout && this.pauseTimeoutBegin != 0 && atms.mContext != null && (dp = getDisplay(this.mActivityRecord.getDisplayId(), atms.mContext)) != null && dp.getType() == 1 && this.mActivityRecord.mActivityComponent != null) {
            long duration = SystemClock.elapsedRealtime() - this.pauseTimeoutBegin;
            SysOptBridge.getFactory().getPauseTimeoutDataUpload().updatePauseTimeoutEvent(this.mActivityRecord.mActivityComponent.getPackageName(), this.mActivityRecord.mActivityComponent.getClassName(), duration);
        }
        this.pauseTimeout = false;
        this.pauseTimeoutBegin = 0L;
    }

    public Display getDisplay(int displayId, Context context) {
        if (this.mDisplayManager == null) {
            this.mDisplayManager = (DisplayManager) context.getSystemService("display");
        }
        DisplayManager displayManager = this.mDisplayManager;
        if (displayManager != null) {
            return displayManager.getDisplay(displayId);
        }
        return null;
    }

    public void setLaunchSource(int sourceUid, String sourcePackageName) {
        this.launchedFromUid = sourceUid;
        this.launchedFromPackage = sourcePackageName;
    }

    public WindowProcessControllerSmtBase getProcessControllerSmt() {
        WindowProcessController app;
        WindowProcessControllerSmtBase windowProcessControllerSmtBase = this.mWpcSmt;
        if (windowProcessControllerSmtBase != null) {
            return windowProcessControllerSmtBase;
        }
        ActivityRecord activityRecord = this.mActivityRecord;
        if (activityRecord != null && (app = activityRecord.app) != null) {
            this.mWpcSmt = app.getWPCSmtEx();
        }
        return this.mWpcSmt;
    }

    public void stopPkgIfMarked() {
        ActivityRecord activityRecord = this.mActivityRecord;
        if (activityRecord != null && activityRecord.info != null && this.mActivityRecord.info.applicationInfo != null) {
            SysOptBridge.getFactory().getSingle3DApp().stopMarkedPkg(this.mActivityRecord.packageName, this.mActivityRecord.getUid(), this.mActivityRecord);
        }
    }

    public boolean isRecordStateAfterStop() {
        return this.mActivityRecord.mState == ActivityStack.ActivityState.STOPPED || this.mActivityRecord.mState == ActivityStack.ActivityState.FINISHING || this.mActivityRecord.mState == ActivityStack.ActivityState.DESTROYING || this.mActivityRecord.mState == ActivityStack.ActivityState.DESTROYED;
    }

    public void setInPowerPolicy(boolean inPowerPolicy) {
        this.mInPowerPolicy = inPowerPolicy;
    }

    public int getToolType() {
        return this.mToolType;
    }

    public void setToolType(int toolType) {
        this.mToolType = toolType;
    }

    void adjustBenchmarkPolicy(int curPid, int[] antutuPerfParams) {
        IMultiPlatSvsFactory factory;
        if (sToolPerfSwitch) {
            try {
                if (!sIsPowerPolicyOn && isInPowerPolicy()) {
                    int tooltype = getToolType();
                    SysOptBridge.getFactory().getSmartService().transact(50, tooltype, curPid);
                    sIsPowerPolicyOn = true;
                    if (mPerf_powerpolicy == null && (factory = SysOptBridge.getMultiPlatFactory()) != null) {
                        mPerf_powerpolicy = factory.getBoostFrameworkByPerf();
                    }
                    if (mPerf_powerpolicy != null) {
                        mPerf_powerpolicy.perfPerformanceMode(true);
                        mPerf_powerpolicy.perfLockAcquire(0, antutuPerfParams);
                        return;
                    }
                    return;
                }
                if (sIsPowerPolicyOn && !isInPowerPolicy()) {
                    sIsPowerPolicyOn = false;
                    SysOptBridge.getFactory().getSmartService().transact(51, 0);
                    if (mPerf_powerpolicy != null) {
                        mPerf_powerpolicy.perfPerformanceMode(false);
                        mPerf_powerpolicy.perfLockRelease();
                    }
                }
            } catch (Exception e) {
                FeatLog.e(TAG, "FEAT_BENCHMARK_IMPROVE", 0, "adjustBenchmarkPolicy exception", e);
            }
        }
    }

    public boolean isPrefetchApp() {
        if (this.mActivityRecord.app != null) {
            return ((ProcessRecord) this.mActivityRecord.app.mOwner).getSmtEx().isPrefetch;
        }
        return false;
    }
}
