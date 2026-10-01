/*
 * Copyright 2026 Picomisu contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package com.android.server.api;

import android.app.ActivityManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Parcelable;
import android.os.RemoteCallback;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Slog;
import android.view.KeyEvent;

import com.android.internal.app.RunningAppInfo;
import com.android.internal.app.ScenesStateListener;
import com.android.server.wm.ActivityTaskManagerService;
import com.android.server.wm.SystemExt;
import com.pico.api.app.IApiLayer;
import com.pico.api.app.IAppSession;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * PICO API layer of system_server (factory PICO OS 5.13.7 services.jar
 * com.android.server.api.ApiLayerService). Clients (the PICO SDK copy inside VRShell, XRShell,
 * SystemExt and other PICO apps) obtain {@link IApiLayer} through IWindowManager transaction
 * {@link #CODE_GET_API_LAYER} and register an {@link IAppSession} to receive seethrough, 3D/2D
 * app, IME, screen, power, key and activity-starting events (see {@link MsgDispatcher}).
 * In-process {@link ScenesStateListener}s (the factory ExtWindowManagerServiceImpl visible app
 * callback) get the seethrough, 3D app and running 2D app changes on the dispatcher thread.
 *
 * Not ported yet: updatePersistentServiceConnection, which needs the factory
 * IExtActivityTaskManagerService.updatePersistentConnection (AppPersistentConnection) of the
 * window manager layer.
 *
 * @hide
 */
public class ApiLayerService {
    public static final String TAG = "ApiLayer";
    /** IWindowManager transaction returning the API layer binder (factory ExtWindowManagerServiceImpl). */
    public static final int CODE_GET_API_LAYER = 10003;
    public static final int POWER_STATE_GO_TO_SLEEP = 1;
    public static final int POWER_STATE_WAKE_UP = 2;

    private static final int SETTINGS_TYPE_GLOBAL = 1;
    private static final int SETTINGS_TYPE_SYSTEM = 2;
    private static final int SETTINGS_TYPE_SECURE = 3;
    private static final boolean DEBUG = true;
    private static final String SHOW_GLOBAL_UI_PERMISSION =
            "com.picovr.globalui.permission.SHOW_GLOBAL_UI";

    private static final ApiLayerService sSelf = new ApiLayerService();

    private ActivityTaskManagerService mAtms;
    private Context mContext;
    private ComponentName mTopAppOnDefaultDisplay;
    private final MsgDispatcher mDispatcher = new MsgDispatcher();
    private final ApiLayerSettingsObserverProxy mApiLayerSettingsObserverProxy =
            new ApiLayerSettingsObserverProxy();
    private final ApiLayerBroadcastProxy mApiLayerBroadcastProxy = new ApiLayerBroadcastProxy();
    private final List<ScenesStateListener> mScenesStateListeners = new ArrayList<>();
    private boolean mImeShowing = false;
    private boolean mIsScreenOn = false;
    private int mSeethroughState = -1;
    private String mShowing3dApp = null;
    private int mXrRuntimeDisplayState = -1;
    private String mRunning2dAppData = null;
    private int mPowerState = POWER_STATE_WAKE_UP;
    private final ApiLayerServiceInner mInner = new ApiLayerServiceInner();

    public static ApiLayerService getInstance() {
        return sSelf;
    }

    private ApiLayerService() {
    }

    public void setActivityTaskManagerService(Context context, ActivityTaskManagerService atms) {
        mAtms = atms;
        mContext = context;
    }

    public IBinder getApiLayer() {
        return mInner.asBinder();
    }

    private class ApiLayerServiceInner extends IApiLayer.Stub {
        @Override
        public boolean getScreenState() {
            synchronized (sSelf) {
                return mIsScreenOn;
            }
        }

        @Override
        public boolean isInputMethodShowing() {
            synchronized (sSelf) {
                return mImeShowing;
            }
        }

        @Override
        public int getSeethroughState() {
            synchronized (sSelf) {
                return mSeethroughState;
            }
        }

        @Override
        public String getShowing3dApp() {
            synchronized (sSelf) {
                return mShowing3dApp;
            }
        }

        @Override
        public int getXrRuntimeDisplayState() {
            synchronized (sSelf) {
                return mXrRuntimeDisplayState;
            }
        }

        @Override
        public String getRunning2dAppData() {
            synchronized (sSelf) {
                return mRunning2dAppData;
            }
        }

        @Override
        public ComponentName getTopAppOnDefaultDisplay() {
            synchronized (sSelf) {
                return mTopAppOnDefaultDisplay;
            }
        }

        @Override
        public void registerAppSession(IBinder session, int flags) {
            registerAppSessionCallbackLocked(IAppSession.Stub.asInterface(session), flags);
        }

        @Override
        public void unregisterAppSession(IBinder session, int flags) {
            unregisterAppSessionCallbackLocked(IAppSession.Stub.asInterface(session), flags);
        }

        @Override
        public void updateSeethroughState(int state) {
            updateSeethroughStateLocked(state);
        }

        @Override
        public void updateRunning2dAppList(String running2dAppData) {
            updateRunning2dAppListLocked(running2dAppData);
        }

        @Override
        public void updateShowing3dApp(String pkg) {
            updateShowing3dAppLocked(pkg);
        }

        @Override
        public void updateXrRuntimeState(Bundle state) {
            updateXrRuntimeStateLocked(state);
        }

        @Override
        public void updatePersistentServiceConnection(ComponentName componentName,
                boolean connect) {
            // Factory: mAtms.getExt().updatePersistentConnection(); the ATMS Ext layer is not
            // ported yet.
            Slog.w(TAG, "updatePersistentServiceConnection not supported yet: " + componentName
                    + ", connect " + connect);
        }

        @Override
        public String getTopAppOnDefaultDisplayForNative() {
            synchronized (sSelf) {
                return mTopAppOnDefaultDisplay != null
                        ? mTopAppOnDefaultDisplay.flattenToShortString() : "";
            }
        }

        @Override
        public int getPowerState() {
            synchronized (sSelf) {
                return mPowerState;
            }
        }

        @Override
        public int getSettingsInt(int type, String key, int def) {
            final long origId = Binder.clearCallingIdentity();
            try {
                Slog.w(TAG, "getSettingsInt type [" + type + "], key [" + key + "], def [" + def
                        + "]");
                switch (type) {
                    case SETTINGS_TYPE_GLOBAL:
                        return Settings.Global.getInt(mContext.getContentResolver(), key, def);
                    case SETTINGS_TYPE_SYSTEM:
                        return Settings.System.getInt(mContext.getContentResolver(), key, def);
                    case SETTINGS_TYPE_SECURE:
                        return Settings.Secure.getInt(mContext.getContentResolver(), key, def);
                    default:
                        return -1;
                }
            } catch (Exception e) {
                e.printStackTrace();
                return -1;
            } finally {
                Binder.restoreCallingIdentity(origId);
            }
        }

        @Override
        public void putSettingsInt(int type, String key, int value) {
            final long origId = Binder.clearCallingIdentity();
            try {
                Slog.w(TAG, "putSettingsInt type [" + type + "], key [" + key + "], value ["
                        + value + "]");
                if (type == SETTINGS_TYPE_GLOBAL) {
                    Settings.Global.putInt(mContext.getContentResolver(), key, value);
                } else if (type == SETTINGS_TYPE_SYSTEM) {
                    Settings.System.putInt(mContext.getContentResolver(), key, value);
                } else if (type == SETTINGS_TYPE_SECURE) {
                    Settings.Secure.putInt(mContext.getContentResolver(), key, value);
                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                Binder.restoreCallingIdentity(origId);
            }
        }

        @Override
        public String getSettingsString(int type, String key) {
            final long origId = Binder.clearCallingIdentity();
            try {
                Slog.w(TAG, "getSettingsString type [" + type + "], key [" + key + "]");
                switch (type) {
                    case SETTINGS_TYPE_GLOBAL:
                        return Settings.Global.getString(mContext.getContentResolver(), key);
                    case SETTINGS_TYPE_SYSTEM:
                        return Settings.System.getString(mContext.getContentResolver(), key);
                    case SETTINGS_TYPE_SECURE:
                        return Settings.Secure.getString(mContext.getContentResolver(), key);
                    default:
                        return null;
                }
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            } finally {
                Binder.restoreCallingIdentity(origId);
            }
        }

        @Override
        public void putSettingsString(int type, String key, String value) {
            final long origId = Binder.clearCallingIdentity();
            try {
                Slog.w(TAG, "putSettingsString type [" + type + "], key [" + key + "], value["
                        + value + "]");
                if (type == SETTINGS_TYPE_GLOBAL) {
                    Settings.Global.putString(mContext.getContentResolver(), key, value);
                } else if (type == SETTINGS_TYPE_SYSTEM) {
                    Settings.System.putString(mContext.getContentResolver(), key, value);
                } else if (type == SETTINGS_TYPE_SECURE) {
                    Settings.Secure.putString(mContext.getContentResolver(), key, value);
                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                Binder.restoreCallingIdentity(origId);
            }
        }

        @Override
        public void registerSettingsObserverForNative(IBinder session, int settingType,
                String name, int valueType) {
            final long origId = Binder.clearCallingIdentity();
            try {
                Slog.w(TAG, "registerSettingsObserverForNative settingType [" + settingType
                        + "], name [" + name + "], valueType[" + valueType + "]");
                mApiLayerSettingsObserverProxy.registerSettingsObserver(mContext,
                        IAppSession.Stub.asInterface(session), settingType, name, valueType,
                        mDispatcher.getHandler());
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                Binder.restoreCallingIdentity(origId);
            }
        }

        @Override
        public void unregisterSettingsObserverForNative(IBinder session, int settingType,
                String name) {
            final long origId = Binder.clearCallingIdentity();
            try {
                Slog.w(TAG, "unregisterSettingsObserverForNative settingType [" + settingType
                        + "], name [" + name + "]");
                mApiLayerSettingsObserverProxy.unregisterSettingsObserver(mContext,
                        IAppSession.Stub.asInterface(session), settingType, name);
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                Binder.restoreCallingIdentity(origId);
            }
        }

        @Override
        public void startActivityForNative(String action, String category, String pkg,
                String cmp, int flags) {
            final long origId = Binder.clearCallingIdentity();
            try {
                Slog.w(TAG, "startActivityForNative pid " + Binder.getCallingPid() + ", uid "
                        + Binder.getCallingUid() + ", action [" + action + "], pkg [" + pkg
                        + "], cmp [" + cmp + "], flags [" + flags + "]");
                final Intent intent = new Intent();
                if (!TextUtils.isEmpty(action)) {
                    intent.setAction(action);
                }
                if (!TextUtils.isEmpty(category)) {
                    intent.addCategory(category);
                }
                intent.setComponent(new ComponentName(pkg, cmp));
                intent.setFlags(flags);
                mContext.startActivity(intent);
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                Binder.restoreCallingIdentity(origId);
            }
        }

        @Override
        public void startServiceForNative(String action, String category, String pkg,
                String cmp, int flags) {
            final long origId = Binder.clearCallingIdentity();
            try {
                Slog.w(TAG, "startServiceForNative pid " + Binder.getCallingPid() + ", uid "
                        + Binder.getCallingUid() + ", action [" + action + "], pkg [" + pkg
                        + "], cmp [" + cmp + "], flags [" + flags + "]");
                final Intent intent = new Intent();
                if (!TextUtils.isEmpty(action)) {
                    intent.setAction(action);
                }
                if (!TextUtils.isEmpty(category)) {
                    intent.addCategory(category);
                }
                if (TextUtils.isEmpty(cmp)) {
                    intent.setPackage(pkg);
                } else {
                    intent.setComponent(new ComponentName(pkg, cmp));
                }
                intent.setFlags(flags);
                mContext.startService(intent);
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                Binder.restoreCallingIdentity(origId);
            }
        }

        @Override
        public void registerBroadcastReceiverForNative(IBinder session, String action,
                String dataScheme) {
            final long origId = Binder.clearCallingIdentity();
            try {
                Slog.w(TAG, "registerBroadcastReceiverForNative action [" + action
                        + "], dataScheme [" + dataScheme + "]");
                mApiLayerBroadcastProxy.registerBroadcastReceiver(mContext,
                        IAppSession.Stub.asInterface(session), action, dataScheme,
                        mDispatcher.getHandler());
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                Binder.restoreCallingIdentity(origId);
            }
        }

        @Override
        public void unregisterBroadcastReceiverForNative(IBinder session, String action) {
            final long origId = Binder.clearCallingIdentity();
            try {
                mApiLayerBroadcastProxy.unregisterBroadcastReceiver(mContext,
                        IAppSession.Stub.asInterface(session), action);
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                Binder.restoreCallingIdentity(origId);
            }
        }

        @Override
        public void requestShowAnrDialog(String pkg, RemoteCallback callback) {
            if (mContext.checkCallingPermission(SHOW_GLOBAL_UI_PERMISSION) != 0) {
                Slog.w(TAG, "java.lang.SecurityException: Need permission "
                        + SHOW_GLOBAL_UI_PERMISSION);
                return;
            }
            Slog.w(TAG, "requestShowAnrDialog [" + pkg + "], callback " + callback);
            final long ident = Binder.clearCallingIdentity();
            try {
                final Intent intent = new Intent();
                intent.setAction("picovr.globalui");
                intent.setComponent(new ComponentName(SystemExt.sCurrentPkg,
                        "com.pvr.vrdisplay.GlobalUIService"));
                intent.putExtra("action_type", 107);
                intent.putExtra("pkg", pkg);
                intent.putExtra("callback", (Parcelable) callback);
                mContext.startService(intent);
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                Binder.restoreCallingIdentity(ident);
            }
        }
    }

    public void updatePowerState(int state) {
        synchronized (sSelf) {
            if (mPowerState != state) {
                mPowerState = state;
                mDispatcher.dispatchPowerStateChanged(state);
            }
        }
    }

    public void updateScreenState(boolean on) {
        synchronized (sSelf) {
            if (mIsScreenOn != on) {
                mIsScreenOn = on;
                mDispatcher.dispatchScreenState(on);
            }
        }
    }

    public void updateImeShowingState(boolean showing) {
        synchronized (sSelf) {
            if (mImeShowing != showing) {
                mImeShowing = showing;
                mDispatcher.dispatchImeState(showing);
            }
        }
    }

    private void registerAppSessionCallbackLocked(IAppSession session, int listeningFlags) {
        if (session == null) {
            return;
        }
        synchronized (sSelf) {
            mDispatcher.registerAppSession(session, listeningFlags);
        }
    }

    private void unregisterAppSessionCallbackLocked(IAppSession session, int listeningFlags) {
        if (session == null) {
            return;
        }
        synchronized (sSelf) {
            mDispatcher.unregisterAppSession(session, listeningFlags);
        }
    }

    public void updateTopAppOnDefaultDisplay(ActivityManager.RunningTaskInfo taskInfo) {
        ComponentName componentName = null;
        if (taskInfo != null) {
            if (taskInfo.baseActivity != null) {
                componentName = taskInfo.baseActivity;
            } else if (taskInfo.topActivity != null) {
                componentName = taskInfo.topActivity;
            }
        }
        mTopAppOnDefaultDisplay = componentName;
    }

    public void onKeyEvent(KeyEvent keyEvent) {
        mDispatcher.dispatchKeyEvent(keyEvent.copy());
    }

    public void onActivityStarting(ActivityInfo aInfo) {
        mDispatcher.dispatchActivityStarting(aInfo);
    }

    /** In-process listener of the scene state (factory ExtWindowManagerServiceImpl). */
    public void registerScenesStateListener(ScenesStateListener listener) {
        if (listener == null) {
            return;
        }
        synchronized (mScenesStateListeners) {
            mScenesStateListeners.remove(listener);
            mScenesStateListeners.add(listener);
        }
    }

    private void dispatch2dRunningAppChanged(String running2dAppData) {
        List<RunningAppInfo> visible2dAppList = parseToRunningAppList(running2dAppData);
        synchronized (mScenesStateListeners) {
            for (ScenesStateListener listener : mScenesStateListeners) {
                listener.onRunning2dAppChanged(visible2dAppList);
            }
        }
    }

    private List<RunningAppInfo> parseToRunningAppList(String running2dAppData) {
        List<RunningAppInfo> runningApps = new ArrayList<>();
        if (TextUtils.isEmpty(running2dAppData)) {
            return runningApps;
        }
        try {
            JSONArray array = new JSONArray(running2dAppData);
            int length = array.length();
            for (int i = 0; i < length; i++) {
                JSONObject object = array.optJSONObject(i);
                if (object != null) {
                    RunningAppInfo runningApp = new RunningAppInfo(object);
                    runningApps.add(runningApp);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return runningApps;
    }

    private void dispatch3dAppDisplayStateChanged(String showing3dApp,
            int xrRuntimeDisplayState) {
        synchronized (mScenesStateListeners) {
            for (ScenesStateListener listener : mScenesStateListeners) {
                listener.on3dAppDisplayStateChanged(showing3dApp, xrRuntimeDisplayState);
            }
        }
    }

    private void dispatchSeethroughState(int seethroughState) {
        synchronized (mScenesStateListeners) {
            for (ScenesStateListener listener : mScenesStateListeners) {
                listener.onSeethroughStateChanged(seethroughState);
            }
        }
    }

    private void updateSeethroughStateLocked(final int state) {
        synchronized (sSelf) {
            if (mSeethroughState != state) {
                mSeethroughState = state;
                mDispatcher.dispatchSeethroughState(state);
                mDispatcher.getHandler().post(() -> dispatchSeethroughState(state));
            }
        }
    }

    private void updateRunning2dAppListLocked(final String running2dAppData) {
        synchronized (sSelf) {
            if (mRunning2dAppData == null || (mRunning2dAppData != null
                    && !mRunning2dAppData.equals(running2dAppData))) {
                mRunning2dAppData = running2dAppData;
                mDispatcher.dispatchRunning2dApp(running2dAppData);
                mDispatcher.getHandler().post(
                        () -> dispatch2dRunningAppChanged(running2dAppData));
            }
        }
    }

    private void updateShowing3dAppLocked(String pkg) {
        synchronized (sSelf) {
            mShowing3dApp = pkg;
            mDispatcher.dispatchShowing3dApp(pkg);
        }
    }

    private void updateXrRuntimeStateLocked(Bundle state) {
        synchronized (sSelf) {
            final String showing3dApp = state.getString("showing_3d_app", null);
            final int xrRuntimeState = state.getInt("runtime_state", -1);
            Slog.w(TAG, "showing3dApp [" + showing3dApp + "], xrRuntimeState [" + xrRuntimeState
                    + "]");
            mShowing3dApp = showing3dApp;
            mXrRuntimeDisplayState = xrRuntimeState;
            mDispatcher.dispatchXrRuntimeState(state);
            mDispatcher.getHandler().post(
                    () -> dispatch3dAppDisplayStateChanged(showing3dApp, xrRuntimeState));
        }
    }
}
