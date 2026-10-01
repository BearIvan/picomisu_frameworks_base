// Copyright 2026 Picomisu contributors
// SPDX-License-Identifier: Apache-2.0
package android.os;

import android.annotation.UnsupportedAppUsage;
import android.util.Singleton;
import android.util.Slog;

/**
 * Client of the Smartisan power advisor service "poweradvisor" (factory PICO OS 5.13.7
 * android.os.PowerAdvisor).
 * @hide
 */
public class PowerAdvisor {
    private static final String TAG = "PowerAdvisor";
    private static PowerAdvisor instance;

    private PowerAdvisor() {
    }

    public static PowerAdvisor getInstance() {
        if (instance == null) {
            synchronized (PowerAdvisor.class) {
                if (instance == null) {
                    instance = new PowerAdvisor();
                }
            }
        }
        return instance;
    }

    public void notePowerSceneState(String pkgName, String mainScene, String subScene,
            int sceneState, String payload) {
        try {
            getService().notePowerSceneState(pkgName, mainScene, subScene, sceneState, payload);
        } catch (RemoteException ex) {
            throw ex.rethrowFromSystemServer();
        } catch (Exception e) {
            Slog.w(TAG, "notePowerSceneState not found" + e);
        }
    }

    @UnsupportedAppUsage
    private static IPowerAdvisor getService() {
        return IPowerAdvisorSingleton.get();
    }

    @UnsupportedAppUsage
    private static final Singleton<IPowerAdvisor> IPowerAdvisorSingleton =
            new Singleton<IPowerAdvisor>() {
                @Override
                protected IPowerAdvisor create() {
                    final IBinder b = ServiceManager.getService("poweradvisor");
                    final IPowerAdvisor powerAdvisor = IPowerAdvisor.Stub.asInterface(b);
                    return powerAdvisor;
                }
            };
}
