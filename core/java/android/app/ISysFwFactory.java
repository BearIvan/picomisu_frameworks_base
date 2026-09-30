// Copyright 2026 Picomisu contributors
// SPDX-License-Identifier: Apache-2.0

package android.app;

import com.android.internal.os.IBatteryStatsHelperOptEx;
import com.android.internal.os.IBatteryStatsImplOptEx;

/**
 * Factory of the Smartisan framework extensions implemented by the optional sys framework JAR
 * ({@code android.app.SysFwFactoryImpl}, loaded by {@link SysFwBridge}). Reconstructed from the
 * PICO OS 5.13.7 factory framework, with the factory default implementations.
 *
 * @hide
 */
public interface ISysFwFactory {
    default IArtTracer getArtTracerUtils() {
        return new IArtTracer() {};
    }

    default IBatteryStatsImplOptEx getBatteryStatsImpl() {
        return new IBatteryStatsImplOptEx() {};
    }

    default IBatteryStatsHelperOptEx getBSHelperOptEx() {
        return new IBatteryStatsHelperOptEx() {};
    }

    default IBootReceiverSmtEx getBootReceiver() {
        return new IBootReceiverSmtEx() {};
    }

    default IAnrLogger getAnrLogger() {
        return new IAnrLogger() {};
    }
}
