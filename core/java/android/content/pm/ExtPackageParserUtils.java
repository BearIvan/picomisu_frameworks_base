// Copyright 2026 Picomisu contributors
// SPDX-License-Identifier: Apache-2.0
package android.content.pm;

import android.os.SystemProperties;
import android.pico.utils.Features;
import android.pico.utils.PicoUtils;
import android.text.TextUtils;
import android.util.ArrayMap;
import android.util.Slog;

import org.json.JSONArray;
import org.json.JSONObject;

/**
 * PICO 2D virtual-display configuration of applications: platform defaults from system
 * properties, optionally overridden per platform and per package by the cloud (or built-in)
 * app configuration JSON file.
 * @hide
 */
public class ExtPackageParserUtils {
    private static final String TAG = "PackageParserExt";
    private static final String APP_CONFIG_PATH_FROM_CLOUD =
            "/data/user_de/0/com.picovr.systemext/pvr_offline_res/app_cloud_config.txt";
    private static final String APP_CONFIG_PATH_DEFAULT = "/system/etc/app_cloud_config.txt";

    private static boolean mAppConfigsLoaded = false;
    private static VirtualDisplayConfig mPlatformVirtualDisplayConfig;
    private static ArrayMap<String, VirtualDisplayConfig> mAppVirtualDisplayOverrideConfigs =
            new ArrayMap<>();

    /**
     * Applies the platform configuration to {@code applicationInfo}, then the positive (or
     * set orientation) values of the package override, if there is one.
     */
    public static void applyVirtualDisplayConfigToApp(ApplicationInfo applicationInfo) {
        applyVirtualDisplayConfigToApp(applicationInfo, mPlatformVirtualDisplayConfig, true);
        VirtualDisplayConfig appConfig =
                mAppVirtualDisplayOverrideConfigs.get(applicationInfo.packageName);
        if (appConfig == null) {
            return;
        }
        applyVirtualDisplayConfigToApp(applicationInfo, appConfig, false);
    }

    private static void applyVirtualDisplayConfigToApp(ApplicationInfo applicationInfo,
            VirtualDisplayConfig appConfig, boolean force) {
        if (force || appConfig.mVirtualDisplayDensity > 0) {
            applicationInfo.getExt().set2dAppDensity(appConfig.mVirtualDisplayDensity);
        }
        if (force || appConfig.mVirtualDisplayPortraitWidth > 0) {
            applicationInfo.getExt().set2dAppPortraitWidth(appConfig.mVirtualDisplayPortraitWidth);
        }
        if (force || appConfig.mVirtualDisplayPortraitHeight > 0) {
            applicationInfo.getExt().set2dAppPortraitHeight(
                    appConfig.mVirtualDisplayPortraitHeight);
        }
        if (force || appConfig.mVirtualDisplayLandscapeWidth > 0) {
            applicationInfo.getExt().set2dAppLandscapeWidth(
                    appConfig.mVirtualDisplayLandscapeWidth);
        }
        if (force || appConfig.mVirtualDisplayLandscapeHeight > 0) {
            applicationInfo.getExt().set2dAppLandscapeHeight(
                    appConfig.mVirtualDisplayLandscapeHeight);
        }
        if (force || appConfig.mForceOrientation != -1) {
            applicationInfo.getExt().set2dAppForceOrientation(appConfig.mForceOrientation);
        }
        if (force || appConfig.mDefaultOrientation != -1) {
            applicationInfo.getExt().set2dAppDefaultOrientation(appConfig.mDefaultOrientation);
        }
    }

    /**
     * Loads the configuration once; {@code force} reloads it.
     */
    public static void loadVirtualDisplayConfigsFromFiles(boolean force) {
        synchronized (mAppVirtualDisplayOverrideConfigs) {
            if (force) {
                mAppConfigsLoaded = false;
            }
            loadVirtualDisplayConfigsFromFiles();
        }
    }

    private static void loadVirtualDisplayConfigsFromFiles() {
        if (mAppConfigsLoaded) {
            return;
        }
        mAppConfigsLoaded = true;
        mPlatformVirtualDisplayConfig = new VirtualDisplayConfig();
        if ("neo3".equals(Features.getProjectName())
                || "merline".equals(Features.getProjectName())) {
            mPlatformVirtualDisplayConfig.mVirtualDisplayDensity =
                    SystemProperties.getInt("persist.sys.pvr.2d_density", 480);
            mPlatformVirtualDisplayConfig.mVirtualDisplayPortraitWidth =
                    SystemProperties.getInt("persist.sys.pvr.2d_port_width", 1080);
            mPlatformVirtualDisplayConfig.mVirtualDisplayPortraitHeight =
                    SystemProperties.getInt("persist.sys.pvr.2d_port_height", 1920);
            mPlatformVirtualDisplayConfig.mVirtualDisplayLandscapeWidth =
                    SystemProperties.getInt("persist.sys.pvr.2d_land_width", 1920);
            mPlatformVirtualDisplayConfig.mVirtualDisplayLandscapeHeight =
                    SystemProperties.getInt("persist.sys.pvr.2d_land_height", 1080);
        } else {
            mPlatformVirtualDisplayConfig.mVirtualDisplayDensity =
                    SystemProperties.getInt("persist.sys.pvr.2d_density", 200);
            mPlatformVirtualDisplayConfig.mVirtualDisplayPortraitWidth =
                    SystemProperties.getInt("persist.sys.pvr.2d_port_width", 506);
            mPlatformVirtualDisplayConfig.mVirtualDisplayPortraitHeight =
                    SystemProperties.getInt("persist.sys.pvr.2d_port_height", 900);
            mPlatformVirtualDisplayConfig.mVirtualDisplayLandscapeWidth =
                    SystemProperties.getInt("persist.sys.pvr.2d_land_width", 1600);
            mPlatformVirtualDisplayConfig.mVirtualDisplayLandscapeHeight =
                    SystemProperties.getInt("persist.sys.pvr.2d_land_height", 900);
        }
        mPlatformVirtualDisplayConfig.mForceOrientation =
                SystemProperties.getInt("persist.sys.pvr.2d_force_ori", -1);
        mPlatformVirtualDisplayConfig.mDefaultOrientation =
                SystemProperties.getInt("persist.sys.pvr.2d_default_ori", 0);

        String jsonString = PicoUtils.parserJsonFile(APP_CONFIG_PATH_FROM_CLOUD);
        if (TextUtils.isEmpty(jsonString)) {
            jsonString = PicoUtils.parserJsonFile(APP_CONFIG_PATH_DEFAULT);
            if (TextUtils.isEmpty(jsonString)) {
                return;
            }
        }
        Slog.d(TAG, "begin load app config :" + jsonString);
        try {
            JSONObject outJson = new JSONObject(jsonString);
            if (outJson.has("platform_key_data")) {
                String depDataString = outJson.getString("platform_key_data");
                JSONArray dataArray = new JSONArray(depDataString);
                for (int i = 0; i < dataArray.length(); i++) {
                    JSONObject dataObj = dataArray.getJSONObject(i);
                    VirtualDisplayConfig platformConfig = mPlatformVirtualDisplayConfig;
                    if (parseVirtualDisplayConfig(dataObj, platformConfig)) {
                        Slog.d(TAG, "load platform config: " + platformConfig);
                        break;
                    }
                }
            }
            if (outJson.has("app_key_data")) {
                String depDataString = outJson.getString("app_key_data");
                JSONArray dataArray = new JSONArray(depDataString);
                for (int i = 0; i < dataArray.length(); i++) {
                    JSONObject dataObj = dataArray.getJSONObject(i);
                    VirtualDisplayConfig appConfig = new VirtualDisplayConfig();
                    if (parseVirtualDisplayConfig(dataObj, appConfig)
                            && appConfig.mPackageName != null) {
                        mAppVirtualDisplayOverrideConfigs.put(appConfig.mPackageName, appConfig);
                    }
                    Slog.d(TAG, "load app config: " + appConfig);
                }
            }
        } catch (Exception e) {
            Slog.i(TAG, "load app config error: " + e.getMessage());
        }
    }

    /**
     * Fills {@code appConfig} from one JSON entry. Returns false when the entry names
     * platforms ("a|b") that do not include the current project, or cannot be parsed.
     */
    private static boolean parseVirtualDisplayConfig(JSONObject dataObj,
            VirtualDisplayConfig appConfig) {
        try {
            if (dataObj.has("platform")) {
                String platform = dataObj.getString("platform");
                boolean hasOverrideConfig = false;
                String[] supportPlatforms = platform.split("\\|");
                for (String supportPlatform : supportPlatforms) {
                    if (Features.getProjectName().equals(supportPlatform)) {
                        hasOverrideConfig = true;
                        break;
                    }
                }
                if (!hasOverrideConfig) {
                    return false;
                }
            }
            if (dataObj.has("packageName")) {
                String packageName = dataObj.getString("packageName");
                appConfig.mPackageName = packageName;
            }
            if (dataObj.has("density")) {
                String density = dataObj.getString("density");
                appConfig.mVirtualDisplayDensity = Integer.parseInt(density);
            }
            if (dataObj.has("forceOrientation")) {
                String forceOrientation = dataObj.getString("forceOrientation");
                appConfig.mForceOrientation = Integer.parseInt(forceOrientation);
            }
            if (dataObj.has("defaultOrientation")) {
                String defaultOrientation = dataObj.getString("defaultOrientation");
                appConfig.mDefaultOrientation = Integer.parseInt(defaultOrientation);
            }
            if (dataObj.has("portraitWidth")) {
                String portraitWidth = dataObj.getString("portraitWidth");
                appConfig.mVirtualDisplayPortraitWidth = Integer.parseInt(portraitWidth);
            }
            if (dataObj.has("portraitHeight")) {
                String portraitHeight = dataObj.getString("portraitHeight");
                appConfig.mVirtualDisplayPortraitHeight = Integer.parseInt(portraitHeight);
            }
            if (dataObj.has("landscapeWidth")) {
                String landscapeWidth = dataObj.getString("landscapeWidth");
                appConfig.mVirtualDisplayLandscapeWidth = Integer.parseInt(landscapeWidth);
            }
            if (dataObj.has("landscapeHeight")) {
                String landscapeHeight = dataObj.getString("landscapeHeight");
                appConfig.mVirtualDisplayLandscapeHeight = Integer.parseInt(landscapeHeight);
            }
            return true;
        } catch (Exception e) {
            Slog.i(TAG, "load app config error: " + e.getMessage());
            return false;
        }
    }

    private static class VirtualDisplayConfig {
        private String mPackageName;
        private int mVirtualDisplayDensity = 0;
        private int mVirtualDisplayPortraitWidth = 0;
        private int mVirtualDisplayPortraitHeight = 0;
        private int mVirtualDisplayLandscapeWidth = 0;
        private int mVirtualDisplayLandscapeHeight = 0;
        private int mForceOrientation = -1;
        private int mDefaultOrientation = -1;

        @Override
        public String toString() {
            return "VirtualDisplayConfig{mPackageName='" + mPackageName + '\''
                    + ", mVirtualDisplayDensity=" + mVirtualDisplayDensity
                    + ", mVirtualDisplayPortraitWidth=" + mVirtualDisplayPortraitWidth
                    + ", mVirtualDisplayPortraitHeight=" + mVirtualDisplayPortraitHeight
                    + ", mVirtualDisplayLandscapeWidth=" + mVirtualDisplayLandscapeWidth
                    + ", mVirtualDisplayLandscapeHeight=" + mVirtualDisplayLandscapeHeight
                    + ", mForceOrientation=" + mForceOrientation
                    + ", mDefaultOrientation=" + mDefaultOrientation + '}';
        }
    }
}
