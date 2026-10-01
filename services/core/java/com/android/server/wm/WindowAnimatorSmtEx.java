// Copyright 2026 Picomisu contributors
// SPDX-License-Identifier: Apache-2.0

package com.android.server.wm;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.SmtSysLog;
import com.android.server.IBoostFrameworkOptEx;
import com.android.server.IMultiPlatSvsFactory;
import com.android.server.SysOptBridge;

/**
 * Reconstructed from the PICO OS 5.13.7 factory services. Nothing in the factory services,
 * sys-services or sysmonitor-services references it; it is carried for parity.
 *
 * @hide
 */
public class WindowAnimatorSmtEx {
    private static final int ANIMATION_BOOST_ENABLE = 1;
    private static final int ANIMATION_BOOST_RELEASE = 2;
    private static final int ANIMATION_BOOST_TIMEINTERVAL = 800;
    private static final int ANIMATION_BOOST_TIMEOUT = 2000;
    private static final String TAG = "WindowAnimatorSmtEx";
    private WindowAnimator mAnimator;
    private BoostThread mBoostThread;
    private static int mHandle = -1;
    private static int animationType = -1;
    private IBoostFrameworkOptEx mBoostFramework = null;
    private boolean isBoost = false;
    private Object mBoostLock = new Object();
    private BHandler bHandler = null;

    WindowAnimatorSmtEx(WindowAnimator windowAnimator) {
        this.mAnimator = windowAnimator;
        initBoost();
    }

    WindowAnimatorSmtEx(WindowAnimator windowAnimator, int type) {
        this.mAnimator = windowAnimator;
        animationType = type;
        initBoost();
    }

    public void scheduleAnimationBoost() {
        initBoost();
        BHandler bHandler = this.bHandler;
        if (bHandler != null && !this.isBoost) {
            bHandler.sendEmptyMessage(1);
        }
    }

    private class BHandler extends Handler {
        public BHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            int i = msg.what;
            if (i != 1) {
                if (i == 2) {
                    synchronized (WindowAnimatorSmtEx.this.mBoostLock) {
                        removeMessages(2);
                        if (WindowAnimatorSmtEx.this.mBoostFramework != null) {
                            WindowAnimatorSmtEx.this.mBoostFramework.disableBoost();
                        }
                        WindowAnimatorSmtEx.this.isBoost = false;
                    }
                    return;
                }
                return;
            }
            synchronized (WindowAnimatorSmtEx.this.mBoostLock) {
                if (WindowAnimatorSmtEx.this.mBoostFramework != null && !WindowAnimatorSmtEx.this.isBoost && WindowAnimatorSmtEx.mHandle != -1) {
                    WindowAnimatorSmtEx.this.isBoost = true;
                    WindowAnimatorSmtEx.this.mBoostFramework.enableBoost(WindowAnimatorSmtEx.ANIMATION_BOOST_TIMEOUT, WindowAnimatorSmtEx.animationType);
                    sendEmptyMessageDelayed(2, 800L);
                }
            }
        }
    }

    private class BoostThread extends Thread {
        private BoostThread() {
        }

        @Override
        public void run() {
            super.run();
            try {
                Looper.prepare();
                bHandler = new BHandler(Looper.myLooper());
                Looper.loop();
            } catch (Exception e) {
                SmtSysLog.fatal(WindowAnimatorSmtEx.TAG, "BoostThread created error!", e);
            }
        }

        public void Interrupt() {
            WindowAnimatorSmtEx.this.bHandler.getLooper().quit();
            super.interrupt();
        }
    }

    private void initBoost() {
        IMultiPlatSvsFactory factory;
        if (this.mBoostThread == null) {
            this.mBoostThread = new BoostThread();
            this.mBoostThread.start();
        }
        if (this.mBoostFramework == null && (factory = SysOptBridge.getMultiPlatFactory()) != null) {
            this.mBoostFramework = factory.getBoostFrameworkByPerf();
        }
        IBoostFrameworkOptEx iBoostFrameworkOptEx = this.mBoostFramework;
        if (iBoostFrameworkOptEx != null) {
            iBoostFrameworkOptEx.initBoost();
            if (mHandle == -1) {
                mHandle = this.mBoostFramework.configBoostParams(animationType);
            }
        }
    }
}
