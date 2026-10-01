// Copyright 2026 Picomisu contributors
// SPDX-License-Identifier: Apache-2.0

package com.android.server;

import java.io.Serializable;

/**
 * Reconstructed from the PICO OS 5.13.7 factory services.
 *
 * @hide
 */
public class PrefetchAppRecord implements Serializable {
    public int mFlag;
    public String mPackageName;
}
