// Copyright 2026 Picomisu contributors
// SPDX-License-Identifier: Apache-2.0
package com.pico.api.app;

import android.os.Parcel;
import android.os.RemoteException;

/**
 * An {@link IAppSession} binder that hands every incoming transaction to an
 * {@link AppSessionCallback} after enforcing the interface descriptor.
 *
 * @hide
 */
public class AppSession extends IAppSession.Stub {
    public static final String APP_SESSION_DESCRIPTOR = "com.pico.api.app.IAppSession";

    private AppSessionCallback mCallback;

    public AppSession(AppSessionCallback callback) {
        mCallback = callback;
    }

    @Override
    public boolean onTransact(int code, Parcel data, Parcel reply, int flags)
            throws RemoteException {
        if (mCallback != null) {
            data.enforceInterface(APP_SESSION_DESCRIPTOR);
            if (mCallback.onTransact(code, data, reply, flags)) {
                return true;
            }
        }
        return super.onTransact(code, data, reply, flags);
    }
}
