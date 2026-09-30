// Copyright 2026 Picomisu contributors
// SPDX-License-Identifier: Apache-2.0
// Reconstructed from the factory PICO OS 5.13.7 DEX by tools/reconstruct-pico-aidl.py.
package android.app;

import android.app.IDataListener;
import android.app.SceneData;

/** @hide */
interface ISceneInfoManager {
    boolean reportData(int listenerType, int event);
    boolean reportDataInfo(int listenerType, int event, in SceneData data);
    boolean registerListener(int listenerType, IDataListener listener, String clsName);
    boolean unRegisterListenerType(int listenerType, IDataListener listener);
    boolean unRegisterListener(IDataListener listener);
    SceneData getTypeLastReport(int listenerType);
}
