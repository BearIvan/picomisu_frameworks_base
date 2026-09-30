// Copyright 2026 Picomisu contributors
// SPDX-License-Identifier: Apache-2.0

package android.net;

/**
 * Smartisan extension of {@link ConnectivityManager}: per-process cache of the active network
 * info. Reconstructed from the PICO OS 5.13.7 factory framework; only the process-wide cache
 * and its reset, reached by the ported ActivityThread (unfreeze) code, are present. The
 * factory ConnectivityManager hooks that fill the cache (getSmtEx(), getCacheOrBinderCall,
 * getAllNetworkInfoSmtEx, the NetworkCallback that clears it) are not ported yet.
 *
 * @hide
 */
public class ConnectivityManagerSmtEx {
    public static volatile NetworkInfo sNetworkInfoCache = null;

    public static void clearActiveNetworkInfoCache() {
        sNetworkInfoCache = null;
    }
}
