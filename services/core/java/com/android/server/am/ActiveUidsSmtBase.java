// Copyright 2026 Picomisu contributors
// SPDX-License-Identifier: Apache-2.0

package com.android.server.am;

import com.android.server.SysOptBridge;

/**
 * Reconstructed from the PICO OS 5.13.7 factory services.
 *
 * @hide
 */
public class ActiveUidsSmtBase {
    private IActiveUidsOptEx mOptEx = SysOptBridge.getFactory().getActiveUidsOptEx();
    private ActiveUids mUids;
    public static int ACTION_ADD = 1;
    public static int ACTION_REMOVE = 2;
    public static int ACTION_CLEAR = 3;

    ActiveUidsSmtBase(ActiveUids uids) {
        this.mUids = uids;
    }

    public void onUidEvent(int action, int uid, UidRecord record) {
        this.mOptEx.onUidEvent(action, uid, record);
    }
}
