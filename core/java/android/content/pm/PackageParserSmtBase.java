// Copyright 2026 Picomisu contributors
// SPDX-License-Identifier: Apache-2.0

package android.content.pm;

import android.content.ComponentName;
import android.os.Bundle;
import android.os.SystemProperties;
import android.util.Slog;

import dalvik.system.DexClassLoader;

import smartisanos.os.PeroptWhiteListParser;

import java.util.ArrayList;

/**
 * Smartisan/PICO hooks called from {@link PackageParser} while a package is parsed.
 * Reconstructed from the PICO OS 5.13.7 factory framework.
 *
 * @hide
 */
public abstract class PackageParserSmtBase {
    private static final String TAG = "PackageParserSmtBase";

    /**
     * Applies the Smartisan activity white list flags to {@code info}.
     */
    public static void updateSmartisanXMLFlags(ActivityInfo info,
            ArrayList<PackageParser.ActivityIntentInfo> intents) {
        info.getSmtEx().smXMLFlags = 0;
        ComponentName componentName = new ComponentName(info.packageName, info.name);
        PeroptWhiteListParser.WhiteItem item =
                PeroptWhiteListParser.activityWhiteList.get(componentName.flattenToShortString());
        if (item != null) {
            info.getSmtEx().smXMLFlags = item.SMFlag;
        }
        item = PeroptWhiteListParser.activityWhiteList.get(info.name);
        if (item != null) {
            info.getSmtEx().smXMLFlags |= item.SMFlag;
        }
    }

    /**
     * Reads the PICO VR meta-data of an application or activity into the Smartisan
     * application extension.
     */
    public static void updateApplicationMetaData(ApplicationInfo info, Bundle owner) {
        String isVr = "";
        if (owner.containsKey("pvr.app.type")) {
            isVr = owner.getString("pvr.app.type");
        } else if (owner.containsKey("com.picovr.type")) {
            isVr = owner.getString("com.picovr.type");
        }
        if ("vr".equals(isVr)) {
            info.getSmtEx().isVrApp = true;
        }
        if (owner.containsKey("pvr.sdk.version")) {
            String enfineType = owner.getString("pvr.sdk.version");
            Slog.e(TAG, " pack = " + info.packageName + " ; enfineType = " + enfineType);
            if (enfineType.contains("XR Platform_") || enfineType.contains("Unity_")) {
                info.getSmtEx().vrAppEngine = 1;
            } else if (enfineType.contains("UE4")) {
                info.getSmtEx().vrAppEngine = 2;
            }
        }
        if (owner.containsKey("pxr.sdk.version_code")) {
            info.getSmtEx().vrAppSdkVersionCode = owner.getInt("pxr.sdk.version_code", 0);
        }
    }

    /**
     * Enables the class overrider for packages that bundle the PICO controller client.
     */
    public static void verifyLibraryFiles(PackageParser.Package parsed) {
        String dexPath = null;
        String path = parsed.codePath;
        String filename = "base.apk";
        if (SystemProperties.getInt("persist.sys.classoverrider.close", 0) == 1) {
            parsed.applicationInfo.getSmtEx().mOverrideClassSDK = 0;
            return;
        }
        try {
            DexClassLoader classLoader = new DexClassLoader(path + "/" + filename, path, null,
                    ClassLoader.getSystemClassLoader());
            Class checkClass = classLoader.loadClass(
                    "com.picovr.picovrlib.cvcontrollerclient.ControllerClient");
            if (checkClass != null) {
                dexPath = "/system/framework/sys-sharememory.jar";
            }
        } catch (Exception e) {
        }
        if (dexPath != null) {
            parsed.applicationInfo.getSmtEx().mOverrideClassSDK = 1;
        }
    }
}
