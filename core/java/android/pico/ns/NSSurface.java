// Copyright 2026 Picomisu contributors
// SPDX-License-Identifier: Apache-2.0
package android.pico.ns;

import android.graphics.Rect;
import android.graphics.SurfaceTexture;
import android.util.Log;
import android.view.Surface;
import android.view.SurfaceControl;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * Window surface backed by the PICO native shell ("NS") service. It replaces the
 * ViewRootImpl surface of NS client windows: when the window manager hands the view root a
 * new surface frame, the NS service is asked to create (or resize) the client surface,
 * which is read into this Surface.
 * @hide
 */
public class NSSurface extends Surface {
    private static final String TAG = "NsClient";
    private static final boolean DEBUG = true;

    private static boolean sReflectionEnable;

    private Object mViewRootImpl;
    private NsClientProxy mProxy;
    private int mClientId;
    private int mWidth;
    private int mHeight;
    private Rect mCurrentFrame = new Rect();
    private boolean mHasSurface = false;

    /**
     * @param viewRootImpl the view root whose frame ({@code mTmpFrame}) sizes the surface
     */
    public NSSurface(Object viewRootImpl, NsClientProxy proxy, int clientId) {
        super(new SurfaceTexture(0));
        // Only the NS surface read in acquireSurface backs this Surface.
        super.release();
        enableReflection();
        mViewRootImpl = viewRootImpl;
        mProxy = proxy;
        mClientId = clientId;
        Log.w(TAG, "NSSurface init [" + mClientId + "] [" + Integer.toHexString(hashCode())
                + "]");
    }

    /**
     * Called by ViewRootImpl with the window-manager surface: sizes the NS surface after the
     * current view-root frame instead of copying {@code other}.
     */
    @Override
    public void copyFrom(SurfaceControl other) {
        mCurrentFrame.set(getFrame());
        int width = mCurrentFrame.width();
        int height = mCurrentFrame.height();
        Log.w(TAG, "NsSurface copyFrom, " + mCurrentFrame);
        if (width <= 0 || height <= 0) {
            return;
        }
        acquireSurface(width, height);
    }

    /** Acquires (first time) or resizes the NS surface and makes the client visible. */
    public void acquireSurface(int width, int height) {
        if (width <= 0 || height <= 0) {
            return;
        }
        if (mWidth != width || mHeight != height) {
            mWidth = width;
            mHeight = height;
            if (!mHasSurface) {
                mHasSurface = true;
                Log.w(TAG, "acquireSurface [" + mClientId + "] frame [" + mCurrentFrame + "] ["
                        + Integer.toHexString(hashCode()) + "]");
                mProxy.acquireSurface(mClientId, mWidth, mHeight, this);
            } else {
                Log.w(TAG, "surface [" + mClientId + "] resize to [" + mCurrentFrame + "] ["
                        + Integer.toHexString(hashCode()) + "]");
                mProxy.resizeSurface(mClientId, mWidth, mHeight);
            }
            mProxy.updateVisibility(mClientId, true);
        }
    }

    @Override
    public void release() {
        super.release();
        mWidth = 0;
        mHeight = 0;
        mCurrentFrame.setEmpty();
        if (mProxy != null && mClientId > 0) {
            Log.w(TAG, "NSSurface release [" + mClientId + "][" + Integer.toHexString(hashCode())
                    + "]");
            mHasSurface = false;
            mProxy.updateVisibility(mClientId, false);
            mProxy.releaseSurface(mClientId);
        }
    }

    private Rect getFrame() {
        try {
            Class c = Class.forName("android.view.ViewRootImpl");
            Field f = c.getDeclaredField("mTmpFrame");
            f.setAccessible(true);
            return (Rect) f.get(mViewRootImpl);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    /** Exempts every API from the hidden-API checks of this process, once. */
    public static void enableReflection() {
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
}
