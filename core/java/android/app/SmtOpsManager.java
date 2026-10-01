// Copyright 2026 Picomisu contributors
// SPDX-License-Identifier: Apache-2.0

package android.app;

/**
 * Reconstructed from the PICO OS 5.13.7 factory framework.
 *
 * @hide
 */
public class SmtOpsManager {
    public static final int MODE_ALLOWED = 0;
    public static final int MODE_IGNORED = 1;
    public static final int OP_BLUETOOTH_CHANGE = 13;
    public static final int OP_BOOT_COMPLETED = 21;
    public static final int OP_THIRD_PARTY_BOOTABLE = 28;
    private static final String[] sModeNames = {"invalid", "allowed", "ignored", "ask", "during_use"};

    public int noteOp(int op, int uid, String packageName) {
        return 1;
    }

    public static String modeToName(int mode) {
        return sModeNames[mode + 1];
    }
}
