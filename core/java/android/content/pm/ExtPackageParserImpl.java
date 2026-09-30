// Copyright 2026 Picomisu contributors
// SPDX-License-Identifier: Apache-2.0
package android.content.pm;

import android.content.Intent;
import android.os.Bundle;
import android.pico.utils.Features;
import android.pico.utils.PicoSystemConfig;
import android.util.ArraySet;
import android.util.Slog;

import java.util.Arrays;
import java.util.List;

/**
 * Derives PICO VR application and activity flags and the 2D virtual-display configuration
 * from parsed manifest data, and filters the PICO eye/face-tracking permissions.
 * @hide
 */
public class ExtPackageParserImpl implements IExtPackageParser {
    private static final String TAG = "PackageParserExt";
    private static String[] VR_CATEGORY_ARRAY = {
            "com.google.intent.category.DAYDREAM",
            "com.google.intent.category.CARDBOARD",
            "com.qti.intent.category.SNAPDRAGON_VR",
            "com.htc.intent.category.VRAPP",
            "org.khronos.openxr.intent.category.IMMERSIVE_HMD",
    };
    private static String[] VR_TAG_ARRAY = {"com.picovr.type", "pvr.app.type"};
    private static String VR_ACTIVITY_FORCE_RENDER = "forceRenderActivity";
    /** Packages whose eye-tracking permission request is added implicitly. */
    public static final List<String> DEF_ADD_ET_PERMISSION =
            Arrays.asList("com.tobii.usercalibration.neo3");

    private PackageParser mBase;

    public ExtPackageParserImpl(PackageParser base) {
        mBase = base;
    }

    /**
     * Applies the factory VR metadata rules and the 2D virtual-display configuration.
     * The interface flag constants are read through {@code getExt()} as the factory does.
     */
    @Override
    public PackageParser.Package parseVrFlags(PackageParser.Package parsed) {
        ExtPackageParserUtils.loadVirtualDisplayConfigsFromFiles(false);
        boolean haveVrAppFlag = false;
        if (parsed.mAppMetaData != null) {
            for (int i = 0; i < VR_TAG_ARRAY.length; i++) {
                String value = parsed.mAppMetaData.getString(VR_TAG_ARRAY[i]);
                if (value != null) {
                    if (value.equalsIgnoreCase("vr")) {
                        haveVrAppFlag = true;
                        parsed.applicationInfo.getExt().setVrAppFlag(
                                parsed.applicationInfo.getExt().FLAG_VR_APP);
                        parsed.applicationInfo.getExt().setVrAppFlag(
                                parsed.applicationInfo.getExt().FLAG_APPLICATION_HAVE_VR_FLAG);
                        break;
                    } else if (value.equalsIgnoreCase("2d")) {
                        break;
                    }
                }
            }
        }
        ArraySet<String> whitelistVrApps =
                PicoSystemConfig.getInstance().getPicoWhitelistVrPackages();
        if (whitelistVrApps.contains(parsed.packageName)) {
            haveVrAppFlag = true;
            parsed.applicationInfo.getExt().setVrAppFlag(
                    parsed.applicationInfo.getExt().FLAG_VR_APP);
        }
        for (PackageParser.Activity activity : parsed.activities) {
            if (activity.className.equals("android.app.AppDetailsActivity")) {
                break;
            }
            Bundle metaData = activity.metaData;
            if (activity.info.targetActivity != null) {
                final int NA = parsed.activities.size();
                for (int i = 0; i < NA; i++) {
                    PackageParser.Activity t = parsed.activities.get(i);
                    if (activity.info.targetActivity.equals(t.info.name)) {
                        Slog.w(TAG, "use realActivity: " + activity.info + ", real: "
                                + activity.info.targetActivity);
                        metaData = t.metaData;
                        break;
                    }
                }
            }
            boolean have2DFlag = false;
            if (metaData != null) {
                activity.info.getExt().updateAppFeature(metaData);
                for (int i = 0; i < VR_TAG_ARRAY.length; i++) {
                    String value = metaData.getString(VR_TAG_ARRAY[i]);
                    if (value != null) {
                        if (value.equalsIgnoreCase("vr")) {
                            parsed.applicationInfo.getExt().setVrAppFlag(
                                    parsed.applicationInfo.getExt().FLAG_VR_APP);
                            activity.info.getExt().setVrActivity(
                                    activity.info.getExt().FLAG_VR_ACTIVITY);
                            break;
                        } else if (value.equalsIgnoreCase("2d")) {
                            have2DFlag = true;
                            break;
                        }
                    }
                }
                if (metaData.getBoolean(VR_ACTIVITY_FORCE_RENDER, false)) {
                    activity.info.getExt().setVrActivityForceRenderFlag(
                            activity.info.getExt().FLAG_VR_ACTIVITY_FORCE_RENDER);
                }
            }
            if (!have2DFlag) {
                for (PackageParser.ActivityIntentInfo intent : activity.intents) {
                    for (String category : VR_CATEGORY_ARRAY) {
                        if (intent.hasCategory(category) && intent.hasAction(Intent.ACTION_MAIN)) {
                            parsed.applicationInfo.getExt().setVrAppFlag(
                                    parsed.applicationInfo.getExt().FLAG_VR_APP);
                            activity.info.getExt().setVrActivity(
                                    activity.info.getExt().FLAG_VR_ACTIVITY);
                        }
                    }
                }
                if (activity.info.requestedVrComponent != null) {
                    parsed.applicationInfo.getExt().setVrAppFlag(
                            parsed.applicationInfo.getExt().FLAG_VR_APP);
                    activity.info.getExt().setVrActivity(
                            activity.info.getExt().FLAG_VR_ACTIVITY);
                }
            }
            if (!activity.info.getExt().isVrActivity() && !have2DFlag && haveVrAppFlag) {
                activity.info.getExt().setVrActivity(activity.info.getExt().FLAG_VR_ACTIVITY);
            }
            if (!activity.info.getExt().isVrActivity()) {
                parsed.applicationInfo.getExt().setVrAppFlag(
                        parsed.applicationInfo.getExt().FLAG_VR_APP_HAVE_2D_ACTIVITY);
            }
            for (PackageParser.ActivityIntentInfo intent : activity.intents) {
                if (intent.hasCategory(Intent.CATEGORY_LAUNCHER)
                        && intent.hasAction(Intent.ACTION_MAIN)) {
                    parsed.applicationInfo.getExt().setLaunchActivityOrientation(
                            activity.info.screenOrientation);
                }
            }
        }
        ExtPackageParserUtils.applyVirtualDisplayConfigToApp(parsed.applicationInfo);
        return parsed;
    }

    /**
     * Drops the face- and eye-tracking permission definitions of the platform package when
     * the device lacks the feature, and adds the eye-tracking permission request to the
     * packages of {@link #DEF_ADD_ET_PERMISSION} when it is supported.
     */
    @Override
    public void parseBaseApkCommon(PackageParser.Package pkg) {
        if (!Features.supportFTFeature()) {
            removePermission(pkg, "com.picovr.permission.FACE_TRACKING");
        }
        if (!Features.supportETFeature()) {
            removePermission(pkg, "com.picovr.permission.EYE_TRACKING");
        } else if (DEF_ADD_ET_PERMISSION.contains(pkg.packageName)
                && pkg.requestedPermissions.indexOf("com.picovr.permission.EYE_TRACKING") == -1) {
            pkg.requestedPermissions.add("com.picovr.permission.EYE_TRACKING");
            Slog.w(TAG, "add permission: com.picovr.permission.EYE_TRACKING, for pkg: "
                    + pkg.packageName);
        }
    }

    /**
     * Removes the permission {@code permissionName} declared by the platform package and,
     * when it has one, its permission group.
     */
    private void removePermission(PackageParser.Package pkg, String permissionName) {
        if (!"android".equals(pkg.packageName)) {
            return;
        }
        for (int i = pkg.permissions.size() - 1; i >= 0; i--) {
            PackageParser.Permission permission = pkg.permissions.get(i);
            if (permissionName.equals(permission.info.name)) {
                pkg.permissions.remove(i);
                Slog.w(TAG, "removePermission: " + permission + ", info.group: "
                        + permission.info.group);
                if (permission.info.group != null) {
                    for (int j = pkg.permissionGroups.size() - 1; j >= 0; j--) {
                        PackageParser.PermissionGroup group = pkg.permissionGroups.get(j);
                        if (group.info.name.equals(permission.info.group)) {
                            pkg.permissionGroups.remove(j);
                            Slog.w(TAG, "removePermissionGroup: " + group);
                            break;
                        }
                    }
                }
                break;
            }
        }
    }
}
