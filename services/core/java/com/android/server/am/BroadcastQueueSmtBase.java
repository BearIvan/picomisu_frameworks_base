// Copyright 2026 Picomisu contributors
// SPDX-License-Identifier: Apache-2.0

package com.android.server.am;

import android.content.pm.ResolveInfo;
import android.os.SystemClock;
import android.util.Slog;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;

/**
 * Reconstructed from the PICO OS 5.13.7 factory services.
 *
 * @hide
 */
public class BroadcastQueueSmtBase {
    private static final String TAG_BROADCAST_STATISTICS = "BroadcastStatistics";
    private static boolean sBDReceiverStatsEnabled;
    private BroadcastQueue mQueue;
    private HashMap<BroadcastRecord, BroadcastRecordTimer> mBDRecordTimerHM = new HashMap<>();
    private HashMap<String, SenderStats> mAllBroadcastStats = new HashMap<>(16);
    final StringBuilder mStringBuilder = new StringBuilder(256);

    public BroadcastQueueSmtBase(BroadcastQueue queue) {
        this.mQueue = queue;
    }

    private class BroadcastRecordTimer {
        String action;
        long endTime;
        String receiver;
        long startTime;

        BroadcastRecordTimer(long startTime, long endTime, String receiver, String action) {
            this.startTime = startTime;
            this.endTime = endTime;
            this.receiver = receiver;
            this.action = action;
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
            Slog.d(BroadcastQueueSmtBase.TAG_BROADCAST_STATISTICS, String.format(Locale.CHINA, "%s:%d", this.sender, Integer.valueOf(this.count)));
            for (BroadcastStats action : this.actions.values()) {
                action.toString();
            }
            return String.format(Locale.CHINA, "%s:%d", this.sender, Integer.valueOf(this.count));
        }
    }

    private class ReceiverStats {
        int count;
        String receiver;
        long timeTotal;

        ReceiverStats(int count, long timeTotal) {
            this.count = count;
            this.timeTotal = timeTotal;
        }

        ReceiverStats(String receiver, int count, long timeTotal) {
            this.count = count;
            this.timeTotal = timeTotal;
            this.receiver = receiver;
        }

        public String toString() {
            Slog.d(BroadcastQueueSmtBase.TAG_BROADCAST_STATISTICS, String.format(Locale.CHINA, "  %s:%s:%s", this.receiver, Integer.valueOf(this.count), Long.valueOf(this.timeTotal)));
            return String.format(Locale.CHINA, "%s:%s:%s", this.receiver, Integer.valueOf(this.count), Long.valueOf(this.timeTotal));
        }
    }

    private class BroadcastStats {
        int count;
        String intentAction;
        boolean ordered;
        HashMap<String, ReceiverStats> receivers = new HashMap<>(10);

        BroadcastStats(String action, int count, boolean ordered) {
            this.intentAction = action;
            this.count = count;
            this.ordered = ordered;
        }

        public String toString() {
            Slog.d(BroadcastQueueSmtBase.TAG_BROADCAST_STATISTICS, String.format(Locale.CHINA, " %s:%d:%b", this.intentAction, Integer.valueOf(this.count), Boolean.valueOf(this.ordered)));
            for (ReceiverStats action : this.receivers.values()) {
                action.toString();
            }
            return String.format(Locale.CHINA, " %s:%d:%b", this.intentAction, Integer.valueOf(this.count), Boolean.valueOf(this.ordered));
        }
    }

    protected static void setBDReceiverStatsEnabled(boolean enable) {
        sBDReceiverStatsEnabled = enable;
    }

    protected void processOrdeBroadcastStats(BroadcastRecord r) {
        if (!sBDReceiverStatsEnabled || r == null || this.mBDRecordTimerHM.containsKey(r) || this.mBDRecordTimerHM.get(r) != null || r.intent == null) {
            return;
        }
        String receiver = null;
        if (r.intent.getComponent() != null) {
            receiver = r.intent.getComponent().flattenToShortString();
        } else if (r.intent.getPackage() != null) {
            receiver = r.intent.getPackage();
        }
        if (receiver == null) {
            return;
        }
        long start = SystemClock.uptimeMillis();
        BroadcastRecordTimer bDTimer = new BroadcastRecordTimer(start, 0L, receiver, r.intent.getAction());
        this.mBDRecordTimerHM.put(r, bDTimer);
    }

    protected void processParallelBroadcastsStats(BroadcastRecord r, String receiver) {
        if (!sBDReceiverStatsEnabled || r == null || receiver == null || r.intent == null) {
            return;
        }
        String action = r.intent.getAction();
        SenderStats senderStats = this.mAllBroadcastStats.get(r.callerPackage);
        if (senderStats != null) {
            senderStats.count++;
        } else {
            senderStats = new SenderStats(r.callerPackage, 1);
            this.mAllBroadcastStats.put(r.callerPackage, senderStats);
        }
        BroadcastStats bdStats = senderStats.actions.get(action);
        if (bdStats != null) {
            bdStats.count++;
        } else {
            bdStats = new BroadcastStats(action, 1, r.ordered);
            senderStats.actions.put(action, bdStats);
        }
        ReceiverStats receiverStats = bdStats.receivers.get(receiver);
        if (receiverStats != null) {
            receiverStats.count++;
        } else {
            bdStats.receivers.put(receiver, new ReceiverStats(receiver, 1, -1L));
        }
    }

    protected void finishOrderBroadcastStats(BroadcastRecord r) {
        SenderStats senderStats;
        BroadcastStats bdStats;
        if (sBDReceiverStatsEnabled && r != null && this.mBDRecordTimerHM.containsKey(r)) {
            BroadcastRecordTimer recordTimer = this.mBDRecordTimerHM.remove(r);
            if (recordTimer.startTime == 0) {
                this.mBDRecordTimerHM.remove(r);
                return;
            }
            String receiver = recordTimer.receiver;
            String action = recordTimer.action;
            long endTime = SystemClock.uptimeMillis();
            SenderStats senderStats2 = this.mAllBroadcastStats.get(r.callerPackage);
            if (senderStats2 == null) {
                SenderStats senderStats3 = new SenderStats(r.callerPackage, 1);
                this.mAllBroadcastStats.put(r.callerPackage, senderStats3);
                senderStats = senderStats3;
            } else {
                senderStats2.count++;
                senderStats = senderStats2;
            }
            BroadcastStats bdStats2 = senderStats.actions.get(action);
            if (bdStats2 == null) {
                BroadcastStats bdStats3 = new BroadcastStats(action, 1, r.ordered);
                senderStats.actions.put(action, bdStats3);
                bdStats = bdStats3;
            } else {
                bdStats2.count++;
                bdStats = bdStats2;
            }
            ReceiverStats receiverStats = bdStats.receivers.get(receiver);
            if (receiverStats != null) {
                receiverStats.count++;
                receiverStats.timeTotal += endTime - recordTimer.startTime;
            } else {
                bdStats.receivers.put(receiver, new ReceiverStats(receiver, 1, endTime - recordTimer.startTime));
            }
        }
    }

    protected void outputBDReceiverStatistics() {
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

    protected void clearStats() {
        this.mAllBroadcastStats.clear();
    }

    protected boolean isBroadcastAllowStart(ResolveInfo info, BroadcastRecord r) {
        if (!this.mQueue.mService.getSmtEx().mProcessIntercept.isBroadcastAllowStart(info, r)) {
            this.mQueue.finishReceiverLocked(r, r.resultCode, r.resultData, r.resultExtras, r.resultAbort, false);
            this.mQueue.scheduleBroadcastsLocked();
            r.state = 0;
            return false;
        }
        return true;
    }
}
