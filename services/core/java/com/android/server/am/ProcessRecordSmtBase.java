// Copyright 2026 Picomisu contributors
// SPDX-License-Identifier: Apache-2.0

package com.android.server.am;

/**
 * Smartisan extension state of a {@link ProcessRecord} (its {@code mSmtEx}). Reconstructed
 * from the PICO OS 5.13.7 factory services; only the members reached by the Smartisan
 * {@code IActivityManagerSmtEx} methods are present.
 *
 * @hide
 */
public class ProcessRecordSmtBase {
    /** Set by {@code IActivityManagerSmtEx.freezePrefetchApp} for the calling process. */
    boolean delayFreezing = false;
    protected String smtExtraInfo;
    protected ProcessRecord mProcessRecord;

    public ProcessRecordSmtBase(ProcessRecord processRecord) {
        mProcessRecord = processRecord;
    }

    public String getSmtExtraInfo() {
        return smtExtraInfo;
    }

    public void setSmtExtraInfo(String info) {
        smtExtraInfo = info;
    }
}
