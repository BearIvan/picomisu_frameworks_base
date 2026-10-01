// Copyright 2026 Picomisu contributors
// SPDX-License-Identifier: Apache-2.0
package com.android.server.pm;

import android.content.pm.PackageParser;
import android.pico.utils.PicoUtils;

import com.pico.util.IExtBase;

import java.io.PrintWriter;

/**
 * PICO package settings extension (factory PICO OS 5.13.7 com.android.server.pm.IExtSettings):
 * the VR flags and 2D virtual display configuration of a package in {@code dumpsys package}.
 */
public interface IExtSettings extends IExtBase {
    static void dumpPackageLPrExt(PrintWriter pw, String prefix, PackageSetting ps) {
        pw.print(prefix);
        pw.print("  isVrApp: ");
        pw.println(ps.pkg.applicationInfo.getExt().isVrApp() ? "true" : "false");
        pw.print(prefix);
        pw.print("  isAllComponentVr: ");
        pw.println(ps.pkg.applicationInfo.getExt().isAllComponentVr() ? "true" : "false");
        if (!ps.pkg.applicationInfo.getExt().isVrApp()
                && !PicoUtils.isSystemApp(ps.pkg.applicationInfo)) {
            pw.print(prefix);
            pw.print("  VD density: ");
            pw.println(ps.pkg.applicationInfo.getExt().get2dAppDensity());
            pw.print(prefix);
            pw.print("  VD portrait width: ");
            pw.println(ps.pkg.applicationInfo.getExt().get2dAppPortraitWidth());
            pw.print(prefix);
            pw.print("  VD portrait height: ");
            pw.println(ps.pkg.applicationInfo.getExt().get2dAppPortraitHeight());
            pw.print(prefix);
            pw.print("  VD landscape width: ");
            pw.println(ps.pkg.applicationInfo.getExt().get2dAppLandscapeWidth());
            pw.print(prefix);
            pw.print("  VD landscape height: ");
            pw.println(ps.pkg.applicationInfo.getExt().get2dAppLandscapeHeight());
            pw.print(prefix);
            pw.print("  VD force orientation: ");
            pw.println(ps.pkg.applicationInfo.getExt().get2dAppForceOrientation());
            pw.print(prefix);
            pw.print("  VD default orientation: ");
            pw.println(ps.pkg.applicationInfo.getExt().get2dAppDefaultOrientation());
            pw.print(prefix);
            pw.print("  VD launch activity orientation: ");
            pw.println(ps.pkg.applicationInfo.getExt().getLaunchActivityOrientation());
        }
        if (ps.pkg.applicationInfo.getExt().isVrApp()) {
            for (PackageParser.Activity activity : ps.pkg.activities) {
                pw.print(prefix);
                pw.print(activity.className);
                pw.print(" isVrActivity: ");
                pw.println(activity.info.getExt().isVrActivity() ? "true" : "false");
            }
        }
    }
}
