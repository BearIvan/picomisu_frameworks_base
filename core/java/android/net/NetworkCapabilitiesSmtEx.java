// Copyright 2026 Picomisu contributors
// SPDX-License-Identifier: Apache-2.0

package android.net;

import android.os.Parcel;

/**
 * Reconstructed from the PICO OS 5.13.7 factory framework.
 *
 * @hide
 */
public class NetworkCapabilitiesSmtEx {
    public boolean mCallbackForCache = false;
    private NetworkCapabilities mNetworkCapabilities;

    public NetworkCapabilitiesSmtEx(NetworkCapabilities networkCapabilities) {
        this.mNetworkCapabilities = networkCapabilities;
    }

    public void set(NetworkCapabilitiesSmtEx networkCapabilitiesSmtEx) {
        this.mCallbackForCache = networkCapabilitiesSmtEx.mCallbackForCache;
    }

    public void writeToParcelSmtEx(Parcel dest) {
        dest.writeBoolean(this.mCallbackForCache);
    }

    public void createFromParcelSmtEx(Parcel in) {
        this.mCallbackForCache = in.readBoolean();
    }
}
