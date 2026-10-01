// Copyright 2026 Picomisu contributors
// SPDX-License-Identifier: Apache-2.0

package com.android.server.audio;

/**
 * A stream volume change reported to {@link AudioEventTracker}, as in the PICO OS 5.13.7
 * factory services.
 */
class VolumeChangedEvent {
    public String callingPackage;
    public int device;
    public int index;
    public int oldIndex;
    public int streamType;

    public VolumeChangedEvent(int streamType, int oldIndex, int index, int device,
            String callingPackage) {
        this.streamType = streamType;
        this.oldIndex = oldIndex;
        this.index = index;
        this.device = device;
        this.callingPackage = callingPackage;
    }
}
