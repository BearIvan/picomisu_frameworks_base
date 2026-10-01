// Copyright 2026 Picomisu contributors
// SPDX-License-Identifier: Apache-2.0

package com.android.server;

/**
 * Reconstructed from the PICO OS 5.13.7 factory services.
 *
 * @hide
 */
public class LocationManagerServiceSmtEx {

    public class UpdateRecordSmtEx {
        protected boolean mFrozen;

        public UpdateRecordSmtEx() {
        }

        protected void updateFrozen(boolean frozen) {
            this.mFrozen = frozen;
        }
    }
}
