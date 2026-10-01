// Copyright 2026 Picomisu contributors
// SPDX-License-Identifier: Apache-2.0
package android.view.inputmethod;

import android.os.Parcel;
import android.pico.utils.Features;
import android.util.SparseArray;
import android.view.View;

/**
 * PICO input method manager extension (factory PICO OS 5.13.7
 * android.view.inputmethod.ExtInputMethodManagerImpl).
 * @hide
 */
public class ExtInputMethodManagerImpl implements IExtInputMethodManager {
    private InputMethodManager mBase;

    public ExtInputMethodManagerImpl(InputMethodManager base) {
        mBase = base;
    }

    /**
     * IInputMethodClient.onTransact: system_server removed this client because its 2D app
     * display went away; drop the cached per-display instance.
     */
    @Override
    public boolean onTransact(int code, Parcel data, Parcel reply, int flags, Object sLock,
            SparseArray<InputMethodManager> sInstanceMap) {
        if (code == CODE_ON_CLIENT_REMOVED) {
            if (Features.limitTheNumberOfDisplayCaches()) {
                synchronized (sLock) {
                    int index = sInstanceMap.indexOfValue(mBase);
                    if (index >= 0) {
                        sInstanceMap.removeAt(index);
                    }
                }
                return true;
            }
            return true;
        }
        return false;
    }

    @Override
    public View getServedView() {
        return mBase.mServedView;
    }
}
