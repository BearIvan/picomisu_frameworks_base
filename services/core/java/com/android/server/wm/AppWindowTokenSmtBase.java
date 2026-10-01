// Copyright 2026 Picomisu contributors
// SPDX-License-Identifier: Apache-2.0

package com.android.server.wm;

import android.app.ActivityManager;
import android.content.res.CompatibilityInfo;
import android.os.IBinder;
import android.util.Slog;
import com.android.server.policy.SmtStartingSurface;
import com.android.server.policy.WindowManagerPolicy;

/**
 * Smartisan blurred starting window state of an {@link AppWindowToken}. The factory
 * AppWindowToken holds it, but nothing in the factory calls its starting window methods.
 * Reconstructed from the PICO OS 5.13.7 factory services.
 *
 * @hide
 */
public class AppWindowTokenSmtBase extends WindowContainerSmtBase {
    protected static final int STARTING_WINDOW_TYPE_NONE = 0;
    protected static final int STARTING_WINDOW_TYPE_SNAPSHOT = 1;
    protected static final int STARTING_WINDOW_TYPE_SPLASH_SCREEN = 2;
    public int animLayerAdjust;
    protected AppWindowToken mAppWindowToken;
    boolean mForceHideDockWindowAfterAnim;
    boolean mIsTranslucent;
    public boolean mNotReparentAnimationLayer;

    public AppWindowTokenSmtBase(AppWindowToken appWindowToken) {
        super(appWindowToken);
        this.animLayerAdjust = 0;
        this.mNotReparentAnimationLayer = false;
        this.mIsTranslucent = false;
        this.mAppWindowToken = appWindowToken;
    }

    private boolean snapshotOrientationSameAsTask(ActivityManager.TaskSnapshot snapshot) {
        return snapshot != null && this.mAppWindowToken.getTask().getConfiguration().orientation == snapshot.getOrientation();
    }

    int getStartingWindowType(boolean newTask, boolean taskSwitch, boolean processRunning, boolean allowTaskSnapshot, boolean activityCreated, boolean fromRecents, ActivityManager.TaskSnapshot snapshot) {
        boolean toLandScape = snapshot != null && this.mAppWindowToken.getDisplayContent().getConfiguration().orientation == 1 && snapshot.getOrientation() == 2;
        if (this.mAppWindowToken.getDisplayContent().mAppTransition.getAppTransition() == 19) {
            return 0;
        }
        if (newTask || !processRunning || (taskSwitch && !activityCreated && !toLandScape)) {
            return 2;
        }
        if (!taskSwitch || (!allowTaskSnapshot && !toLandScape)) {
            return 0;
        }
        if (this.mAppWindowToken.mWmService.mLowRamTaskSnapshotsAndRecents) {
            return 2;
        }
        WindowState mainWin = this.mAppWindowToken.findMainWindow();
        if (mainWin != null && (mainWin.mAttrs.privateFlags & 536870912) != 0) {
            Slog.i("WindowManager", "ignore wrong snapshot of in call screen or others");
            return 0;
        }
        if (snapshot == null) {
            return 0;
        }
        return (snapshotOrientationSameAsTask(snapshot) || fromRecents || toLandScape) ? 1 : 2;
    }

    void cacheFirstFrame(StartingData startingData, WindowState win, WindowList<WindowState> mChildren, WindowStateAnimator winAnimator) {
        if (startingData instanceof SmtStartingData) {
            CompatibilityInfo compatInfo = ((SmtStartingData) startingData).getCompatibilityInfo();
            String folderType = compatInfo.getSmtEx().getSmartisanPreviewStartingWindowType();
            if (compatInfo.getSmtEx().getSmtStartingWindowType() == 1 && "default".equals(folderType)) {
                String pkgName = win.getOwningPackage();
                boolean isTNTMode = compatInfo.getSmtEx().isTNTMode();
                if (isTNTMode) {
                    folderType = BlurStartingWindowUtils.TYPE_DEFAULT_TNT_STARTING_WINDOW;
                }
                if ("".equals(BlurStartingWindowUtils.checkFileExist(pkgName, folderType)) && (win.mToken instanceof AppWindowToken)) {
                    if (folderType != BlurStartingWindowUtils.TYPE_DEFAULT_TNT_STARTING_WINDOW) {
                        SmartisanStartingWindowManager.getInstance().cacheFirstFrame(pkgName, folderType, this.mAppWindowToken, false, null);
                    } else if (win.getDisplayContent() != null) {
                        SmartisanStartingWindowManager.getInstance().cacheFirstFrame(pkgName, folderType, this.mAppWindowToken, true, null);
                    }
                }
            }
        }
    }

    void cacheStartingWindow(StartingData startingData, WindowManagerPolicy.StartingSurface startingSurface) {
        String packageName;
        if (startingData != null && (startingData instanceof SmtStartingData)) {
            CompatibilityInfo compatInfo = ((SmtStartingData) startingData).getCompatibilityInfo();
            String folderType = compatInfo.getSmtEx().getSmartisanPreviewStartingWindowType();
            if (compatInfo.getSmtEx().getSmtStartingWindowType() == -1 && "default".equals(folderType)) {
                WindowState w = this.mAppWindowToken.findMainWindow();
                if ((startingSurface instanceof SmtStartingSurface) && this.mAppWindowToken.mWmService.mPolicy != null && w != null && (packageName = w.getOwningPackage()) != null && !"".equals(packageName)) {
                    if (compatInfo.getSmtEx().isTNTMode()) {
                        folderType = BlurStartingWindowUtils.TYPE_DEFAULT_TNT_STARTING_WINDOW;
                    }
                    if ("".equals(BlurStartingWindowUtils.checkFileExist(packageName, "default"))) {
                        SmartisanStartingWindowManager.getInstance().cacheStartingWindow(packageName, ((SmtStartingSurface) startingSurface).getStartingView(), folderType);
                        int smtStartingWindowFlag = compatInfo.getSmtEx().getSmtStartingWindowFlag();
                        if (smtStartingWindowFlag != 0) {
                            BlurStartingWindowUtils.storeBswInfo(BlurStartingWindowUtils.KEY_WINDOW_FLAG, String.valueOf(smtStartingWindowFlag), packageName, folderType);
                        }
                    }
                }
            }
        }
    }

    boolean isStartingWindowSmt(CompatibilityInfo compatInfo) {
        int smartisanFlag = 0;
        String folderType = "";
        if (compatInfo != null) {
            smartisanFlag = compatInfo.getSmtEx().getAppInfoSmartisanFlag();
            folderType = compatInfo.getSmtEx().getSmartisanPreviewStartingWindowType();
        } else {
            Slog.e("WindowManager", "smartisanFlag is empty from CompatibilityInfo");
        }
        boolean forceStartingWindow = (folderType == null || "".equals(folderType)) ? false : true;
        if ((smartisanFlag & 16) == 0) {
            return false;
        }
        return forceStartingWindow;
    }

    boolean needStartingWindowSmt(CompatibilityInfo compatInfo, boolean windowIsTranslucent, boolean windowIsFloating, boolean windowDisableStarting, boolean forceStartingWindow, IBinder transferFrom) {
        if (windowIsTranslucent) {
            if (!forceStartingWindow) {
                return false;
            }
            if (compatInfo != null) {
                compatInfo.getSmtEx().setSmtStartingWindowType(2);
            }
            this.mIsTranslucent = true;
            Slog.w("WindowManager", "atoken= " + this + " is TRANSLUCENT");
        }
        if (windowIsFloating || windowDisableStarting) {
            if (windowDisableStarting && transferFrom != null) {
                AppWindowToken fromToken = this.mAppWindowToken.getDisplayContent().getAppWindowToken(transferFrom);
                if (fromToken != null && fromToken.getAppWindowTokenSmtBase().mIsTranslucent) {
                    forceStartingWindow = true;
                }
            } else if (windowDisableStarting) {
                return false;
            }
            if (!forceStartingWindow) {
                return false;
            }
            if (compatInfo != null) {
                compatInfo.getSmtEx().setSmtStartingWindowType(2);
            }
        }
        return true;
    }
}
