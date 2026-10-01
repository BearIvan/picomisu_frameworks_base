/*
 * Copyright (C) 2026 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package android.view;

import android.os.IBinder;
import android.os.Message;
import android.os.Process;
import android.os.ServiceManager;
import android.os.SystemClock;
import android.os.SystemProperties;

import com.android.internal.app.ISysTransServer;

/**
 * Smartisan view-root extension base of the factory PICO OS 5.13.7 framework: with bit 2 of
 * persist.sys.monitor, input events are recorded on the window surface and the first touch
 * of a gesture asks systransserver for the touch refresh rate (at most every 2 s).
 *
 * The factory framework has no subclass of this class and no caller of its members, and its
 * libandroid_runtime does not register {@link Surface#setLastInputTime}'s native method; it is
 * ported as it is.
 *
 * @hide
 */
public abstract class ViewRootImplSmtBase {
    protected static final int CHANGE_RATE_MIN_INTERVAL = 2000;
    private static final int MSG_INVALIDATE = 1;
    protected static final String TAG = "ViewRootImplSmtEx";
    public static boolean sMonitorInput =
            (SystemProperties.getInt("persist.sys.monitor", 0) & 4) != 0;

    protected long mLastChangeRateTime;
    protected ISysTransServer mSysTransServer;
    protected ViewRootImpl mViewRootImpl;

    public ViewRootImplSmtBase(ViewRootImpl viewRootImpl) {
        mSysTransServer = null;
        mLastChangeRateTime = 0;
        mViewRootImpl = viewRootImpl;
    }

    private ISysTransServer getSysTransServer() {
        if (mSysTransServer == null) {
            IBinder b = ServiceManager.getService("systransserver");
            mSysTransServer = ISysTransServer.Stub.asInterface(b);
        }
        return mSysTransServer;
    }

    public void dispatchInvalidateImmediately(View view) {
        Message msg = mViewRootImpl.mHandler.obtainMessage(MSG_INVALIDATE, view);
        mViewRootImpl.mHandler.sendMessageAtFrontOfQueue(msg);
    }

    public void requestChangeDisplayRate() {
        long currentTime = SystemClock.uptimeMillis();
        if (currentTime - mLastChangeRateTime <= CHANGE_RATE_MIN_INTERVAL) {
            return;
        }
        if (getSysTransServer() != null) {
            try {
                getSysTransServer().requestChangeDisplayFps(Process.myPid(), 1, 2);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        mLastChangeRateTime = currentTime;
    }

    public void setInputEvent(InputEvent event, int orientation) {
        if (!sMonitorInput || !mViewRootImpl.mSurface.isValid()) {
            return;
        }
        if (event instanceof MotionEvent) {
            int action = ((MotionEvent) event).getAction();
            float x = ((MotionEvent) event).getRawX();
            float y = ((MotionEvent) event).getRawY();
            if (action == MotionEvent.ACTION_DOWN || action == MotionEvent.ACTION_UP) {
                mViewRootImpl.mSurface.setLastInputTime(x, y, event.getEventTimeNano(), -1,
                        action);
            }
            if (orientation == 1 && action == MotionEvent.ACTION_DOWN) {
                requestChangeDisplayRate();
            }
        } else if (event instanceof KeyEvent) {
            int action = ((KeyEvent) event).getAction();
            int keyCode = ((KeyEvent) event).getKeyCode();
            if (action == KeyEvent.ACTION_DOWN || action == KeyEvent.ACTION_UP) {
                mViewRootImpl.mSurface.setLastInputTime(0, 0, event.getEventTimeNano(), keyCode,
                        action);
            }
        }
    }
}
