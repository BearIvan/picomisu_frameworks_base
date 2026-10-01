// Copyright 2026 Picomisu contributors
// SPDX-License-Identifier: Apache-2.0
package com.android.server.pm;

import android.content.ComponentName;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageParser;
import android.text.TextUtils;
import android.util.Log;
import android.util.Slog;

import com.pico.util.IExtBase;

import dalvik.system.VMRuntime;

import java.security.PublicKey;
import java.util.Arrays;
import java.util.List;

/**
 * PICO package manager extension (factory PICO OS 5.13.7
 * com.android.server.pm.IExtPackageManagerService).
 *
 * The static helpers are used by PackageManagerService and PackageManagerServiceUtils directly:
 * {@link #skipSetCpuAbi} keeps a 32-bit system-uid package from forcing its ABI on the shared
 * user, and {@link #skipSigningCheck} lets the packages of {@link #SKIP_SIGNING_CHECK_LIST}
 * signed with the PICO store key ({@link #mAuthPublicKey1}) replace an installed version with a
 * different signature.
 */
public interface IExtPackageManagerService extends IExtBase {
    public static final List<String> SKIP_SIGNING_CHECK_LIST = Arrays.asList(
            "com.kluge.SynthRiders", "unity.SUPERHOT_Team.SUPERHOT_VR");
    public static final String mAuthPublicKey1 = "995f8c9530ebf7392828b461f3ceb364afd7364ff86c83029f72fc4e27e4f1e0bece6bae9eed1724394a746930f27425664d679f4e2f915a6da2f1eb15b2c69fe8ee85998f6243f5671f042e547adb0494e17cbd05247329e6322b59775e366f9e2e6083d6f63917120849699c28d7d0b0c77d67651b9fbb9db2d38660c2dccccbb7ddd30426cdc99eb59623a34003f620f4d5c5af21ab09cf2e06f6c3441514fa2b7b4e861f66da561f13a9553cf81b52fe37c14f45c8e3583e874fedde5ffba84384a4d8e1cff522f950a037f8db991765bc65ca4770330d88920daedcd1a96d4c3be09945c82d3744a29ab166918916fdbb744d2efbf20f0286e1ddd7e837";

    boolean allowPersistentUpdate();

    ActivityInfo getActivityInfoInternal(ComponentName component, int flags, int userId);

    void init();

    boolean isGrantPermission(boolean oldGrantPermissions);

    void notifyPackageInstallEnd(List<PackageManagerService.InstallRequest> requests);

    void notifyPackageInstallStart(List<PackageManagerService.InstallRequest> requests);

    void onSystemReady();

    void registerIInstalldConnectListener(Installer installer);

    void removeHomeCategory(PackageParser.Package parsed);

    void setCreateAppDEDataStateIfNeed(int flags);

    void verityPxrPackage(PackageParser.Package pkg,
            PackageManagerService.PackageInstalledInfo outRes)
            throws PackageManagerService.PrepareFailure;

    static boolean skipSetCpuAbi(PackageSetting ps) {
        boolean skip = false;
        skip = ps.getSharedUserId() == 1000
                && !VMRuntime.is64BitInstructionSet(
                        VMRuntime.getInstructionSet(ps.primaryCpuAbiString));
        if (skip) {
            Slog.v(PackageManagerServiceMonitorEx.TAG,
                    "ps[" + ps.name + "] wants to set cpu abi to 32 bit,skip!");
        }
        return skip;
    }

    static String getPublicKeyStr(String input) {
        int begin = 0;
        int end = 0;
        if (input != null) {
            begin = input.toString().indexOf("modulus=") + 8;
            end = input.toString().indexOf("publicExponent=") - 1;
            String keyString = input.toString().substring(begin, end);
            return keyString;
        }
        return null;
    }

    static boolean skipSigningCheck(PackageParser.SigningDetails parsedSignatures,
            String packageName) {
        try {
            PublicKey publicKey = parsedSignatures.signatures[0].getPublicKey();
            boolean result = SKIP_SIGNING_CHECK_LIST.contains(packageName)
                    && checkPublicKeyMatch(publicKey);
            Log.w(PackageManagerServiceMonitorEx.TAG,
                    "skipSigningCheck parsedSignatures result=" + result);
            return result;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    static boolean skipSigningCheck(PackageParser.Package pkg) {
        try {
            PublicKey publicKey = pkg.mSigningDetails.signatures[0].getPublicKey();
            boolean result = SKIP_SIGNING_CHECK_LIST.contains(pkg.packageName)
                    && checkPublicKeyMatch(publicKey);
            Log.w(PackageManagerServiceMonitorEx.TAG,
                    "skipSigningCheck PackageParser result=" + result);
            return result;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    static boolean checkPublicKeyMatch(PublicKey publicKey) {
        if (publicKey == null) {
            return false;
        }
        String publickKeyStr = getPublicKeyStr(publicKey.toString());
        if (TextUtils.equals(publickKeyStr, mAuthPublicKey1)) {
            return true;
        }
        return false;
    }
}
