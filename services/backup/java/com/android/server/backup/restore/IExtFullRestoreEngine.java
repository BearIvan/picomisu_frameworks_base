// Copyright 2026 Picomisu contributors
// SPDX-License-Identifier: Apache-2.0
package com.android.server.backup.restore;

import com.pico.util.IExtBase;

/**
 * PICO full restore engine extension: options of a PICO restore (agent timeout, restore
 * regardless of the signatures and of the allowBackup flag).
 * @hide
 */
public interface IExtFullRestoreEngine extends IExtBase {
    default long getTimeout() {
        return 0;
    }

    default boolean isIgnoreSignatureAndAllowFlag() {
        return false;
    }

    void setIgnoreSignatureAndAllowFlag(boolean ignore);
    void setTimeout(long timeout);
}
