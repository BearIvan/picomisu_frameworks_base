// Copyright 2026 Picomisu contributors
// SPDX-License-Identifier: Apache-2.0

package com.android.internal.util;

import android.os.Debug;

/**
 * Smartisan extension of {@link MemInfoReader}: a fast /proc/meminfo reader and the ION/zram
 * sizes. Reconstructed from the PICO OS 5.13.7 factory framework.
 *
 * @hide
 */
public class MemInfoReaderSmtEx {
    final long[] mInfosFast = new long[Debug.MEMINFO_COUNT];
    private MemInfoReader mMemInfoReader;

    public MemInfoReaderSmtEx(MemInfoReader memInfoReader) {
        mMemInfoReader = memInfoReader;
    }

    /**
     * Amount of RAM that is not being used for anything, from {@link #readMemInfoFast}.
     */
    public long getFreeSizeFastKb() {
        return mInfosFast[Debug.MEMINFO_FREE];
    }

    public void readMemInfoFast() {
        try {
            Debug.getMemInfoFast(mInfosFast);
        } catch (Exception e) {
        }
    }

    /**
     * Amount of RAM that the kernel is being used for caches, not counting caches
     * that are mapped in to processes, from {@link #readMemInfoFast}.
     */
    public long getCachedSizeFastKb() {
        return mInfosFast[Debug.MEMINFO_BUFFERS] + mInfosFast[Debug.MEMINFO_SLAB_RECLAIMABLE]
                + mInfosFast[Debug.MEMINFO_CACHED] - mInfosFast[Debug.MEMINFO_MAPPED];
    }

    public long getIonSystemSizeKb() {
        return mMemInfoReader.mInfos[15];
    }

    public long getIonCachedSizeKb() {
        return mMemInfoReader.mInfos[16];
    }

    public long getZramPhyUsedSizeKb() {
        return mMemInfoReader.mInfos[17];
    }
}
