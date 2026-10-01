// Copyright 2026 Picomisu contributors
// SPDX-License-Identifier: Apache-2.0
package com.android.server.am;

import com.pico.util.IExtBase;

/**
 * PICO user controller extension (factory PICO OS 5.13.7
 * com.android.server.am.IExtUserController).
 * @hide
 */
public interface IExtUserController extends IExtBase {
    void finishUserUnlocked(ActivityManagerService service);

    void startPicoFactoryTestService(ActivityManagerService service);
}
