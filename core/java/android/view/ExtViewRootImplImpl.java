// Copyright 2026 Picomisu contributors
// SPDX-License-Identifier: Apache-2.0
package android.view;

import android.app.ActivityThread;
import android.app.Application;
import android.os.Bundle;
import android.os.Parcel;
import android.pico.ns.NSSurface;
import android.pico.ns.NsClientProxy;
import android.pico.ns.NsConstants;
import android.pico.ns.NsTransactionInterface;
import android.pico.utils.PicoUtils;
import android.provider.Settings;
import android.util.Log;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

/**
 * PICO view-root extension: VR activity skip-draw policy, application resources of 2D
 * displays, and the native shell ("NS") client of the left/right input-method windows,
 * whose surface and input come from the NS service.
 * @hide
 */
public class ExtViewRootImplImpl implements IExtViewRootImpl {
    private static final String TAG = "ViewRootImplExt";
    private static final String PACKAGE_PERMISSION_CTRL = "com.android.permissioncontroller";
    private static final String SETTINGS_VR_ACTIVITY_SKIP_RENDER_ENABLED =
            "vr_activity_skip_render_enabled";

    private ViewRootImpl mBase;
    private boolean mCanSkipDraw = false;
    private boolean mIsCheckedSkipDraw = false;
    private NsClientProxy mProxy;
    private int mClientId;
    private int mNsType;

    private NsClientProxy.NsCallback mCallback = new NsClientProxy.NsCallback() {
        @Override
        public boolean onCallback(int code, Parcel data, Parcel reply) {
            if (code != NsTransactionInterface.CODE_NS_DISPATCH_EVENT) {
                return false;
            }
            int type = data.readInt();
            if (type == 1) {
                MotionEvent event = MotionEvent.CREATOR.createFromParcel(data);
                dispatchMotionEvent(event);
            } else if (type == 2) {
                KeyEvent event = KeyEvent.CREATOR.createFromParcel(data);
                dispatchKeyEvent(event);
            }
            return true;
        }
    };

    public ExtViewRootImplImpl(ViewRootImpl base) {
        mBase = base;
    }

    /**
     * Evaluated once per view root: the global setting (default 1) must be 1 and the
     * matching VR activity must not force rendering.
     */
    private boolean canSkipDraw() {
        if (mIsCheckedSkipDraw) {
            return mCanSkipDraw;
        }
        mIsCheckedSkipDraw = true;
        if (Settings.Global.getInt(mBase.mContext.getContentResolver(),
                SETTINGS_VR_ACTIVITY_SKIP_RENDER_ENABLED, 1) == 1) {
            // Without an ActivityThread no activity can match, which means force rendering
            // (mCanSkipDraw stays false); the factory dereferences it unconditionally.
            ActivityThread thread = ActivityThread.currentActivityThread();
            if (thread != null) {
                mCanSkipDraw = !thread.getExt().isActivityForceRender(mBase);
            }
        }
        return mCanSkipDraw;
    }

    /**
     * Whether software drawing should use the PICO 1x1 VR canvas instead of the window
     * buffer. Permission-controller windows and displays other than 0 always draw.
     */
    @Override
    public boolean isSkipDrawVrActivity() {
        if (mBase.getTitle().toString().contains(PACKAGE_PERMISSION_CTRL)) {
            return false;
        }
        return canSkipDraw() && mBase.mDisplay.getDisplayId() == 0;
    }

    /**
     * Updates the application resources for the view-root display and, for a non-system
     * application first shown on a 2D virtual display, moves the application context to it.
     */
    @Override
    public void adjustApplicationContextResources() {
        PicoUtils.updateApplicationContextResources(mBase, mBase.mDisplay);
        if (mBase.mDisplay != null && mBase.mDisplay.getExt().isVr2dDisplay()) {
            Application app = ActivityThread.currentApplication();
            if (app != null && app.getDisplayId() <= 0
                    && !PicoUtils.isSystemApp(mBase.mContext.getApplicationInfo())) {
                app.updateDisplay(mBase.mDisplay.getDisplayId());
            }
        }
    }

    /**
     * Makes the left/right input-method windows NS clients: creates the client and replaces
     * the view-root surface with its {@link NSSurface}.
     */
    @Override
    public void onSetView(View view, WindowManager.LayoutParams attrs) {
        if (view == null || attrs == null) {
            return;
        }
        int windowType = getWindowType(attrs);
        if (windowType > 0) {
            mNsType = windowType;
            mProxy = new NsClientProxy();
            mProxy.setCallback(mCallback);
            String name = "NS_" + attrs.getTitle() + "_" + mBase.mBasePackageName;
            Bundle params = mProxy.generateParameter(0, 0, mNsType, name,
                    view.getContext().getPackageName(), false);
            mClientId = mProxy.createClient(params);
            Log.w(TAG, "onSetView [" + attrs.getTitle() + "]");
            replaceSurface();
        }
    }

    /** Hides and destroys the NS client of the window. */
    @Override
    public void onDoDie() {
        if (mClientId > 0) {
            Log.w(TAG, "onDoDie");
            int clientId = mClientId;
            mClientId = 0;
            mProxy.updateVisibility(clientId, false);
            mProxy.destroyClient(clientId);
        }
    }

    /** NS client type of a window, or -1 for windows that are not NS clients. */
    private int getWindowType(WindowManager.LayoutParams attrs) {
        if (attrs == null) {
            return -1;
        }
        // PICO input-method panel window type.
        if (attrs.type != 2998) {
            return -1;
        }
        int type = -1;
        if ("input_method_left".equals(attrs.getTitle())) {
            type = NsConstants.TYPE_KEYBOARD_LEFT;
        } else if ("input_method_right".equals(attrs.getTitle())) {
            type = NsConstants.TYPE_KEYBOARD_RIGHT;
        }
        return type;
    }

    /** Replaces the final ViewRootImpl.mSurface with an {@link NSSurface}. */
    private boolean replaceSurface() {
        try {
            NSSurface surface = new NSSurface(mBase, mProxy, mClientId);
            Field f = mBase.getClass().getDeclaredField("mSurface");
            Field modifiersField = Field.class.getDeclaredField("accessFlags");
            modifiersField.setAccessible(true);
            modifiersField.setInt(f, f.getModifiers() & ~Modifier.FINAL);
            f.setAccessible(true);
            f.set(mBase, surface);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    private void dispatchMotionEvent(final MotionEvent event) {
        mBase.mHandler.post(new Runnable() {
            @Override
            public void run() {
                if (mBase.mView == null) {
                    return;
                }
                mBase.mView.dispatchPointerEvent(event);
            }
        });
    }

    private void dispatchKeyEvent(final KeyEvent event) {
        mBase.mHandler.post(new Runnable() {
            @Override
            public void run() {
                if (mBase.mView == null) {
                    return;
                }
                mBase.mView.dispatchKeyEvent(event);
            }
        });
    }
}
