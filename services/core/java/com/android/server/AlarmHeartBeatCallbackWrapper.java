// Copyright 2026 Picomisu contributors
// SPDX-License-Identifier: Apache-2.0

package com.android.server;

import java.util.ArrayList;

/**
 * Reconstructed from the PICO OS 5.13.7 factory services.
 *
 * @hide
 */
public class AlarmHeartBeatCallbackWrapper {
    private static final String TAG = "AlarmHeartBeatCallbackWrapper";
    private ArrayList<AlarmManagerInternalSmt.IAlarmHeartBeatListener> mHeartBeatListeners = new ArrayList<>();

    public void registerHeartBeatListener(AlarmManagerInternalSmt.IAlarmHeartBeatListener alarmHeartBeatListener) {
        if (alarmHeartBeatListener == null) {
            throw new IllegalArgumentException("alarmHeartBeatListener must not be null");
        }
        synchronized (this.mHeartBeatListeners) {
            this.mHeartBeatListeners.add(alarmHeartBeatListener);
        }
    }

    public void unregisterHeartBeatListener(AlarmManagerInternalSmt.IAlarmHeartBeatListener alarmHeartBeatListener) {
        if (alarmHeartBeatListener == null) {
            throw new IllegalArgumentException("alarmHeartBeatListener must not be null");
        }
        synchronized (this.mHeartBeatListeners) {
            this.mHeartBeatListeners.remove(alarmHeartBeatListener);
        }
    }

    public void notifyOnHeartBeat(long heartbeatTime) {
        synchronized (this.mHeartBeatListeners) {
            int size = this.mHeartBeatListeners.size();
            if (size > 0) {
                for (int i = 0; i < size; i++) {
                    this.mHeartBeatListeners.get(i).onAlarmHeartBeat(heartbeatTime);
                }
            }
        }
    }
}
