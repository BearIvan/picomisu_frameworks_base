// Copyright 2026 Picomisu contributors
// SPDX-License-Identifier: Apache-2.0
// Reconstructed from the factory PICO OS 5.13.7 DEX by tools/reconstruct-pico-aidl.py.
package android.app;

import android.app.IUnFreezeCallback;

/** @hide */
interface IFreezeManager {
    void registerUnFreezeListener(int remotePid, IUnFreezeCallback callBack, boolean byOnce);
    void unRegisterUnFreezeListener(int remotePid, IUnFreezeCallback callBack);
    void freezeUid(int uid);
}
