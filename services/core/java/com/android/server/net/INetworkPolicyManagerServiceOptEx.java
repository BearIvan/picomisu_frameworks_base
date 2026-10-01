// Copyright 2026 Picomisu contributors
// SPDX-License-Identifier: Apache-2.0

package com.android.server.net;

import android.content.Context;
import android.os.DebugSmtEx;
import com.android.internal.util.IndentingPrintWriter;

/**
 * Reconstructed from the PICO OS 5.13.7 factory services.
 *
 * @hide
 */
public interface INetworkPolicyManagerServiceOptEx {
    default void init(Context context, NetworkPolicyManagerService policyManagerService) {
    }

    default int getOldBgUidRule(int oldUidRules) {
        DebugSmtEx.printDefaultFunInfo(getClass());
        return 0;
    }

    default int getNewBgUidRule(int uidPolicy, boolean isForeground) {
        DebugSmtEx.printDefaultFunInfo(getClass());
        return 0;
    }

    default int getNewUidRules(int newRule, int oldUidRules, int newBgUidRule) {
        DebugSmtEx.printDefaultFunInfo(getClass());
        return 0;
    }

    default void updateAllUidNetworkRules(int uid, int oldBgUidRule, int newBgUidRule, int newUidRules) {
        DebugSmtEx.printDefaultFunInfo(getClass());
    }

    default void dump(IndentingPrintWriter fout) {
        DebugSmtEx.printDefaultFunInfo(getClass());
    }
}
