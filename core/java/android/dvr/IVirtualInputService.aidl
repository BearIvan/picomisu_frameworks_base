// Copyright 2026 Picomisu contributors
// SPDX-License-Identifier: Apache-2.0
// Reconstructed from the factory PICO OS 5.13.7 DEX by tools/reconstruct-pico-aidl.py;
// explicit ids reproduce the factory transaction codes (2, 3, 5-14, 16, 18).
package android.dvr;

import android.os.IBinder;

/** @hide */
interface IVirtualInputService {
    oneway void attachDevice(IBinder client, int deviceId, int mapperMask) = 1;
    int createDevice(IBinder client, int mapperMask) = 2;
    oneway void destoryDevice(int deviceId) = 4;
    oneway void touch(int deviceId, float x, float y, float pressure, int slot) = 5;
    oneway void singleTouch(int deviceId, float x, float y, int action) = 6;
    oneway void buttonState(int deviceId, int buttons) = 7;
    oneway void scroll(int deviceId, float x, float y) = 8;
    oneway void key(int deviceId, int keycode, int action) = 9;
    oneway void mouse(int deviceId, int btnState, int action, float relX, float relY) = 10;
    oneway void mouseClick(int deviceId, int btnState, int action) = 11;
    oneway void mouseMove(int deviceId, float relX, float relY) = 12;
    oneway void scale(int deviceId, float scaleFactor) = 13;
    oneway void attachInitDevice(IBinder client, int deviceId, int mapperMask, int width, int height) = 15;
    oneway void axis(int deviceId, int axis, float value) = 17;
}
