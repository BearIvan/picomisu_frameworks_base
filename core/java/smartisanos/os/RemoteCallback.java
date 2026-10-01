// Copyright 2026 Picomisu contributors
// SPDX-License-Identifier: Apache-2.0

package smartisanos.os;

import android.os.Bundle;
import android.os.Handler;
import android.os.IRemoteCallback;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.RemoteException;

/**
 * Smartisan one-shot remote result callback. Reconstructed from the PICO OS 5.13.7 factory
 * framework.
 *
 * @hide
 */
public final class RemoteCallback implements Parcelable {

    /** @hide */
    public interface OnResultListener {
        void onResult(Bundle result);
    }

    private final OnResultListener mListener;
    private final Handler mHandler;
    private final IRemoteCallback mCallback;

    public RemoteCallback(OnResultListener listener) {
        this(listener, null);
    }

    public RemoteCallback(OnResultListener listener, Handler handler) {
        if (listener == null) {
            throw new NullPointerException("listener cannot be null");
        }
        mListener = listener;
        mHandler = handler;
        mCallback = new IRemoteCallback.Stub() {
            @Override
            public void sendResult(Bundle data) {
                RemoteCallback.this.sendResult(data);
            }
        };
    }

    RemoteCallback(Parcel parcel) {
        mListener = null;
        mHandler = null;
        mCallback = IRemoteCallback.Stub.asInterface(parcel.readStrongBinder());
    }

    public void sendResult(final Bundle result) {
        if (mListener != null) {
            if (mHandler != null) {
                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        mListener.onResult(result);
                    }
                });
            } else {
                mListener.onResult(result);
            }
        } else {
            try {
                mCallback.sendResult(result);
            } catch (RemoteException e) {
                /* ignore */
            }
        }
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int flags) {
        parcel.writeStrongBinder(mCallback.asBinder());
    }

    public static final Parcelable.Creator<RemoteCallback> CREATOR =
            new Parcelable.Creator<RemoteCallback>() {
        @Override
        public RemoteCallback createFromParcel(Parcel parcel) {
            return new RemoteCallback(parcel);
        }

        @Override
        public RemoteCallback[] newArray(int size) {
            return new RemoteCallback[size];
        }
    };
}
