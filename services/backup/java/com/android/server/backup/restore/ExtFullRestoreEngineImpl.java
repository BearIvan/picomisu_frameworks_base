// Copyright 2026 Picomisu contributors
// SPDX-License-Identifier: Apache-2.0
package com.android.server.backup.restore;

import static com.android.server.backup.BackupManagerService.TAG;

import android.util.Slog;

/**
 * PICO full restore engine extension.
 * @hide
 */
public class ExtFullRestoreEngineImpl implements IExtFullRestoreEngine {
    private FullRestoreEngine mBase;
    private long mTimeout;
    private boolean mIgnoreSignatureAndAllowFlag = false;

    public ExtFullRestoreEngineImpl(FullRestoreEngine base) {
        mBase = base;
    }

    @Override
    public long getTimeout() {
        return mTimeout;
    }

    @Override
    public boolean isIgnoreSignatureAndAllowFlag() {
        return mIgnoreSignatureAndAllowFlag;
    }

    @Override
    public void setIgnoreSignatureAndAllowFlag(boolean ignore) {
        mIgnoreSignatureAndAllowFlag = ignore;
    }

    @Override
    public void setTimeout(long timeout) {
        mTimeout = timeout;
        Slog.w(TAG, "prepare restore timeout: " + mTimeout);
    }
}
