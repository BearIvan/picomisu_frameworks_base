// Copyright 2026 Picomisu contributors
// SPDX-License-Identifier: Apache-2.0

package android.view;

import android.graphics.Rect;

import com.android.internal.statusbar.SystemUiDecoration;

/**
 * Smartisan window manager constants and layout parameter extension
 * (factory PICO OS 5.13.7 {@code android.view.WindowManagerSmtBase}; nothing in the factory jars
 * creates the layout parameter classes).
 *
 * @hide
 */
public interface WindowManagerSmtBase {
    String TAG = "WindowManagerSmtEx";

    boolean USING_SMARTISAN_KEYGUARD = true;

    int TRANSIT_APPLICATION_OPEN = 30;
    int TRANSIT_APPLICATION_CLOSE = 31;
    int TRANSIT_NO_ANIM = 1000;

    int TYPE_SCREENSHOT_APP_WITH_WALLPAPER = 2;
    int TYPE_SCREENSHOT_APP_WITH_KEYGUARD = 4;

    int IWINDOW_DISPATCH_ZOOMED_STATE_TRANSACTION = 10000;
    int IWINDOW_DISPATCH_PAUSING_REFRESH_TRANSACTION = 10001;

    /** Smartisan part of {@link WindowManager.LayoutParams}. */
    public static class LayoutParamsSmtBase {
        public static final int SM_PRIVATE_FLAG_NOTCH_ANDROID = 0x00000001;
        public static final int SM_PRIVATE_FLAG_NOTCH_SMARTISAN = 0x00000002;
        public static final int SM_PRIVATE_FLAG_NOTCH_NONE = 0x00000004;
        public static final int SM_PRIVATE_FLAG_NOTCH_MODE = SM_PRIVATE_FLAG_NOTCH_ANDROID
                | SM_PRIVATE_FLAG_NOTCH_SMARTISAN | SM_PRIVATE_FLAG_NOTCH_NONE;
        public static final int SM_PRIVATE_FLAG_IGNORE_NOTCH_SETTINGS = 0x00000008;
        public static final int SM_PRIVATE_FLAG_CAN_INTERCEPT_KEY_WHEN_TOP = 0x00000010;
        public static final int SM_PRIVATE_FLAG_HIDE_STATUS_BAR_FOR_KEYGUARD = 0x00000020;
        public static final int SM_PRIVATE_FLAG_FACE_ID_WINDOW = 0x00000040;
        public static final int SM_PRIVATE_FLAG_HIDE_IF_SECURE_WINDOW_SHOWN = 0x00000080;
        public static final int SM_PRIVATE_FLAG_DO_NOT_SCREEN_SHOT = 0x00000100;
        public static final int SM_PRIVATE_FLAG_CAN_BE_DRAGGED = 0x00000200;
        public static final int SM_PRIVATE_FLAG_DISABLE_IDEAPILLS = 0x00000400;
        public static final int SM_PRIVATE_FLAG_DISABLE_IDEAPILLS_LONG_PRESS_WHEN_RECORDING =
                0x00000800;
        public static final int SM_PRIVATE_FLAG_DO_NOT_UPDATE_POSITION = 0x00002000;
        public static final int SM_PRIVATE_FLAG_HAS_DECOR_CAPTION_VIEW = 0x00004000;
        public static final int SM_PRIVATE_FLAG_DISABLE_OVERVIEW_GESTURE = 0x00008000;
        public static final int SM_PRIVATE_FLAG_SCREENSHOT_WINDOW = 0x00010000;
        public static final int SM_PRIVATE_FLAG_FAKE_WINDOW = 0x00020000;
        public static final int SM_PRIVATE_FLAG_WINDOW_HAS_SURFACEVIEW = 0x00040000;
        public static final int SM_PRIVATE_FLAG_EXCLUDE_FROM_TAP_OUT_TASK = 0x00080000;
        public static final int SM_PRIVATE_FLAG_HIDE_CAPTION_BAR = 0x00100000;
        public static final int SM_PRIVATE_FLAG_NOTCH_SMARTISAN_DOUBLE_SIDE = 0x00200000;
        public static final int SM_PRIVATE_FLAG_TRANSLUCENT_THEME = 0x00400000;
        public static final int SM_PRIVATE_FLAG_FULLSCREEN_VOLUME_CONTROL = 0x00800000;
        public static final int SM_PRIVATE_FLAG_HANDLE_FOD_KEY = 0x01000000;
        public static final int SM_PRIVATE_FLAG_BRIGHTNESS_DIM_BEHIND = 0x02000000;
        public static final int SM_PRIVATE_FLAG_SCALE_TASK_FOREGOUND = 0x04000000;
        public static final int SM_PRIVATE_FLAG_FIXED_ROTATION = 0x08000000;
        public static final int SM_PRIVATE_FLAG_BLUR_BEHIND = 0x10000000;
        public static final int SM_PRIVATE_FLAG_SIDEBAR_TOP = 0x20000000;
        public static final int SM_PRIVATE_FLAG_STARTING_WINDOW_FOR_NAVI_BAR_MODE = 0x40000000;

        public static final int SMFLAG_INPUTFLINGER_FORCE_SPLIT = 0x00000001;
        public static final int SMFLAG_INPUTFLINGER_IGNORE_TOUCH_BY_HANDINHAND = 0x00000002;
        public static final int SMFLAG_INPUTFLINGER_CALENDAR_DRAG_WINDOW = 0x00000004;
        public static final int SMFLAG_INPUTFLINGER_IGNORE_DELAY_DISPATCH_EVENT = 0x00000008;
        public static final int SMFLAG_INPUTFLINGER_DISABLE_SCROLL_REPEAT = 0x00000010;
        public static final int SMFLAG_INPUTFLINGER_FIXED_TOUCHREGION = 0x00000020;
        public static final int SMFLAG_INPUTFLINGER_KEY_MAPPING_WINDOW = 0x00000080;
        public static final int SMFLAG_INPUTFLINGER_POINTER_CAPTURE = 0x00000100;

        protected WindowManager.LayoutParams mWindowParams;
        public int privateFlags;
        public int privateFlags2;
        public int privateFlags3;
        public int inputFlags = 0;
        public int smXMLFlagsFromActivityInfo;
        public int smXMLFlagsFromApplicationInfo;
        public float blurAmount = 0f;
        public SystemUiDecoration systemUiDecoration;
        public boolean drawDuringAnimation;
        public boolean isEatHomeKey;
        public Rect smTouchRegion = new Rect();

        public LayoutParamsSmtBase(WindowManager.LayoutParams params) {
            mWindowParams = params;
        }
    }

    /** Smartisan layout parameter extension. */
    public static class LayoutParamsSmtEx extends LayoutParamsSmtBase {
        public static final int PRIVATE_FLAG_SMARTISAN_DISABLE_SAVING_SURFACES = 0x20000000;

        public LayoutParamsSmtEx(WindowManager.LayoutParams params) {
            super(params);
        }
    }
}
