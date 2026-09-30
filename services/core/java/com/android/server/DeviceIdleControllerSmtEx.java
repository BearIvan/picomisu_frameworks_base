// Copyright 2026 Picomisu contributors
// SPDX-License-Identifier: Apache-2.0

package com.android.server;

import android.os.IDeviceIdleControllerSmtEx;

/**
 * Smartisan extension of the {@link DeviceIdleController} (its {@code mSmtEx}), and of its
 * local service ({@link LocalServiceSmtEx}, used by the sys services JAR). Reconstructed from
 * the PICO OS 5.13.7 factory services; as in the factory, the binder is created but not
 * published.
 *
 * @hide
 */
public class DeviceIdleControllerSmtEx extends IDeviceIdleControllerSmtEx.Stub {
    private DeviceIdleController mController;

    public DeviceIdleControllerSmtEx(DeviceIdleController controller) {
        mController = controller;
    }

    @Override
    public void setDozeMode(boolean lightDoze, boolean deepDoze) {
        mController.getContext().enforceCallingOrSelfPermission(
                android.Manifest.permission.DEVICE_POWER, null);
        mController.mLightEnabled = lightDoze;
        mController.mDeepEnabled = deepDoze;
    }

    /** Smartisan extension of the {@link DeviceIdleController.LocalService}. */
    public class LocalServiceSmtEx {
        private DeviceIdleController.LocalService mLocalService;

        public LocalServiceSmtEx(DeviceIdleController.LocalService localService) {
            mLocalService = localService;
        }

        public boolean addPowerSaveWhitelistApp(String name) {
            return mController.addPowerSaveWhitelistAppInternal(name);
        }

        public boolean removePowerSaveWhitelistApp(String name) {
            return mController.removePowerSaveWhitelistAppInternal(name);
        }

        public void updateQuickDozeFlag(boolean enabled) {
            synchronized (mController) {
                mController.updateQuickDozeFlagLocked(enabled);
            }
        }
    }
}
