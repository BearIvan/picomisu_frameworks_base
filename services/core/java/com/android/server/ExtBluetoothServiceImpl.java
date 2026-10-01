// Copyright 2026 Picomisu contributors
// SPDX-License-Identifier: Apache-2.0
package com.android.server;

import android.content.ComponentName;
import android.os.IBinder;
import android.os.RemoteException;
import android.os.SystemProperties;

import com.android.server.api.ApiLayerService;
import com.pico.api.app.IApiLayer;

/**
 * PICO BluetoothService extension (factory PICO OS 5.13.7
 * com.android.server.ExtBluetoothServiceImpl): when the Bluetooth firmware load flag
 * (persist.pvr.bluetooth.firmware.load) selects the Swift controller firmware, the
 * com.pvr.btperipheral KeepAliveService is kept connected through the API layer.
 */
public class ExtBluetoothServiceImpl implements IExtBluetoothService {
    private static final int BLUETOOTH_FIRMWARE_LOAD_ALL_BONDED = 5;
    private static final String BLUETOOTH_FIRMWARE_LOAD_FLAG_PROPERTY =
            "persist.pvr.bluetooth.firmware.load";
    private static final int BLUETOOTH_FIRMWARE_LOAD_ONE_BONDED = 4;
    private static final int BLUETOOTH_FIRMWARE_LOAD_ORIGINAL = 0;
    private static final int BLUETOOTH_FIRMWARE_LOAD_ORIGINAL_NEXT_REBOOT = 3;
    private static final int BLUETOOTH_FIRMWARE_LOAD_SWIFT = 1;
    private static final int BLUETOOTH_FIRMWARE_RELOADING = 2;
    private static final String SWIFT_COMPONENT_NAME = "com.pvr.btperipheral/.KeepAliveService";
    private BluetoothService mBase;

    public ExtBluetoothServiceImpl(BluetoothService base) {
        mBase = base;
    }

    @Override
    public void bindUnbindPeripheralServiceIfNeed(boolean bind) {
        if (isSwiftServicePersistedStateOn()) {
            bindUnbindPeripheralService(bind);
        }
    }

    private boolean isSwiftServicePersistedStateOn() {
        int loadFlag = SystemProperties.getInt(BLUETOOTH_FIRMWARE_LOAD_FLAG_PROPERTY,
                BLUETOOTH_FIRMWARE_LOAD_ORIGINAL);
        return loadFlag != BLUETOOTH_FIRMWARE_LOAD_ORIGINAL
                && loadFlag != BLUETOOTH_FIRMWARE_LOAD_ORIGINAL_NEXT_REBOOT;
    }

    private void bindUnbindPeripheralService(boolean bind) {
        try {
            ComponentName swiftComponentName =
                    ComponentName.unflattenFromString(SWIFT_COMPONENT_NAME);
            IBinder binder = ApiLayerService.getInstance().getApiLayer();
            IApiLayer iApiLayer = IApiLayer.Stub.asInterface(binder);
            iApiLayer.updatePersistentServiceConnection(swiftComponentName, bind);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }
}
