// Copyright 2026 Picomisu contributors
// SPDX-License-Identifier: Apache-2.0

package android.app;

/**
 * Factory of the Smartisan framework extensions implemented by the optional sys framework JAR
 * ({@code android.app.SysFwFactoryImpl}, loaded by {@link SysFwBridge}). Reconstructed from the
 * PICO OS 5.13.7 factory framework; only the getters reached by the ported factory code are
 * present, with their factory default implementations.
 *
 * @hide
 */
public interface ISysFwFactory {
    default IArtTracer getArtTracerUtils() {
        return new IArtTracer() {};
    }
}
