// Copyright 2026 Picomisu contributors
// SPDX-License-Identifier: Apache-2.0
package com.android.server.backup.fullbackup;

import com.pico.util.IExtBase;

import java.util.List;

/**
 * PICO full backup engine extension: options of a PICO backup (manifest without signatures,
 * agent timeout, include/exclude paths, keeping the app alive afterwards).
 * @hide
 */
public interface IExtFullBackupEngine extends IExtBase {
    default boolean backupEndNotKill() {
        return false;
    }

    default List<String> getExcludePaths() {
        return null;
    }

    default List<String> getIncludePaths() {
        return null;
    }

    default long getTimeout() {
        return 0;
    }

    default boolean isIgnoreSignature() {
        return false;
    }

    void setBackupEndNotKill(boolean notKill);
    void setBackupPaths(List<String> includePaths, List<String> excludePaths);
    void setIgnoreSignature(boolean ignoreSignature);
    void setTimeout(long timeout);
}
