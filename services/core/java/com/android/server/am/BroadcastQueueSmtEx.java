// Copyright 2026 Picomisu contributors
// SPDX-License-Identifier: Apache-2.0

package com.android.server.am;

import android.util.Slog;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;

/**
 * Reconstructed from the PICO OS 5.13.7 factory services. Nothing in the factory services,
 * sys-services or sysmonitor-services references it; it is carried for parity.
 *
 * @hide
 */
public class BroadcastQueueSmtEx {
    private static final String TAG = "BroadcastQueue";
    private static final String TAG_BROADCAST_STATISTICS = "BroadcastStatistics";
    private BroadcastQueue mQueue;
    private HashMap<String, SenderStats> mAllBroadcastStats = new HashMap<>(16);
    final StringBuilder mStringBuilder = new StringBuilder(256);

    public BroadcastQueueSmtEx(BroadcastQueue queue) {
        this.mQueue = queue;
    }

    void clearStats() {
        this.mAllBroadcastStats.clear();
    }

    void outputBDReceiverStatistics() {
        if (this.mAllBroadcastStats.size() == 0) {
            return;
        }
        ArrayList<SenderStats> broadcastList = new ArrayList<>(this.mAllBroadcastStats.values());
        StringBuilder buf = this.mStringBuilder;
        buf.setLength(0);
        buf.append(this.mQueue.mQueueName);
        buf.append(" Broadcast Receiver Statistics :\n");
        Slog.d(TAG_BROADCAST_STATISTICS, buf.toString());
        int size = broadcastList.size();
        for (int i = 0; i < size; i++) {
            broadcastList.get(i).toString();
        }
    }

    private class SenderStats {
        HashMap<String, BroadcastStats> actions = new HashMap<>(10);
        int count;
        String sender;

        SenderStats(String sender, int count) {
            this.sender = sender;
            this.count = count;
        }

        public String toString() {
            Slog.d(BroadcastQueueSmtEx.TAG_BROADCAST_STATISTICS, String.format(Locale.CHINA, "%s:%d", this.sender, Integer.valueOf(this.count)));
            for (BroadcastStats action : this.actions.values()) {
                action.toString();
            }
            return String.format(Locale.CHINA, "%s:%d", this.sender, Integer.valueOf(this.count));
        }
    }
}
