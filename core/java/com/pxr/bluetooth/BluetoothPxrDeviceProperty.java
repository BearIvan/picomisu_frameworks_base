// Copyright 2026 Picomisu contributors
// SPDX-License-Identifier: Apache-2.0
package com.pxr.bluetooth;

import android.bluetooth.BluetoothDevice;
import android.os.Parcel;
import android.os.Parcelable;

/**
 * Snapshot of a PICO (PXR) Bluetooth peripheral: the device, its PXR device type, and its
 * connection and bond states.
 *
 * @hide
 */
public class BluetoothPxrDeviceProperty implements Parcelable {
    private BluetoothDevice mDevice;
    private int mDeviceType = 0;
    private int mConnectionState = 0;
    private int mBondState = BluetoothDevice.BOND_NONE;

    public BluetoothPxrDeviceProperty(BluetoothDevice device) {
        mDevice = device;
    }

    public BluetoothPxrDeviceProperty(BluetoothDevice device, int deviceType) {
        mDevice = device;
        mDeviceType = deviceType;
    }

    protected BluetoothPxrDeviceProperty(Parcel in) {
        readFromParcel(in);
    }

    public static final Parcelable.Creator<BluetoothPxrDeviceProperty> CREATOR =
            new Parcelable.Creator<BluetoothPxrDeviceProperty>() {
                @Override
                public BluetoothPxrDeviceProperty createFromParcel(Parcel in) {
                    return new BluetoothPxrDeviceProperty(in);
                }

                @Override
                public BluetoothPxrDeviceProperty[] newArray(int size) {
                    return new BluetoothPxrDeviceProperty[size];
                }
            };

    private void readFromParcel(Parcel in) {
        mDevice = (BluetoothDevice) in.readParcelable(BluetoothDevice.class.getClassLoader());
        mDeviceType = in.readInt();
        mConnectionState = in.readInt();
        mBondState = in.readInt();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeParcelable(mDevice, 0);
        dest.writeInt(mDeviceType);
        dest.writeInt(mConnectionState);
        dest.writeInt(mBondState);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public BluetoothDevice getDevice() {
        return mDevice;
    }

    public int getPxrDeviceType() {
        return mDeviceType;
    }

    public void setPxrDeviceType(int type) {
        mDeviceType = type;
    }

    public int getPxrDeviceConnectionState() {
        return mConnectionState;
    }

    public void setPxrDeviceConnectionState(int state) {
        mConnectionState = state;
    }

    public int getPxrDeviceBondState() {
        return mBondState;
    }

    public void setPxrDeviceBondState(int state) {
        mBondState = state;
    }

    private String getDescriptorTypeString(int type) {
        String ret = "TYPE_UNKNOWN";
        switch (type) {
            case 1:
                ret = "TYPE_AUDIO";
                break;
            case 2:
                ret = "TYPE_KEYBOARD";
                break;
            case 3:
                ret = "TYPE_MOUSE";
                break;
            case 4:
                ret = "TYPE_SWIFT";
                break;
        }
        return ret;
    }

    private String getDescriptorConnectionString(int type) {
        String ret = "STATE_UNUSED";
        switch (type) {
            case -1:
                ret = "STATE_UNUSED";
                break;
            case 0:
                ret = "STATE_DISCONNECTED";
                break;
            case 1:
                ret = "STATE_CONNECTING";
                break;
            case 2:
                ret = "STATE_CONNECTED";
                break;
            case 3:
                ret = "STATE_DISCONNECTING";
                break;
        }
        return ret;
    }

    private String getDescriptorBondString(int type) {
        String ret = "BOND_UNUSED";
        switch (type) {
            case -1:
                ret = "BOND_UNUSED";
                break;
            case BluetoothDevice.BOND_NONE:
                ret = "BOND_NONE";
                break;
            case BluetoothDevice.BOND_BONDING:
                ret = "BOND_BONDING";
                break;
            case BluetoothDevice.BOND_BONDED:
                ret = "BOND_BONDED";
                break;
        }
        return ret;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("BluetoothPxrDeviceProperty {");
        builder.append(" Bluetooth Address =");
        builder.append(mDevice.getAddress());
        builder.append(", BluetoothName = ");
        builder.append(mDevice.getName());
        builder.append(", PxrBluetoothType = ");
        builder.append(getDescriptorTypeString(mDeviceType));
        builder.append(", ConnectionState = ");
        builder.append(getDescriptorConnectionString(mConnectionState));
        builder.append(", BondState = ");
        builder.append(getDescriptorBondString(mBondState));
        builder.append(" }");
        return builder.toString();
    }
}
