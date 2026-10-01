// Copyright 2026 Picomisu contributors
// SPDX-License-Identifier: Apache-2.0

package com.android.server.am;

import android.util.SmtUidUtil;

/**
 * Reconstructed from the PICO OS 5.13.7 factory services.
 *
 * @hide
 */
public class ProcessRecordMonitorEx {
    public boolean isolatedOf3rdPartApp;
    protected ProcessRecord mProcessRecord;
    int smtUid = 1000;

    public ProcessRecordMonitorEx(ProcessRecord record) {
        this.mProcessRecord = record;
    }

    public int getSmtUid(int uid, String packageName) {
        if (uid != 1000) {
            return uid;
        }
        if (this.smtUid == 1000) {
            this.smtUid = SmtUidUtil.getSystemUidForPackage(packageName);
        }
        int result = this.smtUid;
        return result;
    }

    public int getSmtUid() {
        int result = this.mProcessRecord.uid;
        if (this.mProcessRecord.info != null) {
            int result2 = getSmtUid(this.mProcessRecord.info.uid, this.mProcessRecord.info.packageName);
            return result2;
        }
        return result;
    }
}
