// Copyright 2026 Picomisu contributors
// SPDX-License-Identifier: Apache-2.0
// Reconstructed from the factory PICO OS 5.13.7 DEX by tools/reconstruct-pico-aidl.py.
package com.pvr;

import android.os.Bundle;

/** @hide */
interface IPvrCallback {
    oneway void onEventChanged(in Bundle extras);
}
