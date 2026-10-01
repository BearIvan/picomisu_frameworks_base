// Copyright 2026 Picomisu contributors
// SPDX-License-Identifier: Apache-2.0

package android.os;

/**
 * Smartisan "doppelganger" (app twin) user constants
 * (factory PICO OS 5.13.7 {@code android.os.UserHandleSmtEx}).
 *
 * @hide
 */
public class UserHandleSmtEx {
    public static final int USER_DOPPELGANGER = 10;
    public static final UserHandle DOPPELGANGER = new UserHandle(USER_DOPPELGANGER);

    public static final boolean isUserDoppelganger(int userId) {
        return userId == USER_DOPPELGANGER;
    }
}
