// Copyright 2026 Picomisu contributors
// SPDX-License-Identifier: Apache-2.0
package com.android.server.inputmethod;

import android.content.ContentResolver;
import android.content.Context;
import android.hardware.display.DisplayManager;
import android.os.IBinder;
import android.os.Parcel;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemProperties;
import android.pico.utils.Features;
import android.provider.Settings;
import android.util.Slog;
import android.view.Display;

import com.android.internal.view.IInputMethodClient;
import com.android.server.api.ApiLayerService;
import com.android.server.wm.WindowManagerInternal;
import com.android.server.wm.WindowManagerService;
import com.pvr.IPvrManagerService;

/**
 * PICO input method manager service extension (factory PICO OS 5.13.7
 * com.android.server.inputmethod.ExtInputMethodManagerServiceImpl): IME display of 2D app
 * panels, IME visibility reports to the window manager, the API layer and SystemExt, and the
 * cleanup of clients of removed 2D app displays.
 */
public class ExtInputMethodManagerServiceImpl implements IExtInputMethodManagerService {
    private static final String DESCRIPTOR = "com.android.internal.view.IInputMethodClient";
    static final String TAG = "InputMethodManagerService";
    private InputMethodManagerService mBase;
    private IPvrManagerService mPvrManagerService;

    public ExtInputMethodManagerServiceImpl(InputMethodManagerService base) {
        mBase = base;
        InputMethodManagerService.sExtInstance = this;
    }

    /**
     * The IME of a 2D app display (listed by SystemExt in Settings.System "app_display_id_list")
     * runs on the SystemExt input method display ("ime_for_2d_app_display_id"); otherwise on the
     * default display.
     */
    private int computeImeDisplayIdForTargetInner(int displayId) {
        String data;
        try {
            Context context = mBase.mContext;
            ContentResolver cr = context.getContentResolver();
            int imeDisplayIdFor2dApp = Settings.System.getInt(cr, "ime_for_2d_app_display_id", -1);
            if (imeDisplayIdFor2dApp == -1) {
                return 0;
            }
            DisplayManager dm = (DisplayManager) context.getSystemService("display");
            Display display = dm.getDisplay(imeDisplayIdFor2dApp);
            if (display != null
                    && (data = Settings.System.getString(cr, "app_display_id_list")) != null) {
                String[] arr = data.split(";");
                for (String str : arr) {
                    if (Integer.parseInt(str) == displayId) {
                        return imeDisplayIdFor2dApp;
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return 0;
    }

    /** Tells SystemExt on which display the IME target window is (-1 when the IME hides). */
    @Override
    public void dispatchImeVisibleStatusToNS(int displayId) {
        if (!Features.isPvr2DEnabled()) {
            return;
        }
        try {
            IBinder ns = ServiceManager.getService("native_shell");
            if (ns != null && ns.isBinderAlive()) {
                Parcel data = Parcel.obtain();
                Parcel reply = Parcel.obtain();
                data.writeInterfaceToken("com.bytedance.IRemoteCallback");
                data.writeInt(displayId);
                ns.transact(400001, data, reply, IBinder.FLAG_ONEWAY);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * DisplayContent.removeImmediately (through ExtDisplayContentImpl): a second after a display
     * is removed, the client registered for it is removed (hiding the IME if it was shown for it)
     * and told to drop its cached InputMethodManager.
     */
    @Override
    public void onDisplayContentDestroy(final int displayId) {
        mBase.mHandler.postDelayed(() -> {
            synchronized (mBase.mMethodMap) {
                InputMethodManagerService.ClientState state = null;
                int numClients = mBase.mClients.size();
                for (int i = 0; i < numClients; i++) {
                    InputMethodManagerService.ClientState clientState = mBase.mClients.valueAt(i);
                    if (clientState != null && clientState.selfReportedDisplayId == displayId) {
                        state = clientState;
                    }
                }
                if (state == null) {
                    return;
                }
                if (mBase.mInputShown && mBase.mCurClient != null
                        && mBase.mCurClient.selfReportedDisplayId == displayId) {
                    Slog.w(TAG, "IME target display destroy, force hide current showing ime");
                    mBase.hideCurrentInputLocked(0, null);
                }
                Slog.w(TAG, "force remove ClientState by display removed [" + displayId + "]");
                onClientRemoved(state.client);
                mBase.removeClient(state.client);
            }
        }, 1000L);
    }

    private void onClientRemoved(IInputMethodClient client) {
        if (!Features.limitTheNumberOfDisplayCaches()) {
            return;
        }
        Parcel data = Parcel.obtain();
        Parcel reply = Parcel.obtain();
        try {
            data.writeInterfaceToken(DESCRIPTOR);
            client.asBinder().transact(10000, data, reply, IBinder.FLAG_ONEWAY);
        } catch (RemoteException e) {
            e.printStackTrace();
        } finally {
            reply.recycle();
            data.recycle();
        }
    }

    /** A locale change does not replace the PICO keyboard (or the Sogou car IME). */
    @Override
    public boolean disableResetDefaultIme(String curMethodId) {
        if (curMethodId != null) {
            if (curMethodId.contains("pico")
                    || curMethodId.contains("com.sohu.inputmethod.sogou.car")) {
                Slog.i(TAG, "avoid reset pico IME when locale changed, mCurMethodId="
                        + curMethodId);
                return true;
            }
            return false;
        }
        return false;
    }

    @Override
    public int computeImeDisplayIdForTarget(int displayId,
            InputMethodManagerService.ImeDisplayValidator checker) {
        if (Features.isPvr2DEnabled()) {
            return computeImeDisplayIdForTargetInner(displayId);
        }
        return SystemProperties.getInt("sys.pxr.im.displayid", 0);
    }

    /**
     * The IME is shown for the focused window: tell the window manager, the API layer and
     * SystemExt (display of the IME target).
     */
    @Override
    public void onShowCurrentInput(IBinder curFocusedWindow,
            WindowManagerInternal windowManagerInternal) {
        if (curFocusedWindow == null) {
            return;
        }
        WindowManagerService wms = (WindowManagerService) mBase.mIWindowManager;
        wms.getExt().notifyImeVisibleChanged(true);
        ApiLayerService.getInstance().updateImeShowingState(true);
        int windowDisplayId = windowManagerInternal.getDisplayIdForWindow(curFocusedWindow);
        dispatchImeVisibleStatusToNS(windowDisplayId);
    }

    @Override
    public void onHideCurrentInput() {
        if (InputMethodManagerService.DEBUG) {
            Slog.d(TAG, "hideCurrentInputLocked: mCurToken=" + mBase.mCurToken);
        }
        WindowManagerService wms = (WindowManagerService) mBase.mIWindowManager;
        wms.getExt().notifyImeVisibleChanged(false);
        ApiLayerService.getInstance().updateImeShowingState(false);
        dispatchImeVisibleStatusToNS(-1);
    }

    /** Unused on the factory as well. */
    private void sendPvrManagerMessage(String key, String content) {
        try {
            if (mPvrManagerService == null || mPvrManagerService.asBinder() == null
                    || !mPvrManagerService.asBinder().isBinderAlive()) {
                if (ServiceManager.getService("pvr_manager") != null) {
                    mPvrManagerService = IPvrManagerService.Stub.asInterface(
                            ServiceManager.getService("pvr_manager"));
                } else {
                    Slog.w(TAG, "pvr_manager has not been added to ServiceManager,do nothing.");
                }
            }
            if (mPvrManagerService != null) {
                mPvrManagerService.sendPvrMessages(key, content);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /** A call from an unknown client (e.g. of a removed 2D app display) is ignored. */
    @Override
    public boolean disableCheckClientState() {
        return true;
    }

    @Override
    public void updateCurrentFocusedWindow(IBinder currentFocusWin) {
        WindowManagerService wms = (WindowManagerService) mBase.mIWindowManager;
        wms.getExt().notifyImeTargetChanged(currentFocusWin);
    }
}
