// Copyright 2026 Picomisu contributors
// SPDX-License-Identifier: Apache-2.0
package com.pvr.pxrnotification.aidl;

/**
 * Binder interface of the PXR notification service.
 *
 * <p>Checked in as Java instead of AIDL: the factory services.jar carries the pre-Android 10
 * AIDL generator output (no {@code Default} class, no default-implementation hooks and no
 * transaction names), which the AOSP 10 AIDL compiler can no longer produce.
 *
 * @hide
 */
public interface IPxrNotificationService extends android.os.IInterface {
    /** Local-side IPC implementation stub class. */
    public static abstract class Stub extends android.os.Binder
            implements com.pvr.pxrnotification.aidl.IPxrNotificationService {
        private static final java.lang.String DESCRIPTOR =
                "com.pvr.pxrnotification.aidl.IPxrNotificationService";

        /** Construct the stub and attach it to the interface. */
        public Stub() {
            this.attachInterface(this, DESCRIPTOR);
        }

        /**
         * Cast an IBinder object into an IPxrNotificationService interface, generating a proxy
         * if needed.
         */
        public static com.pvr.pxrnotification.aidl.IPxrNotificationService asInterface(
                android.os.IBinder obj) {
            if ((obj == null)) {
                return null;
            }
            android.os.IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
            if (((iin != null)
                    && (iin instanceof com.pvr.pxrnotification.aidl.IPxrNotificationService))) {
                return ((com.pvr.pxrnotification.aidl.IPxrNotificationService) iin);
            }
            return new com.pvr.pxrnotification.aidl.IPxrNotificationService.Stub.Proxy(obj);
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
                case TRANSACTION_addPxrCallback: {
                    data.enforceInterface(descriptor);
                    java.lang.String _arg0;
                    _arg0 = data.readString();
                    int _arg1;
                    _arg1 = data.readInt();
                    com.pvr.pxrnotification.aidl.IPxrNotificationCallback _arg2;
                    _arg2 = com.pvr.pxrnotification.aidl.IPxrNotificationCallback.Stub
                            .asInterface(data.readStrongBinder());
                    this.addPxrCallback(_arg0, _arg1, _arg2);
                    reply.writeNoException();
                    return true;
                }
                case TRANSACTION_sendPxrMessage: {
                    data.enforceInterface(descriptor);
                    java.lang.String _arg0;
                    _arg0 = data.readString();
                    int _arg1;
                    _arg1 = data.readInt();
                    java.lang.String _arg2;
                    _arg2 = data.readString();
                    int _arg3;
                    _arg3 = data.readInt();
                    java.lang.String _arg4;
                    _arg4 = data.readString();
                    this.sendPxrMessage(_arg0, _arg1, _arg2, _arg3, _arg4);
                    reply.writeNoException();
                    return true;
                }
                case TRANSACTION_removePxrCallback: {
                    data.enforceInterface(descriptor);
                    java.lang.String _arg0;
                    _arg0 = data.readString();
                    this.removePxrCallback(_arg0);
                    reply.writeNoException();
                    return true;
                }
                default: {
                    return super.onTransact(code, data, reply, flags);
                }
            }
        }

        private static class Proxy implements com.pvr.pxrnotification.aidl.IPxrNotificationService {
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
            public void addPxrCallback(java.lang.String action, int id,
                    com.pvr.pxrnotification.aidl.IPxrNotificationCallback pcb)
                    throws android.os.RemoteException {
                android.os.Parcel _data = android.os.Parcel.obtain();
                android.os.Parcel _reply = android.os.Parcel.obtain();
                try {
                    _data.writeInterfaceToken(DESCRIPTOR);
                    _data.writeString(action);
                    _data.writeInt(id);
                    _data.writeStrongBinder((((pcb != null)) ? (pcb.asBinder()) : (null)));
                    mRemote.transact(Stub.TRANSACTION_addPxrCallback, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void sendPxrMessage(java.lang.String action, int id, java.lang.String value1,
                    int value2, java.lang.String ext) throws android.os.RemoteException {
                android.os.Parcel _data = android.os.Parcel.obtain();
                android.os.Parcel _reply = android.os.Parcel.obtain();
                try {
                    _data.writeInterfaceToken(DESCRIPTOR);
                    _data.writeString(action);
                    _data.writeInt(id);
                    _data.writeString(value1);
                    _data.writeInt(value2);
                    _data.writeString(ext);
                    mRemote.transact(Stub.TRANSACTION_sendPxrMessage, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void removePxrCallback(java.lang.String action)
                    throws android.os.RemoteException {
                android.os.Parcel _data = android.os.Parcel.obtain();
                android.os.Parcel _reply = android.os.Parcel.obtain();
                try {
                    _data.writeInterfaceToken(DESCRIPTOR);
                    _data.writeString(action);
                    mRemote.transact(Stub.TRANSACTION_removePxrCallback, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }
        }

        static final int TRANSACTION_addPxrCallback =
                (android.os.IBinder.FIRST_CALL_TRANSACTION + 0);
        static final int TRANSACTION_sendPxrMessage =
                (android.os.IBinder.FIRST_CALL_TRANSACTION + 1);
        static final int TRANSACTION_removePxrCallback =
                (android.os.IBinder.FIRST_CALL_TRANSACTION + 2);
    }

    public void addPxrCallback(java.lang.String action, int id,
            com.pvr.pxrnotification.aidl.IPxrNotificationCallback pcb)
            throws android.os.RemoteException;

    public void sendPxrMessage(java.lang.String action, int id, java.lang.String value1,
            int value2, java.lang.String ext) throws android.os.RemoteException;

    public void removePxrCallback(java.lang.String action) throws android.os.RemoteException;
}
