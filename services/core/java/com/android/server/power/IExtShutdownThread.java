// Copyright 2026 Picomisu contributors
// SPDX-License-Identifier: Apache-2.0
package com.android.server.power;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.util.Log;
import android.view.Window;
import android.view.WindowManager;

import com.android.server.SysDataSyncServiceManager;
import com.pico.util.IExtBase;

/**
 * PICO shutdown helpers of {@link ShutdownThread} (factory PICO OS 5.13.7
 * com.android.server.power.IExtShutdownThread, static methods only).
 * @hide
 */
public interface IExtShutdownThread extends IExtBase {
    /**
     * beginShutdownSequence: shows a black full-screen non-cancelable system dialog and starts
     * bootanim with sys.animation.status=shutdown, which makes it play the shutdown animation.
     */
    static void showEmptyDialogForAnimation(Context context) {
        Dialog d = new Dialog(context);
        Window window = d.getWindow();
        window.requestFeature(Window.FEATURE_NO_TITLE);
        // HIDE_NAVIGATION | FULLSCREEN | LAYOUT_STABLE | LAYOUT_HIDE_NAVIGATION
        // | LAYOUT_FULLSCREEN | IMMERSIVE_STICKY
        window.getAttributes().systemUiVisibility |= 3846;
        window.getDecorView();
        window.getAttributes().width = WindowManager.LayoutParams.MATCH_PARENT;
        window.getAttributes().height = WindowManager.LayoutParams.MATCH_PARENT;
        window.getAttributes().layoutInDisplayCutoutMode =
                WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES;
        window.getAttributes().screenOrientation = 0;
        window.setType(WindowManager.LayoutParams.TYPE_VOLUME_OVERLAY);
        window.clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
        window.addFlags(17629472);
        window.setBackgroundDrawableResource(android.R.color.black);
        d.setCancelable(false);
        d.show();
        Log.w("ShutdownThread", "setprop sys.animation.status,ready shutdown anim");
        SystemProperties.set("sys.animation.status", "shutdown");
        SystemProperties.set("ctl.start", "bootanim");
    }

    /**
     * beginShutdownSequence: flushes the TeaTracker events, sends the ordered
     * android.intent.action.PVR_ACTION_SHUTDOWN broadcast and starts the shutdown thread after
     * persist.pxr.shutdown.waittime seconds. Returns true: the caller must not start it.
     */
    static boolean adjustInstantStart(Context context, final ShutdownThread sInstance,
            Handler handler) {
        Log.i("ShutdownThread", "flush and upload teatracker events");
        SysDataSyncServiceManager.flushTeaTrackerEvents();
        int shutdownWaitTime = SystemProperties.getInt("persist.pxr.shutdown.waittime", 0);
        Log.i("ShutdownThread", "ready to shutdown shutdownWaitTime:" + shutdownWaitTime);
        Intent intent = new Intent("android.intent.action.PVR_ACTION_SHUTDOWN");
        intent.addFlags(Intent.FLAG_RECEIVER_FOREGROUND | Intent.FLAG_RECEIVER_REGISTERED_ONLY);
        context.sendOrderedBroadcastAsUser(intent, UserHandle.ALL, null, null, handler, 0, null,
                null);
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                sInstance.start();
            }
        }, shutdownWaitTime * 1000);
        return true;
    }

    /** True: the AOSP shutdown progress dialog is created but not shown. */
    static boolean showShutDownAnim() {
        return true;
    }
}
