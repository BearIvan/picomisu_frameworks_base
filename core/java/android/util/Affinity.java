// Copyright 2026 Picomisu contributors
// SPDX-License-Identifier: Apache-2.0

package android.util;

/**
 * PICO OS 5.13.7: binds the calling thread to a set of CPUs (bit mask), as in the factory
 * framework.jar.
 *
 * @hide
 */
public class Affinity {
    public static native void bindToCpu(int cpu);

    public void bindThreadToCpu(int cpu) {
        bindToCpu(cpu);
    }
}
