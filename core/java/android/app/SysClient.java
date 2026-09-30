// Copyright 2026 Picomisu contributors
// SPDX-License-Identifier: Apache-2.0

package android.app;

import android.content.IIntentReceiver;

/**
 * Per-process {@link ISysClient} binder that the Smartisan ANR monitor of system_server calls
 * to collect the state of this process; the work is done by the {@link IAnrLogger} of the
 * optional sysmonitor framework JAR ({@link SysMonitorFwBridge}). Reconstructed from the
 * PICO OS 5.13.7 factory framework.
 *
 * @hide
 */
public class SysClient extends ISysClient.Stub {
    private static class INSTANCE {
        private static SysClient mInstance = new SysClient();
    }

    public static SysClient getInstance() {
        return INSTANCE.mInstance;
    }

    @Override
    public void collectBroadcastInfo(IIntentReceiver receiver, String name, String infoName,
            int collectTimes, long beginTime) {
        SysMonitorFwBridge.getFactory().getAnrLogger().collectBroadcastInfo(receiver, name,
                infoName, collectTimes, beginTime);
    }

    @Override
    public void collectServiceInfo(String name, int collectTimes, long beginTime, long timeout) {
        SysMonitorFwBridge.getFactory().getAnrLogger().collectServiceInfo(name, collectTimes,
                beginTime, timeout);
    }

    @Override
    public void collectInputInfo(int seq, int collectTimes, long beginTime) {
        SysMonitorFwBridge.getFactory().getAnrLogger().collectInputInfo(seq, collectTimes,
                beginTime);
    }

    @Override
    public void findCurrentMainMsgs(long current, long trackingTime, int uid, long endWallTime) {
        SysMonitorFwBridge.getFactory().getAnrLogger().findCurrentMainMsgs(current, trackingTime,
                uid, endWallTime);
    }
}
