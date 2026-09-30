// Copyright 2026 Picomisu contributors
// SPDX-License-Identifier: Apache-2.0

package android.app;

import android.content.Context;
import android.os.DebugSmtEx;

/**
 * Smartisan ART method tracer, implemented by the optional sys framework JAR
 * ({@link SysFwBridge}). Reconstructed from the PICO OS 5.13.7 factory framework; the default
 * method is the factory behaviour when that JAR is absent.
 *
 * @hide
 */
public interface IArtTracer {
    default void startArtTracer(String[] params, Context context) {
        DebugSmtEx.printDefaultFunInfo(getClass());
    }
}
