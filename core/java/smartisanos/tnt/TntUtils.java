// Copyright 2026 Picomisu contributors
// SPDX-License-Identifier: Apache-2.0

package smartisanos.tnt;

import android.os.SystemClock;
import android.view.KeyEvent;

/**
 * TNT helpers (factory PICO OS 5.13.7 {@code smartisanos.tnt.TntUtils}; on the factory
 * sendEvent only builds the key event and does not inject it).
 *
 * @hide
 */
public class TntUtils {
    public static void sendEvent(long downTime, int action, int code, int metaState,
            int deviceId, int flags, int source) {
        final long eventTime = action == KeyEvent.ACTION_DOWN
                ? downTime : SystemClock.uptimeMillis();
        final KeyEvent ev = new KeyEvent(downTime, eventTime, action, code, 0, metaState,
                deviceId, 0, flags, source);
    }
}
