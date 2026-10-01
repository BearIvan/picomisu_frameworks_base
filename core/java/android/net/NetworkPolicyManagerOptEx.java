// Copyright 2026 Picomisu contributors
// SPDX-License-Identifier: Apache-2.0

package android.net;

/**
 * Smartisan background network restriction policy constants
 * (factory PICO OS 5.13.7 {@code android.net.NetworkPolicyManagerOptEx}).
 *
 * @hide
 */
public class NetworkPolicyManagerOptEx {
    public static final int POLICY_REJECT_ALL_BACKGROUND = 0x4;
    public static final int POLICY_REJECT_ALL_BACKGROUND_SCREEN_OFF = 0x8;
    public static final int RULE_REJECT_ALL_BG = 1 << 7;
}
