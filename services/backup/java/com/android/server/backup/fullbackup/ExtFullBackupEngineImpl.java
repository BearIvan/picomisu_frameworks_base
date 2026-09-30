// Copyright 2026 Picomisu contributors
// SPDX-License-Identifier: Apache-2.0
package com.android.server.backup.fullbackup;

import static com.android.server.backup.BackupManagerService.TAG;

import android.util.Slog;

import java.util.List;

/**
 * PICO full backup engine extension.
 * @hide
 */
public class ExtFullBackupEngineImpl implements IExtFullBackupEngine {
    private FullBackupEngine mBase;
    private boolean mIgnoreSignature = false;
    private long mTimeout;
    private List<String> mIncludePaths;
    private List<String> mExcludePaths;
    private boolean mBackupEndNotKill = false;

    public ExtFullBackupEngineImpl(FullBackupEngine base) {
        mBase = base;
    }

    @Override
    public boolean backupEndNotKill() {
        return mBackupEndNotKill;
    }

    @Override
    public List<String> getExcludePaths() {
        return mExcludePaths;
    }

    @Override
    public List<String> getIncludePaths() {
        return mIncludePaths;
    }

    @Override
    public long getTimeout() {
        return mTimeout;
    }

    @Override
    public boolean isIgnoreSignature() {
        return mIgnoreSignature;
    }

    @Override
    public void setBackupEndNotKill(boolean notKill) {
        mBackupEndNotKill = notKill;
    }

    @Override
    public void setBackupPaths(List<String> includePaths, List<String> excludePaths) {
        mIncludePaths = includePaths;
        mExcludePaths = excludePaths;
    }

    @Override
    public void setIgnoreSignature(boolean ignoreSignature) {
        mIgnoreSignature = ignoreSignature;
    }

    @Override
    public void setTimeout(long timeout) {
        mTimeout = timeout;
        Slog.w(TAG, "prepare backup timeout: " + mTimeout);
    }
}
