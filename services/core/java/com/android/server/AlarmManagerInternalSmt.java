// Copyright 2026 Picomisu contributors
// SPDX-License-Identifier: Apache-2.0

package com.android.server;

/**
 * Reconstructed from the PICO OS 5.13.7 factory services.
 *
 * @hide
 */
public abstract class AlarmManagerInternalSmt {

    public interface IAlarmHeartBeatListener {
        void onAlarmHeartBeat(long j);
    }

    public abstract void registerHeartBeatListener(IAlarmHeartBeatListener iAlarmHeartBeatListener);

    public abstract void unregisterHeartBeatListener(IAlarmHeartBeatListener iAlarmHeartBeatListener);
}
