// Copyright 2026 Picomisu contributors
// SPDX-License-Identifier: Apache-2.0

package android.content.pm;

import android.os.Parcel;
import android.util.SmtUidUtil;

/**
 * Smartisan system monitor extension of {@link ApplicationInfo}. Reconstructed from the PICO
 * OS 5.13.7 factory framework (the factory ApplicationInfo does not call the parcel and copy
 * methods).
 *
 * @hide
 */
public class ApplicationInfoMonitorEx {
    public int smtUid = 1000;

    public void readFromParcel(Parcel source) {
    }

    public void writeToParcel(Parcel dest) {
    }

    public void copyFrom(ApplicationInfoMonitorEx orig) {
    }

    public void clone(ApplicationInfoMonitorEx orig) {
    }

    public int getSmtUid(int uid, String packageName) {
        if (uid != 1000) {
            return uid;
        }
        if (smtUid == 1000) {
            smtUid = SmtUidUtil.getSystemUidForPackage(packageName);
        }
        int result = smtUid;
        return result;
    }
}
