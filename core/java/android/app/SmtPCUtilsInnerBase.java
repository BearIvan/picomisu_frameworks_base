// Copyright 2026 Picomisu contributors
// SPDX-License-Identifier: Apache-2.0

package android.app;

import java.util.HashSet;
import java.util.Set;

/**
 * Smartisan PC mode state used inside the framework and system server; PC mode is never
 * entered in the factory base implementation. Reconstructed from the PICO OS 5.13.7 factory
 * framework.
 *
 * @hide
 */
public class SmtPCUtilsInnerBase extends SmtPCUtilsSmtBase {
    public static final String TAG = "SmtPCUtilsSmtBase#";

    protected static int sDisplayIdInPcMode = -1;

    public static final String SCREEN_SHOT_PKG = "com.android.gallery3d";
    public static final String SCREEN_SHOT_CLS = "com.android.gallery3d.tablet.TabletScreenshot";
    public static final String PROCESS_SCREEN_RECORDER = "com.smartisanos.screenrecorder:service";
    public static final Set<String> DisableScrollRepeatWins = new HashSet<>(1);
    public static final Set<String> DisableScrollRepeatPKG = new HashSet<>(2);
    public static final String PKG_VIRTUAL_REMOTER = "com.smartisanos.virtualremoter";

    public static final Set<String> sSupportRotateList = new HashSet<>(1);

    public static final boolean isPcMode() {
        return false;
    }
}
