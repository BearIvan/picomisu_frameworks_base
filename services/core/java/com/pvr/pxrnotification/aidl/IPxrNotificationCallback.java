// Copyright 2026 Picomisu contributors
// SPDX-License-Identifier: Apache-2.0
package com.pvr.pxrnotification.aidl;

/**
 * Callback for PXR notification events.
 *
 * <p>Checked in as Java instead of AIDL: the factory services.jar carries the pre-Android 10
 * AIDL generator output (no {@code Default} class, no default-implementation hooks and no
 * transaction names), which the AOSP 10 AIDL compiler can no longer produce.
 *
 * @hide
 */
public interface IPxrNotificationCallback extends android.os.IInterface {
    /** Local-side IPC implementation stub class. */
    public static abstract class Stub extends android.os.Binder
            implements com.pvr.pxrnotification.aidl.IPxrNotificationCallback {
        private static final java.lang.String DESCRIPTOR =
                "com.pvr.pxrnotification.aidl.IPxrNotificationCallback";

        /** Construct the stub and attach it to the interface. */
        public Stub() {
            this.attachInterface(this, DESCRIPTOR);
        }

        /**
         * Cast an IBinder object into an IPxrNotificationCallback interface, generating a proxy
         * if needed.
         */
        public static com.pvr.pxrnotification.aidl.IPxrNotificationCallback asInterface(
                android.os.IBinder obj) {
            if ((obj == null)) {
                return null;
            }
            android.os.IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
            if (((iin != null)
                    && (iin instanceof com.pvr.pxrnotification.aidl.IPxrNotificationCallback))) {
                return ((com.pvr.pxrnotification.aidl.IPxrNotificationCallback) iin);
            }
            return new com.pvr.pxrnotification.aidl.IPxrNotificationCallback.Stub.Proxy(obj);
        }

        @Override
        public android.os.IBinder asBinder() {
            return this;
        }

        @Override
        public boolean onTransact(int code, android.os.Parcel data, android.os.Parcel reply,
                int flags) throws android.os.RemoteException {
            java.lang.String descriptor = DESCRIPTOR;
            switch (code) {
                case INTERFACE_TRANSACTION: {
                    reply.writeString(descriptor);
                    return true;
                }
                case TRANSACTION_onEventChanged: {
                    data.enforceInterface(descriptor);
                    java.lang.String _arg0;
                    _arg0 = data.readString();
                    int _arg1;
                    _arg1 = data.readInt();
                    java.lang.String _arg2;
                    _arg2 = data.readString();
                    this.onEventChanged(_arg0, _arg1, _arg2);
                    return true;
                }
                default: {
                    return super.onTransact(code, data, reply, flags);
                }
            }
        }

        private static class Proxy implements com.pvr.pxrnotification.aidl.IPxrNotificationCallback {
            private android.os.IBinder mRemote;

            Proxy(android.os.IBinder remote) {
                mRemote = remote;
            }

            @Override
            public android.os.IBinder asBinder() {
                return mRemote;
            }

            public java.lang.String getInterfaceDescriptor() {
                return DESCRIPTOR;
            }

            @Override
            public void onEventChanged(java.lang.String value1, int value2, java.lang.String ext)
                    throws android.os.RemoteException {
                android.os.Parcel _data = android.os.Parcel.obtain();
                try {
                    _data.writeInterfaceToken(DESCRIPTOR);
                    _data.writeString(value1);
                    _data.writeInt(value2);
                    _data.writeString(ext);
                    mRemote.transact(Stub.TRANSACTION_onEventChanged, _data, null,
                            android.os.IBinder.FLAG_ONEWAY);
                } finally {
                    _data.recycle();
                }
            }
        }

        static final int TRANSACTION_onEventChanged =
                (android.os.IBinder.FIRST_CALL_TRANSACTION + 0);
    }

    public void onEventChanged(java.lang.String value1, int value2, java.lang.String ext)
            throws android.os.RemoteException;
}
