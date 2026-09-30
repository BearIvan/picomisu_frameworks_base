// Copyright 2026 Picomisu contributors
// SPDX-License-Identifier: Apache-2.0

package android.app;

/**
 * Base {@link IDataListener} of {@link SceneInfoManager} clients. Reconstructed from the PICO OS
 * 5.13.7 factory framework.
 *
 * @hide
 */
public class DataListener extends IDataListener.Stub {
    private String mClsName = "";

    public DataListener(String clsName) {
        mClsName = clsName;
    }

    public String getClsName() {
        return mClsName;
    }

    @Override
    public void onInfoChange(int type, int event, SceneData data) {
    }

    @Override
    public void onServerDied() {
    }
}
