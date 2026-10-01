// Copyright 2026 Picomisu contributors
// SPDX-License-Identifier: Apache-2.0

package com.android.server.am;

import com.android.server.SysOptBridge;

/**
 * Reconstructed from the PICO OS 5.13.7 factory services.
 *
 * @hide
 */
public class OomAdjusterSmtEx {
    public IMemoryProcessController mMemProcessController =
            SysOptBridge.getFactory().getMemoryProcessController();

    int computeOomAdjForPrefetch(ProcessRecord app, int schedGroup) {
        int computeSchedGroup = schedGroup;
        if (app.info.getSmtEx().doPrefetch != 0) {
            computeSchedGroup = -1;
        }
        if (app.getSmtEx().isStartDuringPrefetch) {
            computeSchedGroup = 3;
        }
        if (app.getSmtEx().freezeFromPrefetch) {
            app.curAdj = 900;
        }
        return computeSchedGroup;
    }
}
