// Copyright 2026 Picomisu contributors
// SPDX-License-Identifier: Apache-2.0

package android.app;

import android.dvr.IVirtualInputService;
import android.graphics.Bitmap;
import android.os.Binder;
import android.os.IBinder;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.pc.ISmtPCManager;
import android.util.Log;
import android.view.MotionEvent;

/**
 * Smartisan PC mode utilities: PC mode state (always off in the factory base implementation)
 * and the client of the "virtual_input" service. Reconstructed from the PICO OS 5.13.7 factory
 * framework.
 *
 * @hide
 */
public class SmtPCUtilsSmtBase {
    public static final int PC_MODE_NONE = 0;
    public static final int PC_MODE_ON_MAIN_DISPLAY = 1;
    public static final int PC_MODE_ON_EXTEND_DISPLAY = 2;

    public static final int WINDOW_MODE_UNKNOWN = -1;
    public static final int WINDOW_MODE_PORTRAIT = 0;
    public static final int WINDOW_MODE_LANDSCAPE = 1;
    public static final int WINDOW_MODE_FULLSCREEN = 4;

    public static final int TOGGLE_RECENT_DEFAULT = 0;
    public static final int TOGGLE_RECENT_OPEN = 1;
    public static final int TOGGLE_RECENT_CLOSE = 2;

    public static final String SCREEN_SAVERS_PKG = "com.smartisanos.screensaver";

    public static final int DVR_VIRTUAL_TOUCHPAD_ID = 1;
    public static final int DVR_VIRTUAL_AIRMOUSE_ID = 2;
    public static final int DVR_VIRTUAL_WIRELESS_ID = 3;
    public static final int DVR_VIRTUAL_CAMERA_ID = 4;
    public static final int DVR_VIRTUAL_KEYMAPPING = 5;

    public static final int MAPPER_KEYBOARD_MASK = 1 << 0;
    public static final int MAPPER_TOUCH_MASK = 1 << 1;
    public static final int MAPPER_CURSOR_MASK = 1 << 2;
    public static final int MAPPER_TOUCHPAD_MASK = 1 << 3;
    public static final int MAPPER_ABSOLUTE_CURSOR_MASK = 1 << 4;

    private static ISmtPCManager sService;

    public static ISmtPCManager getSmtPCManager() {
        return null;
    }

    public static final boolean isValidExtDisplayId(int displayId) {
        return false;
    }

    public static int getExtDisplayId() {
        return -1;
    }

    public static boolean isPcCastModeInServer() {
        return false;
    }

    public static Bitmap getDisplayBitmap(int displayId, int width, int height, int minLayer,
            int maxLayer, int[] retValue) {
        return null;
    }

    private static IVirtualInputService mVirtualInputService;
    private static IBinder mVirtualInputClient = new Binder();

    protected static IVirtualInputService getVirtualInputManager() {
        if (mVirtualInputService == null) {
            synchronized (SmtPCUtilsSmtBase.class) {
                if (mVirtualInputService == null) {
                    try {
                        IBinder binder = ServiceManager.getService("virtual_input");
                        if (binder != null) {
                            mVirtualInputService = IVirtualInputService.Stub.asInterface(binder);
                            if (mVirtualInputService != null) {
                                try {
                                    mVirtualInputService.asBinder().linkToDeath(
                                            () -> mVirtualInputService = null, 0);
                                } catch (RemoteException e) {
                                    Log.e("VirtualInput", e.getMessage());
                                }
                            }
                        }
                    } catch (Exception e) {
                        Log.e("VirtualInput", e.getMessage());
                    }
                }
            }
        }
        return mVirtualInputService;
    }

    public static void attachDevice(int deviceId, int mapperMask) {
        IVirtualInputService service = getVirtualInputManager();
        if (service == null) {
            return;
        }
        try {
            service.attachDevice(mVirtualInputClient, deviceId, mapperMask);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static int createDevice(int mapperMask) {
        IVirtualInputService service = getVirtualInputManager();
        if (service == null) {
            return -1;
        }
        try {
            return service.createDevice(mVirtualInputClient, mapperMask);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return -1;
    }

    public static void attachInitDevice(int deviceId, int mapperMask, int width, int height) {
        IVirtualInputService service = getVirtualInputManager();
        if (service == null) {
            return;
        }
        try {
            service.attachInitDevice(mVirtualInputClient, deviceId, mapperMask, width, height);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void destoryDevice(int deviceId) {
        IVirtualInputService service = getVirtualInputManager();
        if (service == null) {
            return;
        }
        try {
            service.destoryDevice(deviceId);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void touch(int deviceId, float x, float y, int action, int slot) {
        IVirtualInputService service = getVirtualInputManager();
        if (service == null) {
            return;
        }
        float pressure = (action == MotionEvent.ACTION_DOWN || action == MotionEvent.ACTION_MOVE)
                ? 1.0f : 0.0f;
        try {
            service.touch(deviceId, x, y, pressure, slot);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void singleTouch(int deviceId, float x, float y, int action) {
        IVirtualInputService service = getVirtualInputManager();
        if (service == null) {
            return;
        }
        try {
            service.singleTouch(deviceId, x, y, action);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void buttonState(int deviceId, int buttons) {
        IVirtualInputService service = getVirtualInputManager();
        if (service == null) {
            return;
        }
        try {
            service.buttonState(deviceId, buttons);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void scroll(int deviceId, float x, float y) {
        IVirtualInputService service = getVirtualInputManager();
        if (service == null) {
            return;
        }
        try {
            service.scroll(deviceId, x, y);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void key(int deviceId, int keycode, int action) {
        IVirtualInputService service = getVirtualInputManager();
        if (service == null) {
            return;
        }
        try {
            service.key(deviceId, keycode, action);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void mouse(int deviceId, int btnState, int action, float x, float y) {
        IVirtualInputService service = getVirtualInputManager();
        if (service == null) {
            return;
        }
        try {
            service.mouse(deviceId, btnState, action, x, y);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void mouse(int deviceId, int btnState, int action) {
        IVirtualInputService service = getVirtualInputManager();
        if (service == null) {
            return;
        }
        try {
            service.mouseClick(deviceId, btnState, action);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void mouse(int deviceId, float x, float y) {
        IVirtualInputService service = getVirtualInputManager();
        if (service == null) {
            return;
        }
        try {
            service.mouseMove(deviceId, x, y);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void scale(int deviceId, float scaleFactor) {
        IVirtualInputService service = getVirtualInputManager();
        if (service == null) {
            return;
        }
        try {
            service.scale(deviceId, scaleFactor);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void axis(int deviceId, int axis, float value) {
        IVirtualInputService service = getVirtualInputManager();
        if (service == null) {
            return;
        }
        try {
            service.axis(deviceId, axis, value);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
