// Copyright 2026 Picomisu contributors
// SPDX-License-Identifier: Apache-2.0
// Reconstructed from the factory PICO OS 5.13.7 DEX by tools/reconstruct-pico-aidl.py.
package android.app;

import android.content.IIntentReceiver;

/** @hide */
interface ISysClient {
    oneway void collectBroadcastInfo(IIntentReceiver receiver, String name, String infoName, int collectTimes, long beginTime);
    oneway void collectServiceInfo(String name, int collectTimes, long beginTime, long timeout);
    oneway void collectInputInfo(int seq, int collectTimes, long beginTime);
    oneway void findCurrentMainMsgs(long current, long trackingTime, int uid, long endWallTime);
}
