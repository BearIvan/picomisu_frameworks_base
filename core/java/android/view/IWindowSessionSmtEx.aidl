// Copyright 2026 Picomisu contributors
// SPDX-License-Identifier: Apache-2.0
// Reconstructed from the factory PICO OS 5.13.7 DEX by tools/reconstruct-pico-aidl.py.
package android.view;

/** @hide */
interface IWindowSessionSmtEx {
    void updateVisibleSurfaceViewArea(int pid, int width, int height, boolean currentVisible);
    void onSurfaceViewVisibilityChanged(int pid, int visibility);
}
