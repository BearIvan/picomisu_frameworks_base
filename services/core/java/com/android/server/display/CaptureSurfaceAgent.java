// Copyright 2026 Picomisu contributors
// SPDX-License-Identifier: Apache-2.0
package com.android.server.display;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.util.Slog;
import android.view.Surface;

import com.pxr.pxrapi.IScreenCaptureInterface;

/**
 * PICO capture surface agent (factory PICO OS 5.13.7 com.android.server.display.CaptureSurfaceAgent).
 *
 * <p>A check agent (DisplayManagerService, PHASE_THIRD_PARTY_APPS_CAN_START) binds once to
 * com.pico.xr.openxr_runtime/com.pxr.pxrapi.ScreenCaptureService and records whether the new
 * capture path is available (unless persist.pxr.new_capture.disable is set). A surface agent
 * (one per capture virtual display) binds to that service on its own looper thread and passes
 * the display surface, capture type and size to IScreenCaptureInterface.startCapture; stopCapture
 * unbinds and ends the thread.
 */
public class CaptureSurfaceAgent {
    private static final int AGENT_TYPE_CHECK = 1;
    private static final int AGENT_TYPE_SURFACE = 2;
    private static final String CAPTURE_ACTION = "android.intent.action.SCREEN_CAPTURE";
    private static final String CAPTURE_SERVICE = "com.pxr.pxrapi.ScreenCaptureService";
    private static final int MSG_CHECK_CAPTURE = 3;
    private static final int MSG_START_CAPTURE = 1;
    private static final int MSG_STOP_CAPTURE = 2;
    private static final String NEW_CAPTURE_ENABLE = "1";
    private static final String PROP_DISABLE_NEW_CAPTURE = "persist.pxr.new_capture.disable";
    private static final String PROP_NEW_CAPTURE_RUNTIME = "pico.pxr.new_capture.enable";
    private static final String RUNTIME_PACKAGE = "com.pico.xr.openxr_runtime";
    public static final String TAG = "CaptureSurfaceAgent";
    public static boolean mNewCaptureStatus = false;
    private int mAgentType;
    private int mCaptureType;
    private ServiceConnection mConn;
    private Context mContext;
    private CaptureSurfaceHandler mHandler;
    private HandlerThread mHandlerThread;
    private int mHeight;
    private boolean mIsConnected;
    private IScreenCaptureInterface mService;
    private Surface mSurface;
    private int mWidth;

    public CaptureSurfaceAgent(Context context, Surface surface, int type, int width, int height) {
        mIsConnected = false;
        mContext = context;
        mSurface = surface;
        mCaptureType = type;
        mWidth = width;
        mHeight = height;
        mAgentType = AGENT_TYPE_SURFACE;
        mHandlerThread = new HandlerThread("CaptureSurfaceLooper");
        mHandlerThread.start();
        mHandler = new CaptureSurfaceHandler(mHandlerThread.getLooper());
    }

    public CaptureSurfaceAgent(Context context) {
        mIsConnected = false;
        mContext = context;
        mAgentType = AGENT_TYPE_CHECK;
        mHandlerThread = new HandlerThread("CheckCaptureSurfaceLooper");
        mHandlerThread.start();
        mHandler = new CaptureSurfaceHandler(mHandlerThread.getLooper());
    }

    public void startCapture() {
        if (mAgentType == AGENT_TYPE_SURFACE) {
            mHandler.dispatchStartCapture();
        }
    }

    public void stopCapture() {
        if (mAgentType == AGENT_TYPE_SURFACE) {
            mHandler.dispatchStopCapture();
        }
    }

    public void checkCapture() {
        if (mAgentType == AGENT_TYPE_CHECK) {
            mHandler.dispatchCheckCapture();
        }
    }

    public static boolean isNewCaptureEnable() {
        return mNewCaptureStatus;
    }

    private class CaptureSurfaceHandler extends Handler {
        public CaptureSurfaceHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            try {
                switch (msg.what) {
                    case MSG_START_CAPTURE:
                        startCaptureImpl();
                        break;
                    case MSG_STOP_CAPTURE:
                        stopCaptureImpl();
                        getLooper().quitSafely();
                        break;
                    case MSG_CHECK_CAPTURE:
                        checkCaptureImpl();
                        getLooper().quitSafely();
                        break;
                }
            } catch (Exception e) {
                Slog.e(TAG, "CaptureSurfaceHandler error: " + msg.what);
            }
        }

        public void dispatchStartCapture() {
            sendEmptyMessage(MSG_START_CAPTURE);
        }

        public void dispatchStopCapture() {
            sendEmptyMessage(MSG_STOP_CAPTURE);
        }

        public void dispatchCheckCapture() {
            sendEmptyMessage(MSG_CHECK_CAPTURE);
        }
    }

    private void startCaptureImpl() {
        try {
            Intent intent = new Intent();
            intent.setComponent(new ComponentName(RUNTIME_PACKAGE, CAPTURE_SERVICE));
            intent.setAction(CAPTURE_ACTION);
            mConn = new ServiceConnection() {
                @Override
                public void onServiceConnected(ComponentName name, IBinder service) {
                    try {
                        mService = IScreenCaptureInterface.Stub.asInterface(service);
                        int ret = mService.startCapture(mSurface, mCaptureType, mWidth, mHeight);
                        if (ret == 0) {
                            mIsConnected = true;
                        } else {
                            Slog.e(TAG, "startCapture return error: " + ret);
                        }
                    } catch (Exception e) {
                        Slog.e(TAG, "error: startCapture exception: " + e);
                    }
                }

                @Override
                public void onServiceDisconnected(ComponentName name) {
                    mService = null;
                    mIsConnected = false;
                }
            };
            boolean ret = mContext.bindServiceAsUser(intent, mConn, Context.BIND_AUTO_CREATE,
                    UserHandle.CURRENT);
            if (!ret) {
                mNewCaptureStatus = false;
                mContext.unbindService(mConn);
                Slog.i(TAG, "startCapture bindService failed, disable new capture...");
            }
        } catch (Exception e) {
            Slog.e(TAG, "error: startCapture exception: " + e);
        }
    }

    private void stopCaptureImpl() {
        try {
            try {
                if (mService == null || !mIsConnected) {
                    Slog.e(TAG, "error: stopCapture not connected...");
                } else {
                    mService.stopCapture();
                }
            } catch (Exception e) {
                Slog.e(TAG, "error: stopCapture exception: " + e);
            }
        } finally {
            mContext.unbindService(mConn);
            mService = null;
            mIsConnected = false;
        }
    }

    private void checkCaptureImpl() {
        if (isNewCaptureDisable()) {
            Slog.i(TAG, "new chapture is disabled...");
            mNewCaptureStatus = false;
            return;
        }
        try {
            Intent intent = new Intent();
            intent.setComponent(new ComponentName(RUNTIME_PACKAGE, CAPTURE_SERVICE));
            intent.setAction(CAPTURE_ACTION);
            mConn = new ServiceConnection() {
                @Override
                public void onServiceConnected(ComponentName name, IBinder service) {
                }

                @Override
                public void onServiceDisconnected(ComponentName name) {
                }
            };
            SystemProperties.set(PROP_NEW_CAPTURE_RUNTIME, NEW_CAPTURE_ENABLE);
            mNewCaptureStatus = mContext.bindServiceAsUser(intent, mConn,
                    Context.BIND_AUTO_CREATE, UserHandle.CURRENT);
            Slog.i(TAG, "checkCapture enable: " + mNewCaptureStatus);
            mContext.unbindService(mConn);
        } catch (Exception e) {
            Slog.e(TAG, "error: checkCapture exception: " + e);
        }
    }

    private boolean isNewCaptureDisable() {
        return SystemProperties.getBoolean(PROP_DISABLE_NEW_CAPTURE, false);
    }

    @Override
    protected void finalize() throws Throwable {
        try {
            if (mHandlerThread != null) {
                mHandlerThread.quitSafely();
            }
        } finally {
            mHandlerThread = null;
            super.finalize();
        }
    }
}
