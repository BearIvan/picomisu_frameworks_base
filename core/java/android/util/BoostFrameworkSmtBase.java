// Copyright 2026 Picomisu contributors
// SPDX-License-Identifier: Apache-2.0

package android.util;

import java.lang.reflect.Method;

/**
 * Smartisan extension of {@link BoostFramework}: the DPerformance ({@code
 * com.delta.util.DPerformance}) perf lock profiles and PC/doze/in-call modes. Reconstructed
 * from the PICO OS 5.13.7 factory framework.
 *
 * @hide
 */
public class BoostFrameworkSmtBase {
    private static final String TAG = "BoostFramework";
    private static final String DPERFORMANCE_JAR = "/system/framework/DPerformance.jar";
    private static final String DPERFORMANCE_CLASS = "com.delta.util.DPerformance";

    static Method mUseDProfile = null;
    static Method mEntryPCMode = null;
    static Method mDozeModeFunc = null;
    static Method mEnableACL = null;
    static Method mInCallMode = null;
    Object mDPerf = null;
    private static Class<?> sDPerfClass = null;

    private BoostFramework mBoostFrameworkSmtBase;

    public BoostFrameworkSmtBase(BoostFramework BoostFrameworkSmtBase) {
        mBoostFrameworkSmtBase = BoostFrameworkSmtBase;
    }

    public void initFunctionsEx() {
        try {
            sDPerfClass = Class.forName(DPERFORMANCE_CLASS);
            Class[] argClasses = new Class[] {int.class};
            mDozeModeFunc = sDPerfClass.getMethod("perfDozeMode", argClasses);

            argClasses = new Class[] {int.class};
            mUseDProfile = sDPerfClass.getMethod("perfLockUseDProfile", argClasses);

            argClasses = new Class[] {boolean.class};
            mEntryPCMode = sDPerfClass.getDeclaredMethod("perfEntryPCMode", argClasses);

            argClasses = new Class[] {boolean.class};
            mEnableACL = sDPerfClass.getMethod("perfEnableACL", argClasses);
            argClasses = new Class[] {boolean.class};
            mInCallMode = sDPerfClass.getMethod("perfInCallMode", argClasses);
        } catch (Exception e) {
            Log.e(TAG, "BoostFramework() : DPerf_Function = ", e);
        }
    }

    public void initEx() {
        try {
            if (sDPerfClass != null) {
                mDPerf = sDPerfClass.newInstance();
            }
        } catch (Exception e) {
            Log.e(TAG, "BoostFramework() : Exception_2 = ", e);
        }
    }

    public int perfLockUseProfile(int level) {
        int ret = -1;

        if (level > 10 || level < 0) {
            return ret;
        }

        try {
            Object retVal = mUseDProfile.invoke(mDPerf, level);
            ret = (int) retVal;
        } catch (Exception e) {
            Log.e(TAG, "Exception ", e);
        }
        return ret;
    }

    public int perfEntryPCMode(boolean PCMode) {
        int ret = -1;
        try {
            Object retVal = mEntryPCMode.invoke(mDPerf, PCMode);
            ret = (int) retVal;
        } catch (Exception e) {
            Log.e(TAG, "Exception ", e);
        }
        return ret;
    }

    public int perfDozeMode(int level) {
        int ret = -1;

        try {
            Object retVal = mDozeModeFunc.invoke(mDPerf, level);
            ret = (int) retVal;
        } catch (Exception e) {
            Log.e(TAG, "Exception ", e);
        }
        return ret;
    }

    public int perfEnableACL(boolean enable) {
        int ret = -1;

        try {
            Object retVal = mEnableACL.invoke(mDPerf, enable);
            ret = (int) retVal;
        } catch (Exception e) {
            Log.e(TAG, "Exception ", e);
        }
        return ret;
    }

    public int perfInCallMode(boolean callMode) {
        int ret = -1;

        try {
            Object retVal = mInCallMode.invoke(mDPerf, callMode);
            ret = (int) retVal;
        } catch (Exception e) {
            Log.e(TAG, "Exception ", e);
        }
        return ret;
    }

    public class LaunchSmtEx {
        public static final int BOOST_V4 = 7;
        public static final int BOOST_V5 = 8;
        public static final int TYPE_ATTACH_PRIMCORE = 104;
    }

    public class AnimationSmtEx {
        public static final int APPTRANSITION = 1;
        public static final int VIEWSWITCH = 2;
        public static final int SCREENROTATE = 3;
    }
}
