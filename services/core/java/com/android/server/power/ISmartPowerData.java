// Copyright 2026 Picomisu contributors
// SPDX-License-Identifier: Apache-2.0

package com.android.server.power;

import android.content.Context;
import android.os.DebugSmtEx;
import android.os.WorkSource;
import java.util.ArrayList;

/**
 * Reconstructed from the PICO OS 5.13.7 factory services.
 *
 * @hide
 */
public interface ISmartPowerData {
    default void exitPCMode() {
        DebugSmtEx.printDefaultFunInfo(getClass());
    }

    default void enterPCMode() {
        DebugSmtEx.printDefaultFunInfo(getClass());
    }

    default void registerPushReceiver(Context context) {
        DebugSmtEx.printDefaultFunInfo(getClass());
    }

    default void handlePowerScenesData() {
        DebugSmtEx.printDefaultFunInfo(getClass());
    }

    default void systemReady(Context context) {
        DebugSmtEx.printDefaultFunInfo(getClass());
    }

    default void setScreenBrightness(int brightness) {
        DebugSmtEx.printDefaultFunInfo(getClass());
    }

    default String getSubInfo() {
        DebugSmtEx.printDefaultFunInfo(getClass());
        return "";
    }

    default void cancelCollectPowerLogLocked(String reason) {
        DebugSmtEx.printDefaultFunInfo(getClass());
    }

    default ArrayList<String> getGpuInfo() {
        DebugSmtEx.printDefaultFunInfo(getClass());
        return null;
    }

    default void setLightsState(int id, int color) {
        DebugSmtEx.printDefaultFunInfo(getClass());
    }

    default void setFlashlightState(boolean isOn) {
        DebugSmtEx.printDefaultFunInfo(getClass());
    }

    default void noteStartGps(int uid) {
        DebugSmtEx.printDefaultFunInfo(getClass());
    }

    default void noteStopGps(int uid) {
        DebugSmtEx.printDefaultFunInfo(getClass());
    }

    default void appFrontEvent(int eventType, String packageName, int userId) {
        DebugSmtEx.printDefaultFunInfo(getClass());
    }

    default void noteStateChanged(String name, int state, int uid) {
        DebugSmtEx.printDefaultFunInfo(getClass());
    }

    default void noteSensorStateChange(int uid, int sensor, boolean isStart) {
        DebugSmtEx.printDefaultFunInfo(getClass());
    }

    default void noteSyncStateChange(String name, int uid, boolean isStart) {
        DebugSmtEx.printDefaultFunInfo(getClass());
    }

    default void noteJobStateChange(String name, int uid, boolean isStart) {
        DebugSmtEx.printDefaultFunInfo(getClass());
    }

    default void noteFullWifiLockStateChange(int uid, boolean isStart) {
        DebugSmtEx.printDefaultFunInfo(getClass());
    }

    default void noteWifiScanStateChange(int uid, boolean isStart) {
        DebugSmtEx.printDefaultFunInfo(getClass());
    }

    default void noteWifiScanStateChange(WorkSource ws, boolean isStart) {
        DebugSmtEx.printDefaultFunInfo(getClass());
    }

    default void noteWifiRssiChangedLocked(int newRssi) {
        DebugSmtEx.printDefaultFunInfo(getClass());
    }

    default void noteBleScanStarted(WorkSource ws, boolean isUnoptimized) {
        DebugSmtEx.printDefaultFunInfo(getClass());
    }

    default void noteBleScanStopped(WorkSource ws, boolean isUnoptimized) {
        DebugSmtEx.printDefaultFunInfo(getClass());
    }

    default void noteResetBleScan() {
        DebugSmtEx.printDefaultFunInfo(getClass());
    }

    default void noteWakeupReason(String reason) {
        DebugSmtEx.printDefaultFunInfo(getClass());
    }

    default void noteWakupAlarm(String name, int uid, WorkSource workSource, String tag) {
        DebugSmtEx.printDefaultFunInfo(getClass());
    }

    default void noteLongPartialWakelock(String name, String historyName, int uid) {
        DebugSmtEx.printDefaultFunInfo(getClass());
    }

    default void noteLongPartialWakelockStartFromSource(String name, String historyName, WorkSource workSource) {
        DebugSmtEx.printDefaultFunInfo(getClass());
    }

    default void noteLongPartialWakelockFinish(String name, String historyName, int uid) {
        DebugSmtEx.printDefaultFunInfo(getClass());
    }

    default void noteLongPartialWakelockFinishFromSource(String name, String historyName, WorkSource workSource) {
        DebugSmtEx.printDefaultFunInfo(getClass());
    }

    default void updatePowerCloseWifiEnableLocked(boolean isEnable) {
        DebugSmtEx.printDefaultFunInfo(getClass());
    }

    default void startScreenOffScene() {
        DebugSmtEx.printDefaultFunInfo(getClass());
    }

    default void enterQbShutdown() {
        DebugSmtEx.printDefaultFunInfo(getClass());
    }

    default void exitQbShutdown(int quitReason, boolean isShutdown) {
        DebugSmtEx.printDefaultFunInfo(getClass());
    }

    default void enterQuickBoot(long bootDuration) {
        DebugSmtEx.printDefaultFunInfo(getClass());
    }

    default void addPowerKillInfo(String pkg, int killType, int isKill, int score, String reason) {
        DebugSmtEx.printDefaultFunInfo(getClass());
    }

    default void notePowerSceneState(String pkgName, String mainScene, String subScene, int sceneState, String payload) {
        DebugSmtEx.printDefaultFunInfo(getClass());
    }

    default void noteVstSceneState(int state) {
        DebugSmtEx.printDefaultFunInfo(getClass());
    }

    default void updateWifiState(boolean enable) {
        DebugSmtEx.printDefaultFunInfo(getClass());
    }

    default void noteSystemState(String params) {
        DebugSmtEx.printDefaultFunInfo(getClass());
    }

    default void noteVideoInfo(String data) {
        DebugSmtEx.printDefaultFunInfo(getClass());
    }
}
