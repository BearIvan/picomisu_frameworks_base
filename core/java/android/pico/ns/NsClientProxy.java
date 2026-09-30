// Copyright 2026 Picomisu contributors
// SPDX-License-Identifier: Apache-2.0
package android.pico.ns;

import android.graphics.Rect;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Looper;
import android.os.Parcel;
import android.util.Log;

import java.lang.reflect.Method;

/**
 * Client of the PICO native shell ("NS") window service, the "native_shell" binder. It
 * creates NS clients for windows, drives their surfaces and visibility, and forwards the
 * service callbacks to an {@link NsCallback}. The service is looked up again (up to ten
 * times, 100 ms apart) while it is unavailable or after it dies.
 * @hide
 */
public class NsClientProxy {
    private static final String TAG = "NsClient";
    private static final boolean DEBUG = false;
    private static final String DESCRIPTOR = "com.bytedance.IRemoteCallback";
    public static final int PARAM_PARENT_ID_NONE = -1;
    public static final int PARAM_SHAPE_NONE = 0;

    private static boolean sReflectionEnable = false;

    private IBinder mNS = null;
    private int mRetryCount = 0;
    private final int mMaxRetryCount = 10;
    private Handler mHandler = new Handler(Looper.getMainLooper());
    private NsCallback mCallback;

    private Runnable mInitNsService = new Runnable() {
        @Override
        public void run() {
            mRetryCount++;
            initNs();
        }
    };

    private IBinder.DeathRecipient mNsDeathRecipient = new IBinder.DeathRecipient() {
        @Override
        public void binderDied() {
            try {
                if (mNS != null) {
                    mNS.unlinkToDeath(this, 0);
                    mNS = null;
                }
                retryInitService();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    };

    public NsClientProxy() {
        initNs();
    }

    public void setCallback(NsCallback callback) {
        mCallback = callback;
    }

    public boolean isAvailable() {
        return mNS != null && mNS.isBinderAlive();
    }

    /** Parameters of {@link #createClient}. */
    public Bundle generateParameter(int width, int height, int type, String name, String pkg,
            boolean composeWithDisplay) {
        Bundle params = new Bundle();
        params.putInt(NsConstants.LAYOUT_SIZE_W, width);
        params.putInt(NsConstants.LAYOUT_SIZE_H, height);
        params.putInt(NsConstants.PARAM_TYPE, type);
        params.putString(NsConstants.PARAM_NAME, name);
        params.putBinder(NsConstants.PARAM_CALLBACK, new ClientBinder());
        params.putString(NsConstants.PARAM_PACKAGE_NAME, pkg);
        params.putBoolean(NsConstants.PARAM_COMPOSE_WITH_DISPLAY, composeWithDisplay);
        return params;
    }

    /** Returns the new client ID, or -1 when the service call fails. */
    public int createClient(Bundle params) {
        int clientId = -1;
        Parcel data = Parcel.obtain();
        Parcel reply = Parcel.obtain();
        try {
            data.writeInterfaceToken(DESCRIPTOR);
            params.writeToParcel(data, 0);
            mNS.transact(NsTransactionInterface.CODE_CLIENT_CREATE, data, reply, 0);
            clientId = reply.readInt();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            data.recycle();
            reply.recycle();
        }
        return clientId;
    }

    public void destroyClient(int clientId) {
        Parcel data = Parcel.obtain();
        Parcel reply = Parcel.obtain();
        try {
            data.writeInterfaceToken(DESCRIPTOR);
            data.writeInt(-1);
            data.writeInt(clientId);
            mNS.transact(NsTransactionInterface.CODE_CLIENT_DESTROY, data, reply,
                    IBinder.FLAG_ONEWAY);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            data.recycle();
            reply.recycle();
        }
    }

    public void resizeSurface(int clientId, int width, int height) {
        Parcel data = Parcel.obtain();
        Parcel reply = Parcel.obtain();
        try {
            data.writeInterfaceToken(DESCRIPTOR);
            data.writeInt(clientId);
            data.writeInt(width);
            data.writeInt(height);
            mNS.transact(NsTransactionInterface.CODE_CLIENT_RESIZE, data, reply,
                    IBinder.FLAG_ONEWAY);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            data.recycle();
            reply.recycle();
        }
    }

    public void updateVisibility(int clientId, boolean visible) {
        Parcel data = Parcel.obtain();
        Parcel reply = Parcel.obtain();
        try {
            data.writeInterfaceToken(DESCRIPTOR);
            data.writeInt(-1);
            data.writeBoolean(visible);
            data.writeInt(clientId);
            mNS.transact(NsTransactionInterface.CODE_CLIENT_VISIBLE, data, reply,
                    IBinder.FLAG_ONEWAY);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            data.recycle();
            reply.recycle();
        }
    }

    public void updateTouchRegion(int clientId, Rect rect) {
        Parcel data = Parcel.obtain();
        Parcel reply = Parcel.obtain();
        try {
            data.writeInterfaceToken(DESCRIPTOR);
            data.writeInt(clientId);
            if (rect != null) {
                data.writeInt(1);
                rect.writeToParcel(data, 0);
            } else {
                data.writeInt(0);
            }
            mNS.transact(NsTransactionInterface.CODE_CLIENT_UPDATE_TOUCH_REGION, data, reply,
                    IBinder.FLAG_ONEWAY);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            data.recycle();
            reply.recycle();
        }
    }

    /** Fills {@code surface} from the reply when the service returns one. */
    public void acquireSurface(int clientId, int width, int height, NSSurface surface) {
        Parcel data = Parcel.obtain();
        Parcel reply = Parcel.obtain();
        try {
            data.writeInterfaceToken(DESCRIPTOR);
            data.writeInt(clientId);
            data.writeInt(width);
            data.writeInt(height);
            mNS.transact(NsTransactionInterface.CODE_CLIENT_ACQUIRE_SURFACE, data, reply, 0);
            if (reply.readInt() > 0) {
                surface.readFromParcel(reply);
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            data.recycle();
            reply.recycle();
        }
    }

    public void releaseSurface(int clientId) {
        Parcel data = Parcel.obtain();
        Parcel reply = Parcel.obtain();
        try {
            data.writeInterfaceToken(DESCRIPTOR);
            data.writeInt(clientId);
            mNS.transact(NsTransactionInterface.CODE_CLIENT_RELEASE_SURFACE, data, reply,
                    IBinder.FLAG_ONEWAY);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            data.recycle();
            reply.recycle();
        }
    }

    /** Looks up the "native_shell" service through reflection on ServiceManager. */
    protected IBinder getNS() {
        enableReflection();
        try {
            Class c = Class.forName("android.os.ServiceManager");
            Method method = c.getDeclaredMethod("getService", String.class);
            return (IBinder) method.invoke(null, "native_shell");
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private void initNs() {
        mNS = getNS();
        if (!isAvailable()) {
            mNS = null;
            retryInitService();
            return;
        }
        mRetryCount = 0;
        try {
            mNS.linkToDeath(mNsDeathRecipient, 0);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void retryInitService() {
        if (mRetryCount > mMaxRetryCount) {
            Log.w(TAG, "retryInitService abandon, retry too many times [" + mRetryCount + "]");
            return;
        }
        mHandler.removeCallbacks(mInitNsService);
        mHandler.postDelayed(mInitNsService, 100);
    }

    /** Exempts every API from the hidden-API checks of this process, once. */
    private static void enableReflection() {
        if (sReflectionEnable) {
            return;
        }
        try {
            Method forNameMethod = Class.class.getDeclaredMethod("forName", String.class);
            Method getDeclaredMethod = Class.class.getDeclaredMethod("getDeclaredMethod",
                    String.class, Class[].class);
            Class<?> runtimeClass = (Class<?>) forNameMethod.invoke(null,
                    "dalvik.system.VMRuntime");
            Method getRuntimeMethod = (Method) getDeclaredMethod.invoke(runtimeClass,
                    "getRuntime", null);
            Method setHiddenApiExemptions = (Method) getDeclaredMethod.invoke(runtimeClass,
                    "setHiddenApiExemptions", new Class[] {String[].class});
            Object runtime = getRuntimeMethod.invoke(null);
            setHiddenApiExemptions.invoke(runtime, new Object[] {new String[] {"L"}});
            sReflectionEnable = true;
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /** Receiver of the NS service callbacks of a client. */
    public interface NsCallback {
        boolean onCallback(int code, Parcel data, Parcel reply);
    }

    /** Callback binder passed to the service with {@link #generateParameter}. */
    private class ClientBinder extends Binder implements IInterface {
        private static final String DESCRIPTOR = "com.bytedance.IRemoteCallback";

        public ClientBinder() {
            attachInterface(this, DESCRIPTOR);
        }

        @Override
        public IBinder asBinder() {
            return this;
        }

        @Override
        protected boolean onTransact(int code, Parcel data, Parcel reply, int flags)
                throws android.os.RemoteException {
            if (mCallback != null) {
                data.enforceInterface(DESCRIPTOR);
                if (mCallback.onCallback(code, data, reply)) {
                    return true;
                }
            }
            return super.onTransact(code, data, reply, flags);
        }
    }
}
