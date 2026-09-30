// Copyright 2026 Picomisu contributors
// SPDX-License-Identifier: Apache-2.0
// Reconstructed from the factory PICO OS 5.13.7 DEX by tools/reconstruct-pico-aidl.py.
package com.pvr;

import android.os.Bundle;
import android.os.IBinder;
import com.pvr.IPvrCallback;
import com.pvr.IPvrCallbackNative;

/** @hide */
interface IPvrManagerService {
    boolean setSystemFeatures(int featureId, String featureValue, IBinder token);
    boolean addSystemService(String name, IBinder binder, IBinder token);
    String getSystemFeatures(int featureId, IBinder token);
    void addPvrCallback(String className, IPvrCallback pcb, int type);
    void removePvrCallback(String className);
    void sendPvrMessage(String event, String value, int type);
    void updateUserSettings(String countryCode, boolean saveToConfigFileUnderPersistPartition);
    int[] getScreenBrightnessLevel();
    void setCurrentScreenBrightnessLevel(int vrBrightness, int setlevel);
    int setLedStatus(int led, int color, int blink, int ontime, int offtime);
    int getHeadstrapStatus();
    void addPvrCallbacks(IPvrCallback pcb);
    void removePvrCallbacks(int pid);
    oneway void sendPvrMessages(String event, String value);
    void updateDistributionChannel(String channel, boolean manuallyModified);
    void addPvrCallbacksNative(IPvrCallbackNative pcbn);
    void setClientStatus(int status);
    oneway void sendEvent(String event, in Bundle bundle);
    void addPvrCallbacksWithEventType(IPvrCallback pcb, in String[] eventType);
    void addPvrCallbacksNativeWithEventType(IPvrCallbackNative pcbn, in String[] eventType);
}
