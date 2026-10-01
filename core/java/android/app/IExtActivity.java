// Copyright 2026 Picomisu contributors
// SPDX-License-Identifier: Apache-2.0
package android.app;

import android.content.Intent;

import com.pico.util.IExtBase;

/**
 * PICO activity extension (factory PICO OS 5.13.7 android.app.IExtActivity).
 * @hide
 */
public interface IExtActivity extends IExtBase {
    void onFinish();

    void onRequestPermissionsFromFragmentCalled(Intent intent, String[] permissionDescriptions);

    void onResumeCalled();

    void requestPermissionsCalled(Intent intent, String[] permissionDescriptions);
}
