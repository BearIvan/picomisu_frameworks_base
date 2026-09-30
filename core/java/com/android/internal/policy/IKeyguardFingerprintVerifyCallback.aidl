// Copyright 2026 Picomisu contributors
// SPDX-License-Identifier: Apache-2.0
// Reconstructed from the factory PICO OS 5.13.7 DEX by tools/reconstruct-pico-aidl.py.
package com.android.internal.policy;

/** @hide */
interface IKeyguardFingerprintVerifyCallback {
    oneway void onAuthenticationFailed();
    oneway void onAuthenticationSucceeded();
    oneway void onAuthenticationHelp(int helpMsgId, in CharSequence helpString);
    oneway void onAuthenticationError(int errMsgId, in CharSequence errString);
    oneway void onAuthenticationAcquired(int acquireInfo);
}
