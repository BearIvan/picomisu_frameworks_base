// Copyright 2026 Picomisu contributors
// SPDX-License-Identifier: Apache-2.0
// Reconstructed from the factory PICO OS 5.13.7 DEX by tools/reconstruct-pico-aidl.py.
package com.pxr.net;

import com.pxr.net.ILinkLayerQualityListener;
import com.pxr.net.INetworkQualityListener;
import com.pxr.net.LinkLayerQuality;
import com.pxr.net.PxrWifiConnectionInfo;

/** @hide */
interface IPxrNetworkManager {
    boolean registerNetworkQualityChangedListener(String callingPackage, int timeout, INetworkQualityListener listener);
    boolean unregisterNetworkQualityChangedListener(String callingPackage, INetworkQualityListener listener);
    boolean registerLinkLayerQualityChangedListener(String callingPackage, ILinkLayerQualityListener listener);
    boolean unregisterLinkLayerQualityChangedListener(String callingPackage, ILinkLayerQualityListener listener);
    void updateLinkLayerQuality(String callingPackage, in PxrWifiConnectionInfo pwci);
    LinkLayerQuality getLinkLayerQuality(String callingPackage);
    LinkLayerQuality getAverageLinkLayerQuality(String callingPackage);
}
