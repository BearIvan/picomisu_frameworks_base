// Copyright 2026 Picomisu contributors
// SPDX-License-Identifier: Apache-2.0
package android.pico.ns;

/**
 * Client types, parameter keys and layout keys of the PICO native shell ("NS") window
 * service.
 * @hide
 */
public class NsConstants {
    public static final int TYPE_NONE = 0;

    public static final int TYPE_KEYBOARD = 1001;
    public static final int TYPE_SYSTEM_DIALOG = 1002;
    public static final int TYPE_SYSTEM_TOAST = 1003;
    public static final int TYPE_DIALOG = 1004;
    public static final int TYPE_KEYBOARD_LEFT = 1005;
    public static final int TYPE_KEYBOARD_RIGHT = 1006;

    static final int TYPE_NEAR_BASE = 2000;
    public static final int TYPE_NEAR_PANEL_DIALOG = TYPE_NEAR_BASE + 1;
    public static final int TYPE_NEAR_PANEL = TYPE_NEAR_BASE + 2;
    public static final int TYPE_NEAR_DOCK = TYPE_NEAR_BASE + 3;

    static final int TYPE_FAR_BASE = 3000;
    public static final int TYPE_FAR_PANEL_DIALOG = TYPE_FAR_BASE + 1;
    public static final int TYPE_FAR_PANEL = TYPE_FAR_BASE + 2;
    public static final int TYPE_FAR_PANEL_CAPTION_BAR = TYPE_FAR_BASE + 3;

    public static final String PARAM_TYPE = "type";
    public static final String PARAM_SUB_TYPE = "sub_type";
    public static final String PARAM_NAME = "name";
    public static final String PARAM_PACKAGE_NAME = "package_name";
    public static final String PARAM_COMPONENT_NAME = "component_name";
    public static final String PARAM_CALLBACK = "callback";
    public static final String PARAM_FLAGS = "flags";
    public static final String PARAM_DENSITY = "density";
    public static final String PARAM_PARENT_ID = "parent_id";
    public static final String PARAM_PARENT_DISPLAY_ID = "parent_display_id";
    public static final String PARAM_SHAPE_TYPE = "shape_type";
    public static final String PARAM_COMPOSE_WITH_DISPLAY = "compose_with_display";

    public static final String LAYOUT_SIZE_W = "size.w";
    public static final String LAYOUT_SIZE_H = "size.h";
    public static final String LAYOUT_POSITION_X = "position.x";
    public static final String LAYOUT_POSITION_Y = "position.y";
    public static final String LAYOUT_POSITION_Z = "position.z";
    public static final String LAYOUT_ROTATION_X = "rotation.x";
    public static final String LAYOUT_ROTATION_Y = "rotation.y";
    public static final String LAYOUT_ROTATION_Z = "rotation.z";
}
