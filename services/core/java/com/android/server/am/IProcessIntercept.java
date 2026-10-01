// Copyright 2026 Picomisu contributors
// SPDX-License-Identifier: Apache-2.0

package com.android.server.am;

import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.ResolveInfo;
import android.os.DebugSmtEx;
import java.util.HashSet;

/**
 * Reconstructed from the PICO OS 5.13.7 factory services.
 *
 * @hide
 */
public interface IProcessIntercept {
    public static final int SHOW_BROADCAST_INTERCEPT_LOG = 3003;
    public static final int SHOW_INTERCEPT_LOG = 3000;
    public static final int SHOW_PROVIDER_INTERCEPT_LOG = 3001;
    public static final int SHOW_SERVICES_INTERCEPT_LOG = 3002;
    public static final String VPN_SERVICE_OFF = "vpn_service_off";

    default boolean isProviderAllowStart(ProcessRecord caller, ContentProviderRecord cpr) {
        DebugSmtEx.printDefaultFunInfo(getClass());
        return true;
    }

    default HashSet<String> getPushServiceNames() {
        DebugSmtEx.printDefaultFunInfo(getClass());
        return null;
    }

    default HashSet<String> getPushServiceActions() {
        DebugSmtEx.printDefaultFunInfo(getClass());
        return null;
    }

    default HashSet<String> getPushProviderNames() {
        DebugSmtEx.printDefaultFunInfo(getClass());
        return null;
    }

    default boolean isActivityAllowStart(ActivityInfo activityInfo, String processName, int uid) {
        DebugSmtEx.printDefaultFunInfo(getClass());
        return true;
    }

    default boolean isActivityAllowStart(String callingPackage, int callingUid, ActivityInfo activityInfo, Intent intent) {
        DebugSmtEx.printDefaultFunInfo(getClass());
        return true;
    }

    default boolean isBroadcastAllowStart(ResolveInfo receiver, BroadcastRecord r) {
        DebugSmtEx.printDefaultFunInfo(getClass());
        return true;
    }

    default boolean isServiceAllowStart(ProcessRecord caller, ServiceRecord target, int callingUid) {
        DebugSmtEx.printDefaultFunInfo(getClass());
        return true;
    }

    default boolean isAllowStartInstrumentation(String packageName, String className, int callingUid) {
        DebugSmtEx.printDefaultFunInfo(getClass());
        return true;
    }

    default boolean isShowInterceptLog() {
        DebugSmtEx.printDefaultFunInfo(getClass());
        return true;
    }

    default void switchInterceptLog() {
        DebugSmtEx.printDefaultFunInfo(getClass());
    }

    default void switchProviderInterceptLog() {
        DebugSmtEx.printDefaultFunInfo(getClass());
    }

    default void switchServicesInterceptLog() {
        DebugSmtEx.printDefaultFunInfo(getClass());
    }

    default void switchBroadcastInterceptLog() {
        DebugSmtEx.printDefaultFunInfo(getClass());
    }

    default void updatePackagesKilledTimeByForceStop(String packageName) {
        DebugSmtEx.printDefaultFunInfo(getClass());
    }

    default void setDeviceOwnerUid(int uid) {
        DebugSmtEx.printDefaultFunInfo(getClass());
    }
}
