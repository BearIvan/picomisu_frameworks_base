/*
 * Copyright 2026 Picomisu contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package com.android.internal.app;

import org.json.JSONObject;

/**
 * One running 2D app of the PICO API layer "running 2D app" JSON array (factory PICO OS 5.13.7
 * framework com.android.internal.app.RunningAppInfo).
 *
 * @hide
 */
public class RunningAppInfo {
    protected static final String KEY_JSON_CLIENT_ID = "clientId";
    protected static final String KEY_JSON_COMPONENT_NAME = "cmp";
    protected static final String KEY_JSON_DISPLAY_ID = "displayId";
    protected static final String KEY_JSON_PACKAGE_NAME = "pkg";
    protected static final String KEY_JSON_TYPE = "type";
    protected static final String KEY_JSON_VISIBLE = "visible";
    public int clientId;
    public String componentName;
    public int displayId;
    public String packageName;
    public int type;
    public boolean visible;

    public RunningAppInfo(JSONObject object) {
        clientId = object.optInt(KEY_JSON_CLIENT_ID);
        displayId = object.optInt(KEY_JSON_DISPLAY_ID);
        type = object.optInt(KEY_JSON_TYPE);
        packageName = object.optString(KEY_JSON_PACKAGE_NAME);
        componentName = object.optString(KEY_JSON_COMPONENT_NAME);
        visible = object.optBoolean(KEY_JSON_VISIBLE);
    }

    @Override
    public String toString() {
        StringBuffer buffer = new StringBuffer();
        buffer.append("[displayId: ");
        buffer.append(displayId);
        buffer.append(", clientId: ");
        buffer.append(clientId);
        buffer.append(", type: ");
        buffer.append(type);
        buffer.append(", packageName: ");
        buffer.append(packageName);
        buffer.append(", componentName: ");
        buffer.append(componentName);
        buffer.append(", visible: ");
        buffer.append(visible);
        buffer.append("]");
        return buffer.toString();
    }
}
