// Copyright 2026 Picomisu contributors
// SPDX-License-Identifier: Apache-2.0
package com.pico.api.os;

import android.os.Bundle;
import android.os.Handler;
import android.os.RemoteCallback;

/**
 * Wraps a {@link RemoteCallback} behind an opaque {@link Object}, so that callers outside the
 * framework can hand a {@link ResultListener} across binder without linking against the hidden
 * {@link RemoteCallback} type.
 *
 * @hide
 */
public class RemoteCallbackProxy {

    class OnResultListenerProxy implements RemoteCallback.OnResultListener {
        private ResultListener mListener = null;

        public OnResultListenerProxy(ResultListener listener) {
            mListener = listener;
        }

        @Override
        public void onResult(Bundle result) {
            if (mListener != null) {
                mListener.onResult(result);
            }
        }
    }

    private RemoteCallback mRemoteCallback = null;

    public RemoteCallbackProxy(ResultListener listener, Handler handler) {
        if (listener != null) {
            mRemoteCallback = new RemoteCallback(new OnResultListenerProxy(listener), handler);
        }
    }

    /** Returns the wrapped {@link RemoteCallback}, or {@code null} when no listener was given. */
    public Object getRemoteCallback() {
        return mRemoteCallback;
    }
}
