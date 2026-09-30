// Copyright 2026 Picomisu contributors
// SPDX-License-Identifier: Apache-2.0

package com.android.server.am;

/**
 * Smartisan monitor extension state of a {@link ProcessRecord} (its {@code mMonitorEx}).
 * Reconstructed from the PICO OS 5.13.7 factory services; the getSmtUid() helpers (which need
 * the Smartisan android.util.SmtUidUtil) are not present.
 *
 * @hide
 */
public class ProcessRecordMonitorEx {
    /** Whether this isolated process belongs to a third party application. */
    public boolean isolatedOf3rdPartApp;
    protected ProcessRecord mProcessRecord;
    int smtUid = 1000;

    public ProcessRecordMonitorEx(ProcessRecord processRecord) {
        mProcessRecord = processRecord;
    }
}
