// Copyright 2026 Picomisu contributors
// SPDX-License-Identifier: Apache-2.0

package android.util;

import java.io.Serializable;
import java.util.HashMap;

/**
 * Serialized table of the Smartisan per-package uids given to system-uid packages.
 * Reconstructed from the PICO OS 5.13.7 factory framework; the member set (and so the default
 * serialVersionUID) matches the factory class.
 *
 * @hide
 */
public class SmtUidStatus implements Serializable {
    int base = 10000;
    HashMap<String, Integer> mSystemUid = new HashMap<>();

    public int getSystemUidForPackage(String packageName) {
        Integer result = mSystemUid.get(packageName);
        if (result == null) {
            result = newSystemUidForPackage();
            mSystemUid.put(packageName, result);
        }
        return result;
    }

    private int newSystemUidForPackage() {
        int newUid = 1000 - base;
        base++;
        return newUid;
    }

    public void resetSmtUidIfNeeded() {
        if (base > 10000000) {
            base = 10000;
            mSystemUid.clear();
        }
    }
}
