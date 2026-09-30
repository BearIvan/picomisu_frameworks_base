// Copyright 2026 Picomisu contributors
// SPDX-License-Identifier: Apache-2.0
// Reconstructed from the factory PICO OS 5.13.7 DEX by tools/reconstruct-pico-aidl.py.
package android.os;

/** @hide */
interface IPowerAdvisor {
    void updateAppLaunchPrediction(in String[] packages, in int[] whenLaunches, in float[] probabilities);
    void updateDefaultWhitelist(in String[] whitelists);
    String[] getDefaultWhitelist();
    void userSelectedWhitelistApp(String packageName);
    int getLongTimeNoResumeDay();
    void notifyLongTimeNoResumeApp(in String[] packages);
    boolean inPowerCheckBlacklist(String packageName, int uid);
    void updateSleepPrediction(int whichHours, float probability);
    boolean isSleepModeSupport();
    void notifyLockFreqState(boolean enable);
    void enterEnduranceMode(in Map params);
    oneway void notePowerSceneState(String pkgName, String mainScene, String subScene, int sceneState, String payload);
    boolean enableWifi(String pkgName, int timeout);
    void disableWifi(String pkgName);
    oneway void noteSystemState(String pkgName, String event, String params);
}
