// Copyright 2026 Picomisu contributors
// SPDX-License-Identifier: Apache-2.0
package com.android.server.am;

import android.content.ComponentName;
import android.content.Intent;
import android.os.SystemProperties;
import android.provider.Settings;
import android.util.Slog;

import java.io.FileReader;

/**
 * PICO user controller extension (factory PICO OS 5.13.7
 * com.android.server.am.ExtUserControllerImpl):
 * <ul>
 * <li>finishUserUnlocked (system user): binds the PICO persistent services again
 * (IExtActivityManagerService.startPicoPersistentService);</li>
 * <li>startPicoFactoryTestService (after BOOT_COMPLETED is posted): unless the first byte of
 * /mnt/vendor/persist/falcon/identifying is '2' (an unreadable file counts as not '2'), the
 * device is in factory test mode: persist.pvr.logcatch is set to 1; on user builds adb is enabled, otherwise the
 * factory test serial port service is started; with persist.picovr.no_adb_auth=1,
 * pico.factory.adb_noauth is set to 0.</li>
 * </ul>
 * @hide
 */
public class ExtUserControllerImpl implements IExtUserController {
    static final String TAG = "ActivityManager";
    private UserController mBase;

    public ExtUserControllerImpl(UserController base) {
        mBase = base;
    }

    @Override
    public void startPicoFactoryTestService(ActivityManagerService service) {
        boolean needFactoryTest = read("/mnt/vendor/persist/falcon/identifying") != 50;
        boolean isOpenLogcat = SystemProperties.getInt("persist.pvr.logcatch", 0) == 1;
        Slog.i(TAG, "FinishBooting ...." + needFactoryTest + " ,isOpenLogcat = "
                + isOpenLogcat);
        if (needFactoryTest) {
            if (!isOpenLogcat) {
                SystemProperties.set("persist.pvr.logcatch", "1");
            }
            boolean isUser = "user".equals(SystemProperties.get("ro.build.type"));
            if (isUser) {
                Settings.Global.putInt(service.mContext.getContentResolver(),
                        Settings.Global.ADB_ENABLED, 1);
                return;
            }
            try {
                Slog.i(TAG, "android.intent.action.PICO_FACTORY_TEST ....");
                Intent factory = new Intent();
                factory.setComponent(new ComponentName("com.picovr.factorytest",
                        "com.picovr.factorytest.serialport.SerialPortService"));
                service.mContext.startService(factory);
            } catch (Exception e) {
                Slog.e(TAG, "Factory test error : " + e.toString());
            }
        }
        boolean isNotNeedADBAuth = SystemProperties.getInt("persist.picovr.no_adb_auth", 0) == 1;
        if (needFactoryTest && isNotNeedADBAuth) {
            SystemProperties.set("pico.factory.adb_noauth", "0");
        }
    }

    private int read(String filename) {
        try {
            FileReader reader = new FileReader(filename);
            int ret = reader.read();
            reader.close();
            Slog.i(TAG, "reader filename status is" + ret);
            return ret;
        } catch (Exception e) {
            Slog.e(TAG, "read tp status err: " + filename);
            return -1;
        }
    }

    @Override
    public void finishUserUnlocked(ActivityManagerService service) {
        Slog.w(TAG, "finishUserUnlocked, startPicoPersistentService");
        service.getExt().startPicoPersistentService();
    }
}
