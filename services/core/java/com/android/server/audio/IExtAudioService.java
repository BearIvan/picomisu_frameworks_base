// Copyright 2026 Picomisu contributors
// SPDX-License-Identifier: Apache-2.0

package com.android.server.audio;

import android.media.PlayerBase;

import com.pico.util.IExtBase;

import java.io.FileDescriptor;
import java.io.PrintWriter;

/**
 * PICO audio service extension (background playback muting), as in the PICO OS 5.13.7
 * factory services.
 *
 * @hide
 */
interface IExtAudioService extends IExtBase {
    void dump(FileDescriptor fd, PrintWriter pw, String[] args);

    void initialize();

    void setPlaybackActivityMonitor(PlaybackActivityMonitor monitor);

    void trackPlayer(int piid, int uid, int pid, PlayerBase.PlayerIdCard pic);
}
