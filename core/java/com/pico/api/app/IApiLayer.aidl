// Copyright 2026 Picomisu contributors
// SPDX-License-Identifier: Apache-2.0
// Reconstructed from the factory PICO OS 5.13.7 DEX by tools/reconstruct-pico-aidl.py.
package com.pico.api.app;

import android.content.ComponentName;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteCallback;

/** @hide */
interface IApiLayer {
    const int API_VERSION = 3;
    boolean getScreenState();
    boolean isInputMethodShowing();
    int getSeethroughState();
    String getShowing3dApp();
    int getXrRuntimeDisplayState();
    String getRunning2dAppData();
    ComponentName getTopAppOnDefaultDisplay();
    oneway void updatePersistentServiceConnection(in ComponentName componentName, boolean connect);
    oneway void registerAppSession(IBinder session, int flags);
    oneway void unregisterAppSession(IBinder session, int flags);
    oneway void updateSeethroughState(int state);
    oneway void updateRunning2dAppList(String runningAppData);
    oneway void updateShowing3dApp(String pkg);
    oneway void updateXrRuntimeState(in Bundle state);
    String getTopAppOnDefaultDisplayForNative();
    int getPowerState();
    int getSettingsInt(int type, String key, int def);
    void putSettingsInt(int type, String key, int value);
    String getSettingsString(int type, String key);
    void putSettingsString(int type, String key, String value);
    oneway void registerSettingsObserverForNative(IBinder session, int settingType, String name, int valueType);
    oneway void unregisterSettingsObserverForNative(IBinder session, int settingType, String name);
    oneway void startActivityForNative(String action, String category, String pkg, String cmp, int flags);
    oneway void startServiceForNative(String action, String category, String pkg, String cmp, int flags);
    oneway void registerBroadcastReceiverForNative(IBinder session, String action, String dataScheme);
    oneway void unregisterBroadcastReceiverForNative(IBinder session, String action);
    oneway void requestShowAnrDialog(String pkg, in RemoteCallback callback);
}
