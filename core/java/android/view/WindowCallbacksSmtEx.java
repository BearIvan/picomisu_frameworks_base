// Copyright 2026 Picomisu contributors
// SPDX-License-Identifier: Apache-2.0

package android.view;

/**
 * Smartisan window callbacks (factory PICO OS 5.13.7 {@code android.view.WindowCallbacksSmtEx};
 * nothing in the factory jars implements it).
 *
 * @hide
 */
public interface WindowCallbacksSmtEx {
    default void onDecorViewSwipeFromLeftEdge(int gestureStatus) {
    }
}
