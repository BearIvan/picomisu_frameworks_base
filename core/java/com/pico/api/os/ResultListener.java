// Copyright 2026 Picomisu contributors
// SPDX-License-Identifier: Apache-2.0
package com.pico.api.os;

import android.os.Bundle;

/**
 * Receives a result {@link Bundle} delivered through a {@link RemoteCallbackProxy}.
 *
 * @hide
 */
public interface ResultListener {
    void onResult(Bundle result);
}
