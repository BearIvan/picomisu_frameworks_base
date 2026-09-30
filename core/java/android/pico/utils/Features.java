// Copyright 2026 Picomisu contributors
// SPDX-License-Identifier: Apache-2.0

package android.pico.utils;

import android.os.SystemProperties;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;

/**
 * Compile-time and property-driven PICO framework feature switches.
 *
 * @hide
 */
public final class Features {
    // The factory build resolves the project name to a compile-time constant ("phoenix").
    private static boolean sIsPhx = Features.PROJECT_PHOENIX.equals(Features.PROJECT_PHOENIX);
    private static boolean sIsNeo3 = Features.PROJECT_NEO3.equals(Features.PROJECT_PHOENIX);

    private static final boolean KEYGUARD_ENABLE =
            SystemProperties.getBoolean("persist.pvr.keyguard_enabled", false);
    private static final boolean HAND_DIALOG_ENABLE =
            SystemProperties.getBoolean("persist.pvr.hand_dialog_enabled", !PicoUtils.IS_TOB_DEVICE);

    public static final String CONFIG_KEYGUARD_COMPONENT =
            "com.picovr.keyguard/com.pico.vr.keyguard.KeyguardService";

    public static final boolean FEAT_IMMS = true;
    public static final boolean FEAT_IMMS_2D_APP = true;
    public static final boolean FEAT_IMMS_ADJUST_DISPLAY_FRAME = true;
    public static final boolean FEAT_IMMS_DISABLE_CHECK_CLIENT_STATE = true;
    public static final boolean FEAT_IMMS_EXPAND_PANEL = true;
    public static final boolean FEAT_INPUT_ADJUST_KEY_DISPATCH = true;
    public static final boolean FEAT_INPUT_ADJUST_KEY_EVENT_DISPLAY_ID = true;
    public static final boolean FEAT_WMS_DEFAULT_DISPLAY_ALWAYS_HAS_FOCUS_WIN = true;
    public static final boolean FEAT_WMS_DISABLE_ANDROID_DIALOG = true;
    public static final boolean FEAT_WMS_DISABLE_ANDROID_GESTURE = true;
    public static final boolean FEAT_WMS_DISABLE_CUSTOM_SCREEN_ROTATION = true;
    public static final boolean FEAT_WMS_DISABLE_SHOW_STRICT_MODE = true;
    public static final boolean FEAT_WMS_FLOATING_WINDOW = true;
    public static final boolean FEAT_WMS_LOCK_TASK_MODE_WHITE_LIST = true;
    public static final boolean FEAT_WMS_WINDOW_ANTIALIASING = true;

    public static final boolean FEAT_DISABLE_ANR_CRASH_DIALOG =
            SystemProperties.getInt("persist.pvr.disableCrashAnr", 1) == 1;

    public static final boolean FEAT_ALLOW_PERSISTENT_APP_UPDATE = true;
    public static final boolean FEAT_API_LAYER_ENABLE = true;
    public static final boolean FEAT_APP_TOAST = true;
    public static final boolean FEAT_BACKUP_RESTORE = true;
    public static final boolean FEAT_BATTERY_OTG_LIMITE_PHX = true;
    public static final boolean FEAT_DEFAULT_GRANT_PERMISSIONS = true;
    public static final boolean FEAT_DISABLE_BOOT_COMPLETED = true;
    public static final boolean FEAT_DISABLE_DIM_POWER_STATE = true;
    public static final boolean FEAT_DISABLE_PROXIMITY_SCREEN_OFF_WAKE_LOCK = true;
    public static final boolean FEAT_DISABLE_START_SYNC_ADAPTER = true;
    public static final boolean FEAT_ENABLE_API_LAYER_NOTIFY_ACTIVITY_STRATING = true;
    public static final boolean FEAT_ENABLE_SHOW_ANR_DIALOG = true;
    public static final boolean FEAT_FIXED_SURFACE_LEAK_OF_UNITY_SDK = true;
    public static final boolean FEAT_GRANT_VR_OR_WHITE_APP_ALERT_WINDOW_PERMISSION = true;
    public static final boolean FEAT_HOLD_SCREEN_STATUS_WHEN_PLUG_STATE_CHANGE = true;
    public static final boolean FEAT_MODIFY_NO_RELEASE_PROVIDER = true;
    public static final boolean FEAT_POWER_DOUBLE_TAP_ACTION = true;
    public static final boolean FEAT_POWER_LED = true;
    public static final boolean FEAT_POWER_SHUTDOWN = true;
    public static final boolean FEAT_REDUCE_VR_ACTIVITY_BUFFER = true;
    public static final boolean FEAT_REFUSE_START_LAUNCHER = true;
    public static final boolean FEAT_REMOVE_HOME_GATEGORY = true;
    public static final boolean FEAT_RUNTIME_PERMISSION = true;
    public static final boolean FEAT_SEND_HOME_KEY_TO_USER = true;
    public static final boolean FEAT_SENSOR_NO_OFF_SCREEN = true;
    public static final boolean FEAT_SKIP_SET_CPU_ABI = true;
    public static final boolean FEAT_SKIP_SIGNING_CHECK = true;
    private static final boolean FEAT_SUPPORT_APP_DIRECT_LAUNCH = true;
    public static final boolean FEAT_SYNC_TIME_FROM_SERVER = true;
    public static final boolean FEAT_SYSTEM_APP_DOWNGRADE_UNPERMITTED = true;
    public static final boolean FEAT_TOB_DISABLE_DOZE = true;
    public static final boolean FEAT_USB = true;
    public static final boolean FEAT_VERITYPXRPACKAGE = true;
    public static final boolean FEAT_VRAPP_ALWAYS_LAUNCH_TO_MAIN_DISPLAY = true;

    public static final boolean ENABLE_CONFIG_2D_APP_DENSITY = true;

    public static final String ET_PERMISSION = "com.picovr.permission.EYE_TRACKING";
    public static final String ET_PERMISSION_GROUP = "com.picovr.permission-group.EYE_TRACKING";
    public static final String FT_PERMISSION = "com.picovr.permission.FACE_TRACKING";
    public static final String FT_PERMISSION_GROUP = "com.picovr.permission-group.FACE_TRACKING";

    public static final String PRODUCT_PICO_NEO3 = "FalconCV3";
    public static final String PRODUCT_PICO_PHOENIX = "Phoenix";
    public static final String PROJECT_MERLIN_E = "merline";
    public static final String PROJECT_NEO3 = "neo3";
    public static final String PROJECT_PHOENIX = "phoenix";

    public static boolean IS_PRO_DEVICES =
            SystemProperties.getInt("ro.pxr.eyetracking.support", 0) == 1;
    private static final String PRV_PRODUCT_NAME = SystemProperties.get("ro.pvr.product.name", "");

    public static boolean isDenyBackKeyIn2dApp() {
        return SystemProperties.getInt("pvr.2d_screen.reposition", 0) == 1;
    }

    public static final boolean isResizeVirtualDisplayEnabled() {
        return isPvr2DEnabled();
    }

    public static final boolean isAdjustConfigurationEnabled() {
        return isPvr2DEnabled();
    }

    public static final boolean isNsStartAppEnabled() {
        return true;
    }

    public static final boolean isPvr2DEnabled() {
        return true;
    }

    public static final boolean isKeyguardEnabled() {
        return !sIsNeo3;
    }

    public static final boolean isHandDialogEnabled() {
        return HAND_DIALOG_ENABLE;
    }

    public static final boolean disableDreamService() {
        return true;
    }

    public static final boolean disableShowInAuxiliaryDisplayToast(boolean isDefaultDisplay) {
        return isPvr2DEnabled() && !isDefaultDisplay;
    }

    public static final boolean disableWallpaper() {
        boolean showWallpaper = SystemProperties.getInt("sys.pvr.show.wallpaper", 0) == 1;
        if (!showWallpaper) {
            return true;
        }
        return false;
    }

    public static final boolean disableSystemAlert(Window window, boolean hasButton,
            View buttonPanel) {
        if (hasButton
                && window.getAttributes().type == WindowManager.LayoutParams.TYPE_SYSTEM_ALERT) {
            View spacer = buttonPanel.findViewById(com.android.internal.R.id.spacer);
            if (spacer != null) {
                spacer.setVisibility(View.GONE);
                return true;
            }
        }
        return false;
    }

    public static boolean isSupportAppDirectLaunch() {
        return FEAT_SUPPORT_APP_DIRECT_LAUNCH;
    }

    public static final boolean disableSystemUI() {
        return true;
    }

    public static boolean limitTheNumberOfDisplayCaches() {
        return true;
    }

    public static boolean enableLocalAnimation(String packageName) {
        return "com.sina.weibo".equals(packageName);
    }

    public static boolean supportETFeature() {
        if (!IS_PRO_DEVICES) {
            return false;
        }
        return true;
    }

    public static boolean supportFTFeature() {
        if (!IS_PRO_DEVICES) {
            return false;
        }
        if (PRODUCT_PICO_NEO3.equalsIgnoreCase(PRV_PRODUCT_NAME)) {
            return false;
        }
        return true;
    }

    public static boolean supportETCalibration() {
        if (IS_PRO_DEVICES && PRODUCT_PICO_PHOENIX.equalsIgnoreCase(PRV_PRODUCT_NAME)) {
            return true;
        }
        return false;
    }

    public static boolean isEnableConfig2dAppDensity() {
        return ENABLE_CONFIG_2D_APP_DENSITY
                && (PROJECT_NEO3.equals(getProjectName())
                        || PROJECT_MERLIN_E.equals(getProjectName()));
    }

    public static String getProjectName() {
        return PROJECT_PHOENIX;
    }
}
