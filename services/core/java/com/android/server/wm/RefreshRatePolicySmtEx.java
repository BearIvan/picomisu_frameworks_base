// Copyright 2026 Picomisu contributors
// SPDX-License-Identifier: Apache-2.0

package com.android.server.wm;

import com.android.server.ISmartService;
import com.android.server.SysOptBridge;
import smartisanos.util.FeatLog;

/**
 * Reconstructed from the PICO OS 5.13.7 factory services. Nothing in the factory services,
 * sys-services or sysmonitor-services references it; it is carried for parity.
 *
 * @hide
 */
public class RefreshRatePolicySmtEx {
    public static final int SMT_REFRESH_AUTO_60 = 4;
    public static final int SMT_REFRESH_AUTO_90 = 8;
    public static final int SMT_REFRESH_CLOUD_60 = 1;
    public static final int SMT_REFRESH_CLOUD_90 = 2;
    public static final int SMT_REFRESH_NONE = 0;
    ISmartService mSmartService = SysOptBridge.getFactory().getSmartService();
    public static int sSmtRefreshFlag = 0;
    public static int sOldSmtRefreshFlag = 0;

    RefreshRatePolicySmtEx() {
    }

    public static void updateAppCloudSmtRefreshFlag(int judgementId, int lowRefreshRateId) {
        sSmtRefreshFlag &= -2;
        sSmtRefreshFlag &= -3;
        if (judgementId == lowRefreshRateId) {
            sSmtRefreshFlag |= 1;
        } else if (judgementId == 0) {
            sSmtRefreshFlag |= 2;
        }
        int i = sSmtRefreshFlag;
        if (i != sOldSmtRefreshFlag) {
            sOldSmtRefreshFlag = i;
            FeatLog.i("RefreshRatePolicy", "FEAT_PERF_REFRESH_RATE", 10, "update cloud smtRefreshFlag : " + sSmtRefreshFlag);
        }
    }

    public static void updateAppRequestSmtRefreshFlag(int judgementId, int lowRefreshRateId) {
        sSmtRefreshFlag &= -5;
        sSmtRefreshFlag &= -9;
        if (judgementId == lowRefreshRateId) {
            sSmtRefreshFlag |= 4;
        } else if (judgementId == 0) {
            sSmtRefreshFlag |= 8;
        }
        int i = sSmtRefreshFlag;
        if (i != sOldSmtRefreshFlag) {
            sOldSmtRefreshFlag = i;
            FeatLog.i("RefreshRatePolicy", "FEAT_PERF_REFRESH_RATE", 10, "update app auto smtRefreshFlag : " + sSmtRefreshFlag);
        }
    }

    int judgementAutoDisplayRefreshRate(WindowState w, int lowRefreshRateId) {
        boolean detectSmartRefresh = !this.mSmartService.closeDetectSmartRefresh();
        if (w.mAppToken != null && detectSmartRefresh) {
            int flag = 0;
            if (w.mAppToken.mActivityRecord != null && (flag = w.mAppToken.mActivityRecord.info.getSmtEx().autoDisplayFlags) == 0) {
                flag = w.mAppToken.mActivityRecord.appInfo.getSmtEx().autoDisplayFlags;
            }
            if ((flag & 4) != 0) {
                return lowRefreshRateId;
            }
            if ((flag & 8) != 0) {
                return 0;
            }
            return -1;
        }
        return -1;
    }

    int judgementForceDisplayRefreshRate(WindowState w, int lowRefreshRateId) {
        if (w.mAppToken != null) {
            int flag = 0;
            if (w.mAppToken.mActivityRecord != null && (flag = w.mAppToken.mActivityRecord.info.getSmtEx().forceDisplayFlags) == 0) {
                flag = w.mAppToken.mActivityRecord.appInfo.getSmtEx().forceDisplayFlags;
            }
            if ((flag & 1) != 0) {
                return lowRefreshRateId;
            }
            if ((flag & 2) != 0) {
                return 0;
            }
            return -1;
        }
        return -1;
    }
}
