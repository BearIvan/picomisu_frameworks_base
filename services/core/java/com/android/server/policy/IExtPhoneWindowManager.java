/*
 * Copyright 2026 Picomisu contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package com.android.server.policy;

import android.content.Context;
import android.view.KeyEvent;
import android.view.WindowManager;

import com.pico.util.IExtBase;

/**
 * PICO extension of {@link PhoneWindowManager} (factory PICO OS 5.13.7
 * com.android.server.policy.IExtPhoneWindowManager), implemented by
 * {@link ExtPhoneWindowManagerImpl}.
 *
 * @hide
 */
public interface IExtPhoneWindowManager extends IExtBase {
    long adjustResultFromInterceptKeyBeforeDispatchingInner(WindowManagerPolicy.WindowState win,
            KeyEvent event, int policyFlags, long result);

    boolean denyBackKeyIn2DApp();

    void dispatchBackKeyTapMsg(KeyEvent event);

    void doMediaScan(Context context);

    int getDefaultMaxMultiPressPowerCount();

    boolean getPowerDefined();

    int getPowerLongPressStauts();

    int getPowerLongPressTime();

    int getPowerTapStauts();

    boolean handleKey(KeyEvent event, String winpackage);

    void init(Context context);

    void initSystemKey();

    void interceptVolumeEventAndApply(Context context, KeyEvent event);

    boolean interruptPowerLongPress();

    boolean interruptPowerPress();

    boolean isPowerDisabledForHcit();

    boolean isShortctShowOn3dApp(Context context, int repeatCount);

    boolean launchDockIfNeeded();

    boolean needCheckSystemAlertWindowPermission(Context context,
            WindowManager.LayoutParams attrs, int callingUid);

    int processKey(KeyEvent event, WindowManagerPolicy.WindowState win);

    void sendTapHomeMsgIfNeeded(KeyEvent event);

    boolean startLauncher3IfNeeded();

    void updateSystemKeyConfig();
}
