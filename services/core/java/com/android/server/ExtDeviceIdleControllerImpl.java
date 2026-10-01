// Copyright 2026 Picomisu contributors
// SPDX-License-Identifier: Apache-2.0
package com.android.server;

import android.content.Context;
import android.database.ContentObserver;
import android.os.Handler;
import android.os.ResultReceiver;
import android.pico.utils.PicoUtils;
import android.provider.Settings;
import android.util.Log;

import java.io.FileDescriptor;

/**
 * PICO DeviceIdleController extension (factory PICO OS 5.13.7
 * com.android.server.ExtDeviceIdleControllerImpl). On ToB devices the global setting
 * pico_tob_disable_doze = 1 turns light and deep idle off at start-up, and a change of the
 * setting runs the "deviceidle disable" / "deviceidle enable" shell command.
 */
public class ExtDeviceIdleControllerImpl implements IExtDeviceIdleController {
    private static final String PICO_TOB_DISABLE_DOZE = "pico_tob_disable_doze";
    private static final String PICO_TOB_DOZE_DISABLE_CMD = "disable";
    private static final String PICO_TOB_DOZE_ENABLE_CMD = "enable";
    private static final String TAG = "DeviceIdleController";
    private DeviceIdleController mBase;

    public ExtDeviceIdleControllerImpl(DeviceIdleController controller) {
        mBase = controller;
    }

    @Override
    public boolean isDisableIdle(Context context) {
        if (!PicoUtils.IS_TOB_DEVICE) {
            return false;
        }
        boolean isDisableIdle = Settings.Global.getInt(context.getContentResolver(),
                PICO_TOB_DISABLE_DOZE, 0) == 1;
        Log.i(TAG, "isDisableIdle = " + isDisableIdle);
        return Settings.Global.getInt(context.getContentResolver(), PICO_TOB_DISABLE_DOZE, 0) == 1;
    }

    @Override
    public void registerDisableIdle(final Context context) {
        if (!PicoUtils.IS_TOB_DEVICE) {
            return;
        }
        context.getContentResolver().registerContentObserver(
                Settings.Global.getUriFor(PICO_TOB_DISABLE_DOZE), false,
                new ContentObserver(new Handler()) {
                    @Override
                    public void onChange(boolean selfChange) {
                        boolean disable = isDisableIdle(context);
                        DeviceIdleController.Shell shell = mBase.new Shell();
                        String[] args = new String[1];
                        args[0] = disable ? PICO_TOB_DOZE_DISABLE_CMD : PICO_TOB_DOZE_ENABLE_CMD;
                        shell.exec(null, null, new FileDescriptor(), null, args, null,
                                new ResultReceiver(null));
                    }
                });
    }
}
