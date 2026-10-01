// Copyright 2026 Picomisu contributors
// SPDX-License-Identifier: Apache-2.0

package com.android.server.am;

import android.util.ArraySet;
import android.util.SparseArray;

/**
 * Reconstructed from the PICO OS 5.13.7 factory services.
 *
 * @hide
 */
public class UidRecordSmtBase {
    static final int CHANGE_FROZEN = 131072;
    static final int CHANGE_SCHEDGROUP = 65536;
    static final int FLAG_CONNECTED_SYSTEMSERVER = 1;
    static final int FLAG_CONNECTED_TOP = 2;
    int SmtFlags;
    int curFrozenStat;
    int curSchedGroup;
    String not3rdReason;
    long perceptibleTime;
    SparseArray<UidRecord> systemSmtUidRecoreds;
    IApplicationFreezer.Mode freezingMode = IApplicationFreezer.Mode.INVALID;
    int setSchedGroup = Integer.MIN_VALUE;
    int setFrozenStat = Integer.MAX_VALUE;
    int freezingStat = 0;
    long freezingStartTime = 0;
    int tntPriorityFraction = 0;
    ArraySet<ProcessRecord> procRecords = new ArraySet<>();

    boolean isFreezing() {
        return this.freezingStat == 1;
    }

    boolean isFrozen() {
        return this.freezingStat == 2;
    }

    boolean inFreezeStat() {
        return this.freezingStat != 0;
    }

    static final class ChangeItemSmtEx {
        int frozenStat;
        int schedGroup;

        ChangeItemSmtEx() {
        }
    }

    void reset() {
        this.curSchedGroup = Integer.MIN_VALUE;
        this.not3rdReason = "";
        this.SmtFlags = 0;
        this.curFrozenStat = Integer.MAX_VALUE;
    }

    public void initSmtUidrecord(int uid) {
        if (uid == 1000) {
            this.systemSmtUidRecoreds = new SparseArray<>();
        }
    }

    public UidRecord getSystemSmtUidRecord(int smtUid) {
        SparseArray<UidRecord> sparseArray = this.systemSmtUidRecoreds;
        if (sparseArray == null || smtUid > 0) {
            return null;
        }
        UidRecord recored = sparseArray.get(smtUid);
        if (recored == null) {
            UidRecord recored2 = new UidRecord(smtUid);
            this.systemSmtUidRecoreds.put(smtUid, recored2);
            return recored2;
        }
        return recored;
    }
}
