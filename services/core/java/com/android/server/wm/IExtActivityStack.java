// Copyright 2026 Picomisu contributors
// SPDX-License-Identifier: Apache-2.0

package com.android.server.wm;

import com.pico.util.IExtBase;

/**
 * PICO activity stack extension. Reconstructed from the PICO OS 5.13.7 factory services; only
 * the activity timeout report (IApplicationThread.scheduleActivityTimeout) is present.
 * @hide
 */
public interface IExtActivityStack extends IExtBase {
    void scheduleActivityTimeout(ActivityRecord r, String reason);
}
