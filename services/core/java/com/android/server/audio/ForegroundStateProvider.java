// Copyright 2026 Picomisu contributors
// SPDX-License-Identifier: Apache-2.0

package com.android.server.audio;

import android.os.Bundle;
import android.os.IBinder;
import android.os.Parcel;
import android.os.RemoteCallback;
import android.os.ServiceManager;

import java.util.ArrayList;

/**
 * Foreground (visible) packages and see-through state, delivered by the PICO window manager
 * through a RemoteCallback registered with the one-way IWindowManager transaction 10004. As in
 * the PICO OS 5.13.7 factory services.
 */
public class ForegroundStateProvider {
    private static final String TAG = "ForegroundStateProvider";
    private static final String kForegroundListKey = "visible_app_list";
    private static final int kRegisterTransCode = 10004;
    private static final String kRuntimeDisplayState = "xr_runtime_display_state";
    private static final String kSeeThroughKey = "seethrough_status";
    private static final String kSeeThroughSettingPkg = "com.pvr.seethrough.setting";
    private static final int kSeeThroughShow = 2;
    private static final int kXRDisplayStateLoading = 0;

    private RemoteCallback mRemoteCallback = new RemoteCallback(
            new RemoteCallback.OnResultListener() {
        @Override
        public void onResult(Bundle result) {
            if (result == null) {
                return;
            }
            boolean seeThroughShow = false;
            if (result.containsKey(kForegroundListKey)) {
                ArrayList<String> visibleAppList = result.getStringArrayList(kForegroundListKey);
                if (visibleAppList != null) {
                    mStateListener.onForegroundPkgsChanged(visibleAppList);
                }
                // As in the factory, a null list is not guarded here.
                seeThroughShow = visibleAppList.contains(kSeeThroughSettingPkg);
                if (seeThroughShow) {
                    mStateListener.onSeeThroughStateChanged(true);
                }
            }
            if (!seeThroughShow && result.containsKey(kRuntimeDisplayState)) {
                int runtimeState = result.getInt(kRuntimeDisplayState, -1);
                if (runtimeState == kXRDisplayStateLoading) {
                    mStateListener.onSeeThroughStateChanged(true);
                    seeThroughShow = true;
                }
            }
            if (!seeThroughShow && result.containsKey(kSeeThroughKey)) {
                int seeThrough = result.getInt(kSeeThroughKey, -1);
                mStateListener.onSeeThroughStateChanged(seeThrough == kSeeThroughShow);
            }
        }
    });
    private ForegroundStateListener mStateListener;

    interface ForegroundStateListener {
        void onForegroundPkgsChanged(ArrayList<String> pkgs);

        void onSeeThroughStateChanged(boolean show);
    }

    ForegroundStateProvider(ForegroundStateListener listener) {
        mStateListener = listener;
        registerForegroundStateCallback(mRemoteCallback);
    }

    private void registerForegroundStateCallback(RemoteCallback callback) {
        IBinder wm = ServiceManager.getService("window");
        Parcel data = Parcel.obtain();
        Parcel reply = Parcel.obtain();
        try {
            data.writeInterfaceToken("android.view.IWindowManager");
            data.writeInt(1);
            callback.writeToParcel(data, 0);
            wm.transact(kRegisterTransCode, data, reply, IBinder.FLAG_ONEWAY);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            data.recycle();
            reply.recycle();
        }
    }
}
