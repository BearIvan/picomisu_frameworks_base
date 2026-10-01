// Copyright 2026 Picomisu contributors
// SPDX-License-Identifier: Apache-2.0

package com.android.server.am;

import android.os.IBinder;
import java.io.OutputStream;
import java.nio.ByteBuffer;

/**
 * Reconstructed from the PICO OS 5.13.7 factory services.
 *
 * @hide
 */
public interface IProcessListOptEx {
    public static final int INCLUDE_ALL = 3;
    public static final int INCLUDE_FROZEN = 1;
    public static final int INCLUDE_PREFETCH = 2;
    public static final int NOT_INCLUDE = 0;

    ProcessRecord findAppProcessSmtLocked(IBinder iBinder, int i);

    ProcessRecord findFrozenAppProcessByPid(int i);

    boolean handleUnsolicitedMessage(ByteBuffer byteBuffer, int i);

    void handlerKilledPid(int i, String str, int i2);

    boolean isLmkdKilled(int i);

    void onLmkdConnect(OutputStream outputStream);
}
