// Copyright 2026 Picomisu contributors
// SPDX-License-Identifier: Apache-2.0

package com.android.server.wm;

import android.content.Context;
import android.os.Handler;
import com.android.server.IBoostFrameworkOptEx;
import com.android.server.SysOptBridge;

/**
 * Reconstructed from the PICO OS 5.13.7 factory services. Nothing in the factory services,
 * sys-services or sysmonitor-services references it; it is carried for parity.
 *
 * @hide
 */
public abstract class DisplayPolicySmtBase {
    protected static final boolean DEBUG = false;
    protected static final int DELAY_APPLY_THUMB_WINDOW_MOVE_DOWN = 65;
    protected static final int MSG_SHOW_GAME_BAR = 1000;
    static final int MSG_UPDATE_SYSTEM_UI_TIMEOUT = 1001;
    protected static final String TAG = "WindowManager";
    protected Context mContext;
    protected DisplayContent mDisplayContent;
    protected DisplayPolicy mDisplayPolicy;
    protected Handler mHandler;
    private boolean mIsPerfBoostFlingAcquired;
    boolean mLeftSlideLunchBubble;
    int mNavigationBarFixedState = -1;
    int mNavigationBarMode = -1;
    int mNavigationBarTriggerMode = 0;
    int mStatusBarDisabled = 0;
    int mNavigationIconHints = 0;
    boolean mSmForceShowNavigationBar = false;
    boolean mOnUpdateSystemUiVisibility = false;
    boolean mNeedUpdateSystemUiVisibility = false;
    WindowState mRecentWin = null;
    private IBoostFrameworkOptEx mBoostFrameworkFlinger = null;
    private IBoostFrameworkOptEx mBoostFrameworkDrag = null;
    private int mBoostFlingerHandle = -1;
    private int mBoostDragHandle = -1;

    public DisplayPolicySmtBase(DisplayPolicy displayPolicy, DisplayContent displayContent, Context context, Handler handler) {
        this.mDisplayPolicy = displayPolicy;
        this.mDisplayContent = displayContent;
        this.mContext = context;
        this.mHandler = handler;
        if (this.mDisplayContent.isDefaultDisplay) {
            displayContent.registerPointerEventListener(SysOptBridge.getFactory().getScenesPointerEventListener());
        }
    }

    public void onVerticalFling(int durationMs) {
        IBoostFrameworkOptEx iBoostFrameworkOptEx = this.mBoostFrameworkFlinger;
        if (iBoostFrameworkOptEx != null) {
            if (this.mBoostFlingerHandle == -1) {
                this.mBoostFlingerHandle = iBoostFrameworkOptEx.configBoostParams(4, 1814000, 4, 1226000);
            }
            if (this.mBoostFlingerHandle != -1) {
                this.mBoostFrameworkFlinger.enableBoost(durationMs + 160);
                this.mIsPerfBoostFlingAcquired = true;
            }
        }
    }

    public void onHorizontalFling(int durationMs) {
    }

    public void onScroll(boolean started) {
        IBoostFrameworkOptEx iBoostFrameworkOptEx = this.mBoostFrameworkDrag;
        if (iBoostFrameworkOptEx != null) {
            if (this.mBoostDragHandle == -1) {
                this.mBoostDragHandle = iBoostFrameworkOptEx.configBoostParams(4, 1085000, 4, 1050000);
            }
            if (started) {
                if (this.mBoostDragHandle != -1) {
                    this.mBoostFrameworkDrag.enableBoost(-1);
                    return;
                }
                return;
            }
            this.mBoostFrameworkDrag.disableBoost();
        }
    }

    public void onDown() {
        IBoostFrameworkOptEx iBoostFrameworkOptEx = this.mBoostFrameworkFlinger;
        if (iBoostFrameworkOptEx != null && this.mIsPerfBoostFlingAcquired) {
            iBoostFrameworkOptEx.disableBoost();
            this.mIsPerfBoostFlingAcquired = false;
        }
    }
}
