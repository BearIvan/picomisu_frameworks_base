// Copyright 2026 Picomisu contributors
// SPDX-License-Identifier: Apache-2.0

package android.view;

import android.content.pm.ApplicationInfo;

import java.util.HashSet;

/**
 * Smartisan window manager policy constants. Reconstructed from the PICO OS 5.13.7 factory
 * framework.
 *
 * @hide
 */
public interface WindowManagerPolicyConstantsSmtEx {
    int FLAG_EXTERNAL = 0x80000000;
    int FLAG_EXTERNAL_TOUCHPAD = 0x00200000;
    int PRESENCE_EXTERNAL_LID_KEYBOARD = 4;
    int OFF_BECAUSE_OF_PROXIMITY = 24;

    /** @hide */
    interface VisibleWindowChangeListenerSmtEx {
        void onVisibleWindowAdd(String packageName, String windowName);

        void onVisibleWindowClear();

        void onVisibleUidsChange(HashSet<Integer> addedUids, HashSet<Integer> removedUids);

        void onVisibleApplicationInfosChange(HashSet<ApplicationInfo> visibleInfos,
                HashSet<ApplicationInfo> addedInfos, HashSet<ApplicationInfo> removedInfos);
    }
}
