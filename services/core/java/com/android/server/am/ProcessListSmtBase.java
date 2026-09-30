// Copyright 2026 Picomisu contributors
// SPDX-License-Identifier: Apache-2.0

package com.android.server.am;

import android.content.pm.ApplicationInfo;
import android.os.RemoteException;

import com.android.internal.app.ProcessMap;
import com.android.server.SysOptBridge;

import smartisanos.os.PeroptWhiteListParser;
import smartisanos.util.FeatLog;

/**
 * Smartisan extension of the {@link ProcessList} (its {@code mSmtEx}): the prefetched
 * (pre-started) application processes and the Smartisan app type white list. Reconstructed from
 * the PICO OS 5.13.7 factory services.
 *
 * @hide
 */
public class ProcessListSmtBase {
    private static final String TAG = "ProcessListSmtBase";

    static final int SCHED_GROUP_BG_3RD_APP = -10;
    static final int SCHED_GROUP_PREFETCH_VR_APP = -15;
    static final int SCHED_GROUP_CLUSTER_BIG = 8;
    static final int SCHED_GROUP_CLUSTER_SUPER = 9;
    static final int SCHED_GROUP_COMPOSITOR = 12;

    PeroptWhiteListParser mPeroptWhiteListParser;
    final MyPrefetchProcessMap mPrefetchProcess = new MyPrefetchProcessMap();
    protected ProcessList mProcessList;

    public ProcessListSmtBase(ProcessList processList) {
        mProcessList = processList;
    }

    final class MyPrefetchProcessMap extends ProcessMap<ProcessRecord> {
        @Override
        public ProcessRecord put(String name, int uid, ProcessRecord value) {
            final ProcessRecord r = super.put(name, uid, value);
            mProcessList.mService.mAtmInternal.getSmtEx().onPrefetchProcessAdded(
                    r.getWindowProcessController());
            return r;
        }

        @Override
        public ProcessRecord remove(String name, int uid) {
            final ProcessRecord r = super.remove(name, uid);
            mProcessList.mService.mAtmInternal.getSmtEx().onPrefetchProcessRemoved(name, uid);
            return r;
        }
    }

    public int getPrefetchSize() {
        return mPrefetchProcess.size();
    }

    ProcessRecord findPrefetchProcess(String processName, ApplicationInfo info) {
        ProcessRecord prefetchProcess = mPrefetchProcess.get(processName, info.uid);
        if (prefetchProcess != null) {
            if (info.getSmtEx().doPrefetch == 1) {
                return prefetchProcess;
            }
            boolean goodPrefetch = false;
            if (prefetchProcess.thread != null && !prefetchProcess.killed) {
                prefetchProcess.info.getSmtEx().doPrefetch = 3;
                prefetchProcess.verifiedAdj = ProcessList.INVALID_ADJ;
                prefetchProcess.curAdj = ProcessList.INVALID_ADJ;
                prefetchProcess.setAdj = ProcessList.INVALID_ADJ;
                ProcessList.setOomAdj(prefetchProcess.pid, prefetchProcess.uid,
                        prefetchProcess.setAdj);
                mProcessList.mProcessNames.put(processName, info.uid, prefetchProcess);
                mPrefetchProcess.remove(processName, info.uid);
                synchronized (mProcessList.mService.mPidsSelfLocked) {
                    mProcessList.mService.mPidsSelfLocked.put(prefetchProcess);
                    mProcessList.mService.getSmtEx().mPrefetchPidsSelf.remove(prefetchProcess);
                    mProcessList.mService.getSmtEx().removePrefetchApp(prefetchProcess.pid);
                }
                try {
                    prefetchProcess.thread.completePrefetchBindApplication(
                            prefetchProcess.startSeq);
                } catch (RemoteException e) {
                    FeatLog.e(TAG, "FEAT_PERF_PREFETCH", 50,
                            "system_server: complete prefetch bind application error!", e);
                    mProcessList.mProcessNames.remove(processName, info.uid);
                    synchronized (mProcessList.mService.mPidsSelfLocked) {
                        mProcessList.mService.mPidsSelfLocked.remove(prefetchProcess);
                        SysMonitorSvcBridge.getFactory().getSysPerfMonitorService().removePid(
                                prefetchProcess.pid);
                    }
                    SysOptBridge.getFactory().getSysPrefetchService().removeAlivePrefetch(
                            processName);
                    prefetchProcess = null;
                }
                if (prefetchProcess != null) {
                    FeatLog.d(TAG, "FEAT_PERF_PREFETCH", 50,
                            "system_server: prefetch real start process = "
                                    + prefetchProcess.processName
                                    + ", move to real process map");
                    goodPrefetch = true;
                }
            } else if (prefetchProcess.thread == null) {
                mProcessList.mProcessNames.put(processName, info.uid, prefetchProcess);
                mPrefetchProcess.remove(processName, info.uid);
                synchronized (mProcessList.mService.mPidsSelfLocked) {
                    mProcessList.mService.mPidsSelfLocked.put(prefetchProcess);
                    mProcessList.mService.getSmtEx().mPrefetchPidsSelf.remove(prefetchProcess);
                    mProcessList.mService.getSmtEx().removePrefetchApp(prefetchProcess.pid);
                }
                prefetchProcess.info.getSmtEx().doPrefetch = 0;
                goodPrefetch = true;
            }
            if (goodPrefetch) {
                prefetchProcess.addPackage(info.packageName, info.versionCode,
                        mProcessList.mService.mProcessStats);
                return prefetchProcess;
            }
        }
        return null;
    }

    void registerPeroptWhiteListReceiver() {
        mPeroptWhiteListParser = PeroptWhiteListParser.getInstance();
        mPeroptWhiteListParser.setContext(mProcessList.mService.mContext);
        mPeroptWhiteListParser.registerPackageBroadCast();
    }

    void updateAppTypesMapFromCallback(String packageName, String appTypes) {
        mPeroptWhiteListParser.updateAppTypesMapFromCallback(packageName, appTypes);
    }

    void updateAppTypeMayFromExpandService(ProcessRecord app) {
        if ((app.info.getSmtEx().appTypeFlag == 0 || app.getMonitorEx().isolatedOf3rdPartApp)
                && mPeroptWhiteListParser != null) {
            mPeroptWhiteListParser.updateAppTypeMayFromExpandService(app.info);
        }
    }
}
