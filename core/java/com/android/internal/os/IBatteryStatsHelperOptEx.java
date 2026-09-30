// Copyright 2026 Picomisu contributors
// SPDX-License-Identifier: Apache-2.0

package com.android.internal.os;

import android.os.BatteryStats;
import android.os.DebugSmtEx;
import android.util.ArrayMap;

import java.util.List;

/**
 * Smartisan battery stats helper extension implemented by the optional sys/sysmonitor framework
 * JARs. Reconstructed from the PICO OS 5.13.7 factory framework, with the factory default
 * implementations.
 *
 * @hide
 */
public interface IBatteryStatsHelperOptEx {
    default void initSystemSipper(BatteryStats.Uid u) {
        DebugSmtEx.printDefaultFunInfo(getClass());
    }

    default void sumSystemPower(BatteryStats.Uid u) {
        DebugSmtEx.printDefaultFunInfo(getClass());
    }

    default void addUsageList(BatteryStats.Uid u, List<BatterySipper> usageList) {
        DebugSmtEx.printDefaultFunInfo(getClass());
    }

    default void splitSystemCpuPowerUsage(BatteryStats.Uid u,
            ArrayMap<String, ? extends BatteryStats.Uid.Proc> processStats, BatterySipper app,
            int statsType) {
        DebugSmtEx.printDefaultFunInfo(getClass());
    }

    default void calCameraPower(BatteryStats.Uid u, BatterySipper app) {
        DebugSmtEx.printDefaultFunInfo(getClass());
    }
}
