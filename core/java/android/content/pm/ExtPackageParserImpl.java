// Copyright 2026 Picomisu contributors
// SPDX-License-Identifier: Apache-2.0
package android.content.pm;

import android.content.Intent;
import android.os.Bundle;
import android.pico.utils.PicoSystemConfig;
import android.util.Slog;

/**
 * Derives PICO VR application and activity flags from parsed manifest data.
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

    private PackageParser mBase;

    public ExtPackageParserImpl(PackageParser base) {
        mBase = base;
    }

    /**
     * Applies the factory VR metadata rules. The factory 2D virtual-display
     * configuration (ExtPackageParserUtils) is not applied here yet.
     */
    @Override
    public PackageParser.Package parseVrFlags(PackageParser.Package pkg) {
        final IExtApplicationInfo app = pkg.applicationInfo.getExt();
        boolean vrApp = false;
        if (pkg.mAppMetaData != null) {
            for (String tag : VR_TAG_ARRAY) {
                String type = pkg.mAppMetaData.getString(tag);
                if (type == null) {
                    continue;
                }
                if (type.equalsIgnoreCase("vr")) {
                    vrApp = true;
                    app.setVrAppFlag(IExtApplicationInfo.FLAG_VR_APP);
                    app.setVrAppFlag(IExtApplicationInfo.FLAG_APPLICATION_HAVE_VR_FLAG);
                    break;
                }
                if (type.equalsIgnoreCase("2d")) {
                    break;
                }
            }
        }
        if (PicoSystemConfig.getInstance().getPicoWhitelistVrPackages().contains(pkg.packageName)) {
            vrApp = true;
            app.setVrAppFlag(IExtApplicationInfo.FLAG_VR_APP);
        }
        for (PackageParser.Activity activity : pkg.activities) {
            if (activity.className.equals("android.app.AppDetailsActivity")) {
                break;
            }
            final IExtActivityInfo ext = activity.info.getExt();
            Bundle metaData = activity.metaData;
            if (activity.info.targetActivity != null) {
                final int count = pkg.activities.size();
                for (int i = 0; i < count; i++) {
                    PackageParser.Activity target = pkg.activities.get(i);
                    if (activity.info.targetActivity.equals(target.info.name)) {
                        Slog.w(TAG, "use realActivity: " + activity.info + ", real: "
                                + activity.info.targetActivity);
                        metaData = target.metaData;
                        break;
                    }
                }
            }
            boolean flat = false;
            if (metaData != null) {
                ext.updateAppFeature(metaData);
                for (String tag : VR_TAG_ARRAY) {
                    String type = metaData.getString(tag);
                    if (type == null) {
                        continue;
                    }
                    if (type.equalsIgnoreCase("vr")) {
                        app.setVrAppFlag(IExtApplicationInfo.FLAG_VR_APP);
                        ext.setVrActivity(IExtActivityInfo.FLAG_VR_ACTIVITY);
                        break;
                    }
                    if (type.equalsIgnoreCase("2d")) {
                        flat = true;
                        break;
                    }
                }
                if (metaData.getBoolean(VR_ACTIVITY_FORCE_RENDER, false)) {
                    ext.setVrActivityForceRenderFlag(IExtActivityInfo.FLAG_VR_ACTIVITY_FORCE_RENDER);
                }
            }
            if (!flat) {
                for (PackageParser.ActivityIntentInfo intent : activity.intents) {
                    for (String category : VR_CATEGORY_ARRAY) {
                        if (intent.hasCategory(category) && intent.hasAction(Intent.ACTION_MAIN)) {
                            app.setVrAppFlag(IExtApplicationInfo.FLAG_VR_APP);
                            ext.setVrActivity(IExtActivityInfo.FLAG_VR_ACTIVITY);
                        }
                    }
                }
                if (activity.info.requestedVrComponent != null) {
                    app.setVrAppFlag(IExtApplicationInfo.FLAG_VR_APP);
                    ext.setVrActivity(IExtActivityInfo.FLAG_VR_ACTIVITY);
                }
            }
            if (!ext.isVrActivity() && !flat && vrApp) {
                ext.setVrActivity(IExtActivityInfo.FLAG_VR_ACTIVITY);
            }
            if (!ext.isVrActivity()) {
                app.setVrAppFlag(IExtApplicationInfo.FLAG_VR_APP_HAVE_2D_ACTIVITY);
            }
            for (PackageParser.ActivityIntentInfo intent : activity.intents) {
                if (intent.hasCategory(Intent.CATEGORY_LAUNCHER)
                        && intent.hasAction(Intent.ACTION_MAIN)) {
                    app.setLaunchActivityOrientation(activity.info.screenOrientation);
                }
            }
        }
        return pkg;
    }
}
