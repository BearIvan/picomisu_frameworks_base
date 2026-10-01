// Copyright 2026 Picomisu contributors
// SPDX-License-Identifier: Apache-2.0
package android.view.inputmethod;

import android.os.Parcel;
import android.util.SparseArray;
import android.view.View;

import com.pico.util.IExtBase;

/**
 * PICO input method manager extension (factory PICO OS 5.13.7
 * android.view.inputmethod.IExtInputMethodManager).
 * @hide
 */
public interface IExtInputMethodManager extends IExtBase {
    /** IInputMethodClient transaction: system_server removed this client. */
    int CODE_ON_CLIENT_REMOVED = 10000;

    View getServedView();

    boolean onTransact(int code, Parcel data, Parcel reply, int flags, Object sLock,
            SparseArray<InputMethodManager> sInstanceMap);
}
