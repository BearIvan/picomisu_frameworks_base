// Copyright 2026 Picomisu contributors
// SPDX-License-Identifier: Apache-2.0
// Reconstructed from the factory PICO OS 5.13.7 DEX by tools/reconstruct-pico-aidl.py.
package android.content.pm;

/** @hide */
interface IPackageManagerSmtEx {
    void parseAppRefreshRate(String jsonStr);
    void updateAppTypeInfo(String packageName, int flag);
    boolean performDexOptModeSmt(String packageName, boolean checkProfiles, String targetCompilerFilter, boolean force, boolean bootComplete, String splitName, boolean immediately);
    boolean isTaskPersist(String packagename, int userId);
    void clearOverrideFlag(String packageName);
    void updateAppInfo(String packageName, String appInfoJsonConfig, long appLastTime, boolean removeUnityChoreographerVsync, boolean permissionFlag, boolean isDexApp, boolean downloadProfileFlag);
}
