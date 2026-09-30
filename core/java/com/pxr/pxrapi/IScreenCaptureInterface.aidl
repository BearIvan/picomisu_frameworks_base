// Copyright 2026 Picomisu contributors
// SPDX-License-Identifier: Apache-2.0
// Reconstructed from the factory PICO OS 5.13.7 DEX by tools/reconstruct-pico-aidl.py.
package com.pxr.pxrapi;

import android.view.Surface;

/** @hide */
interface IScreenCaptureInterface {
    int startCapture(in Surface surface, int captureType, int width, int height);
    void stopCapture();
}
