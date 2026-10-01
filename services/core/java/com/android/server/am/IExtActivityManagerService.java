// Copyright 2026 Picomisu contributors
// SPDX-License-Identifier: Apache-2.0
package com.android.server.am;

import com.pico.util.IExtBase;

/**
 * PICO activity manager extension (factory PICO OS 5.13.7
 * com.android.server.am.IExtActivityManagerService).
 * @hide
 */
public interface IExtActivityManagerService extends IExtBase {
    void handleAppDiedLocked(ProcessRecord app);

    void onSystemReadyFinished();

    void startPicoPersistentService();

    void updatePersistentApplicationInfo(ProcessRecord app);
}
