// Copyright 2026 Picomisu contributors
// SPDX-License-Identifier: Apache-2.0

package com.android.server;

/**
 * Smartisan performance boost framework implemented by the optional multi-platform services
 * ({@link IMultiPlatSvsFactory#getBoostFramework()}). Reconstructed from the PICO OS 5.13.7
 * factory services.
 *
 * @hide
 */
public interface IBoostFrameworkOptEx {
    void bindCoreAcquire(int pid);

    void bindCoreRelease(int pid);

    int configBoostParams(int param);

    int configBoostParams(int param0, int param1, int param2, int param3);

    void disableBoost();

    void enableBoost(int duration);

    void enableBoost(int duration, int type);

    void initBoost();

    void perfDozeMode(int mode);

    void perfEnableACL(boolean enable);

    void perfEntryPCMode(boolean enter);

    void perfInCallMode(boolean inCall);

    void perfLockAcquire(int duration, int... list);

    void perfLockRelease();

    void perfLockUseProfile(int profile);

    void perfPerformanceMode(boolean enable);
}
