// Copyright 2026 Picomisu contributors
// SPDX-License-Identifier: Apache-2.0

package smartisanos.os;

import android.util.SparseArray;

/**
 * Smartisan cache of binder call results made from system_server (AppOpsManager.checkPackage
 * on the factory). Reconstructed from the PICO OS 5.13.7 factory framework.
 *
 * @hide
 */
public class BinderCallCacheAgent {
    public static boolean isCalledFromSystemServer = false;
    private static SparseArray<String> checkPackageBinderCache = new SparseArray<>();

    public static void addCheckPackageBinderCache(int uid, String packageName) {
        synchronized (checkPackageBinderCache) {
            checkPackageBinderCache.put(uid, packageName);
        }
    }

    public static boolean inCheckPackageBinderCache(int uid, String packageName) {
        if (packageName == null) {
            return false;
        }
        synchronized (checkPackageBinderCache) {
            return packageName.equals(checkPackageBinderCache.get(uid));
        }
    }

    public static void removeCheckPackageBinderCache(int uid) {
        synchronized (checkPackageBinderCache) {
            checkPackageBinderCache.remove(uid);
        }
    }
}
