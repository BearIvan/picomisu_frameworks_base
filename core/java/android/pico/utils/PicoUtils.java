// Copyright 2026 Picomisu contributors
// SPDX-License-Identifier: Apache-2.0

package android.pico.utils;

import android.app.ActivityThread;
import android.app.Application;
import android.app.WindowConfiguration;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PermissionInfo;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Rect;
import android.os.SystemProperties;
import android.util.DisplayMetrics;
import android.util.Slog;
import android.view.Display;
import android.view.DisplayInfo;
import android.view.Surface;
import android.view.ViewRootImpl;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * PICO helpers for 2D application configuration/display adjustment, VR application
 * detection and system-assisted runtime permission requests.
 *
 * @hide
 */
public class PicoUtils {
    public static final String TAG = "PicoResourcesUtils";

    public static final boolean IS_TOB_DEVICE =
            SystemProperties.getInt("ro.pxr.externalfunc", 0) != 0;
    public static boolean ENABLE_RESOURCE_UPDATE =
            SystemProperties.getBoolean("persist.pvr.resources.update", true);
    public static boolean DEBUG_RESOURCES_UPDATE =
            SystemProperties.getBoolean("pvr.debug.resources", false);

    private static ApplicationInfo mApplicationInfo;
    private static Configuration mConfiguration;

    public static void updateApplicationContextResources(ViewRootImpl viewRoot,
            Display display) {
        if (!Features.isAdjustConfigurationEnabled()) {
            return;
        }
        if (usingNewConfigurationSolution()) {
            return;
        }
        Application app = ActivityThread.currentApplication();
        if (app == null || app.getResources() == null || isVrType(mApplicationInfo)) {
            return;
        }
        Resources appResouces = app.getResources();
        if (display.getDisplayId() == app.getDisplayId()
                && appResouces.getDisplayMetrics().widthPixels == display.getWidth()) {
            return;
        }
        DisplayMetrics dm = new DisplayMetrics();
        display.getMetrics(dm);
        app.updateDisplay(display.getDisplayId());
        Configuration configuration =
                new Configuration(viewRoot.mContext.getResources().getConfiguration());
        appResouces.updateConfiguration(configuration, dm, null);
        viewRoot.requestLayout();
    }

    public static boolean isVrType(ApplicationInfo applicationInfo) {
        if (applicationInfo == null) {
            return false;
        }
        return applicationInfo.getExt().isVrApplication();
    }

    public static ApplicationInfo getApplicationInfo() {
        return mApplicationInfo;
    }

    public static void initApplication(ApplicationInfo applicationInfo,
            Configuration configuration) {
        if (applicationInfo != null) {
            mApplicationInfo = applicationInfo;
        }
        if (configuration != null) {
            mConfiguration = new Configuration(configuration);
        }
    }

    public static void updateDisplayInfo(int displayId, DisplayInfo displayInfo) {
        if (!Features.isAdjustConfigurationEnabled()) {
            return;
        }
        if (!usingNewConfigurationSolution()) {
            return;
        }
        if (displayId != Display.DEFAULT_DISPLAY || displayInfo == null) {
            return;
        }
        if (mApplicationInfo == null || isSystemApp(mApplicationInfo)
                || mApplicationInfo.getExt().isVrApp() || mConfiguration == null
                || ActivityThread.isSystem()) {
            return;
        }
        WindowConfiguration windowConfiguration = mConfiguration.windowConfiguration;
        Rect bound = windowConfiguration.getBounds();
        int w = bound.width();
        int h = bound.height();
        if (Math.abs(w - mApplicationInfo.getExt().get2dAppPortraitWidth()) <= 2
                && Math.abs(h - mApplicationInfo.getExt().get2dAppPortraitHeight()) <= 2) {
            w = mApplicationInfo.getExt().get2dAppPortraitWidth();
            h = mApplicationInfo.getExt().get2dAppPortraitHeight();
        } else if (Math.abs(w - mApplicationInfo.getExt().get2dAppLandscapeWidth()) <= 2
                && Math.abs(h - mApplicationInfo.getExt().get2dAppLandscapeHeight()) <= 2) {
            w = mApplicationInfo.getExt().get2dAppLandscapeWidth();
            h = mApplicationInfo.getExt().get2dAppLandscapeHeight();
        }
        int densityDpi = mConfiguration.densityDpi;
        displayInfo.logicalWidth = w;
        displayInfo.appWidth = w;
        displayInfo.largestNominalAppWidth = w;
        displayInfo.smallestNominalAppWidth = w;
        displayInfo.logicalHeight = h;
        displayInfo.appHeight = h;
        displayInfo.largestNominalAppHeight = h;
        displayInfo.smallestNominalAppHeight = h;
        displayInfo.logicalDensityDpi = densityDpi;
        displayInfo.physicalXDpi = displayInfo.physicalYDpi = densityDpi;
        displayInfo.rotation = Surface.ROTATION_0;
    }

    public static boolean usingNewConfigurationSolution() {
        return usingNewConfigurationSolution(mApplicationInfo);
    }

    public static boolean usingNewConfigurationSolution(ApplicationInfo applicationInfo) {
        if (applicationInfo == null) {
            return false;
        }
        if (isSystemApp(applicationInfo)) {
            return false;
        }
        return true;
    }

    public static boolean isSystemApp(ApplicationInfo applicationInfo) {
        return applicationInfo.isSystemApp() || applicationInfo.isSignedWithPlatformKey();
    }

    private static final List<String> PRE_REQUEST_PERMISSION_LIST = Arrays.asList(
            "com.motionx.pico.xkart",
            "com.tvb.cubism",
            "com.StormingTech.RestInPiecesPico",
            "com.SpheroomLimited.Jentrix",
            "com.MyronSoftware.Deisim",
            "com.sumalab.CrisisVRigade2",
            "com.StarcadeArcadeLLC.SpaceSlurpies",
            "com.Mardonpol.Alvo",
            "com.immersed.pico",
            "com.BitPlanetGamesLLC.Ultrawings2",
            "com.WenklyStudio.SurvivalNation",
            "com.ISVR.PartyPieFreePie",
            "com.cyberspline.boombox",
            "com.SDI.TWDCH2",
            "com.chesstar.chaoranyike.launcher",
            "com.miketeevee.shoresofloci",
            "com.Appnori.SummerSports",
            "com.gameboomvr.CookingSimulatorVR",
            "com.survios.CreedGlobal",
            "com.survios.Creed",
            "com.zoomgames.kidnapped.pico",
            "com.SalmiGmbH.SweetSurrenderVRTests",
            "com.WenklyStudio.PrivateProperty",
            "com.XrealGames.ZcReloaded",
            "com.hxvr.shuihuen",
            "com.MyDearest.Chronos",
            "com.LuoLin.vfc_normal",
            "com.Appnori.AllInOneSports",
            "com.Appnori.AllInOneSportsGB",
            "com.resolutiongames.DemeoPicoChina",
            "com.resolutiongames.DemeoPicoGlobal",
            "com.EmergeWorlds.DanceColliderQIYIVR",
            "com.VIVAGAMES.ImmortalLegacyVR",
            "com.Mardonpol.Alvo",
            "com.ThreeDAR.PaperBirds2Quest_Global",
            "com.ThreeDAR.PaperBirds2Quest",
            "com.tobii.usercalibration.neo3",
            "com.SquingleStudios.Squingle",
            "com.com2usroca.DS",
            "com.parkline.wander",
            "com.TribeXR.TribeXR",
            "com.shootv1.luckstudio",
            "Zzwei.Facing",
            "com.YourCompany.Sightofjoy",
            "com.Newmatic.PolystarPico",
            "com.hxdf.vrgame",
            "com.Cykyria.QuestForRunia",
            "com.VRFactory.VRBartenderSymulator",
            "com.YourCompany.ArcheryLand_2",
            "com.seventeenbit.songinthesmoke",
            "com.DX.BeatHero",
            "com.DX.Drum",
            "com.MagicTouch.Mashroom",
            // The leading "c" is part of the factory entry.
            "ccom.YourCompany.MeowStar");

    /**
     * Asks the permission controller to request the dangerous runtime permissions of a
     * listed (or flagged) application on its behalf before its activity starts.
     *
     * @return true when the VR permission request activity was started.
     */
    public static boolean helpRequestPermission(ActivityInfo aInfo, Context context,
            int userId) {
        if (aInfo == null) {
            return false;
        }
        if (!PRE_REQUEST_PERMISSION_LIST.contains(aInfo.packageName)
                && (aInfo.applicationInfo == null
                        || !aInfo.applicationInfo.getSmtEx().permissionFlag)) {
            return false;
        }
        PackageInfo packageInfo = null;
        try {
            packageInfo = context.getPackageManager().getPackageInfo(aInfo.packageName,
                    PackageManager.GET_PERMISSIONS);
        } catch (PackageManager.NameNotFoundException e) {
            Slog.i(TAG, "Cannot find package info for " + aInfo.packageName);
        }
        if (packageInfo == null || packageInfo.requestedPermissions == null) {
            return false;
        }
        ArrayList<String> reqList = new ArrayList<>();
        for (String permission : packageInfo.requestedPermissions) {
            PermissionInfo permissionInfo = null;
            try {
                permissionInfo = context.getPackageManager().getPermissionInfo(permission, 0);
            } catch (PackageManager.NameNotFoundException e) {
                e.printStackTrace();
            }
            if (permissionInfo != null) {
                if ((permissionInfo.getProtection() & PermissionInfo.PROTECTION_DANGEROUS)
                        == 0) {
                    continue;
                }
                reqList.add(permission);
            }
        }
        boolean needHelpRequestPermission = false;
        for (String permission : reqList) {
            if (context.getPackageManager().checkPermission(permission, aInfo.packageName)
                    != PackageManager.PERMISSION_GRANTED) {
                needHelpRequestPermission = true;
                break;
            }
        }
        if (needHelpRequestPermission) {
            Intent permissionIntent =
                    new Intent("android.content.pm.action.REQUEST_PERMISSIONS_VR");
            permissionIntent.putExtra(PackageManager.EXTRA_REQUEST_PERMISSIONS_NAMES,
                    reqList.toArray(new String[0]));
            permissionIntent.setPackage(
                    context.getPackageManager().getPermissionControllerPackageName());
            permissionIntent.putExtra("framework_transit_package", aInfo.packageName);
            permissionIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                    | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
            context.startActivity(permissionIntent);
            Slog.w(TAG, "startActivity return, special app need system help to apply for "
                    + "permission");
            return true;
        }
        return false;
    }

    public static void adjustConfiguration(Configuration configuration, int densityDpi,
            int widthPixels, int heightPixels) {
        widthPixels += 2;
        heightPixels += 2;
        configuration.densityDpi = densityDpi;
        float density = densityDpi / (float) DisplayMetrics.DENSITY_DEFAULT;
        configuration.screenWidthDp = (int) (widthPixels / density);
        configuration.screenHeightDp = (int) (heightPixels / density);
        int sl = Configuration.resetScreenLayout(configuration.screenLayout);
        if (widthPixels > heightPixels) {
            configuration.orientation = Configuration.ORIENTATION_LANDSCAPE;
            configuration.screenLayout = Configuration.reduceScreenLayout(sl,
                    configuration.screenWidthDp, configuration.screenHeightDp);
        } else {
            configuration.orientation = Configuration.ORIENTATION_PORTRAIT;
            configuration.screenLayout = Configuration.reduceScreenLayout(sl,
                    configuration.screenHeightDp, configuration.screenWidthDp);
        }
        configuration.smallestScreenWidthDp =
                Math.min(configuration.screenWidthDp, configuration.screenHeightDp);
        configuration.compatScreenWidthDp = configuration.screenWidthDp;
        configuration.compatScreenHeightDp = configuration.screenHeightDp;
        configuration.compatSmallestScreenWidthDp = configuration.smallestScreenWidthDp;
        WindowConfiguration windowConfiguration = configuration.windowConfiguration;
        windowConfiguration.setAppBounds(0, 0, widthPixels, heightPixels);
        windowConfiguration.setBounds(new Rect(0, 0, widthPixels, heightPixels));
        windowConfiguration.setRotation(Surface.ROTATION_0);
    }

    public static String parserJsonFile(String s) {
        File file = new File(s);
        if (!file.exists()) {
            return null;
        }
        String jsonStr = "";
        try {
            FileInputStream fis = new FileInputStream(file);
            Reader reader = new InputStreamReader(fis, "utf-8");
            StringBuffer sb = new StringBuffer();
            int ch;
            while ((ch = reader.read()) != -1) {
                sb.append((char) ch);
            }
            reader.close();
            fis.close();
            jsonStr = sb.toString();
            return jsonStr;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return jsonStr;
    }
}
