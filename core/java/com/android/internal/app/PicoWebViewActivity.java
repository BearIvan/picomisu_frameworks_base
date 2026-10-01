// Copyright 2026 Picomisu contributors
// SPDX-License-Identifier: Apache-2.0
package com.android.internal.app;

import android.app.Activity;
import android.app.ActivityThread;
import android.app.Application;
import android.os.Bundle;
import android.util.Log;

/**
 * PICO WebXR activity (factory PICO OS 5.13.7 com.android.internal.app.PicoWebViewActivity).
 * {@link android.webkit.WebView#startWebViewActivity} starts it in the app's own package (the
 * package manager resolves the component to a synthesized VR activity); its lifecycle is forwarded
 * to the callbacks registered through the activity thread extension.
 * @hide
 */
public class PicoWebViewActivity extends Activity {
    private static final String TAG = "PicoWebViewActivity";

    public PicoWebViewActivity() {
        registerActivityLifecycleCallbacks(new Application.ActivityLifecycleCallbacks() {
            @Override
            public void onActivityCreated(Activity activity, Bundle savedInstanceState) {
                Log.d(TAG, "onActivityCreated" + activity);
                Object[] callbacks =
                        ActivityThread.currentActivityThread().getExt()
                                .collectActivityLifecycleCallbacks();
                if (callbacks != null) {
                    for (Object obj : callbacks) {
                        ((Application.ActivityLifecycleCallbacks) obj)
                                .onActivityCreated(activity, savedInstanceState);
                    }
                }
            }

            @Override
            public void onActivityStarted(Activity activity) {
                Log.d(TAG, "onActivityStarted" + activity);
                Object[] callbacks =
                        ActivityThread.currentActivityThread().getExt()
                                .collectActivityLifecycleCallbacks();
                if (callbacks != null) {
                    for (Object obj : callbacks) {
                        ((Application.ActivityLifecycleCallbacks) obj).onActivityStarted(activity);
                    }
                }
            }

            @Override
            public void onActivityResumed(Activity activity) {
                Log.d(TAG, "onActivityResumed" + activity);
                Object[] callbacks =
                        ActivityThread.currentActivityThread().getExt()
                                .collectActivityLifecycleCallbacks();
                if (callbacks != null) {
                    for (Object obj : callbacks) {
                        ((Application.ActivityLifecycleCallbacks) obj).onActivityResumed(activity);
                    }
                }
            }

            @Override
            public void onActivityPaused(Activity activity) {
                Log.d(TAG, "onActivityPaused" + activity);
                Object[] callbacks =
                        ActivityThread.currentActivityThread().getExt()
                                .collectActivityLifecycleCallbacks();
                if (callbacks != null) {
                    for (Object obj : callbacks) {
                        ((Application.ActivityLifecycleCallbacks) obj).onActivityPaused(activity);
                    }
                }
            }

            @Override
            public void onActivityStopped(Activity activity) {
                Log.d(TAG, "onActivityStopped" + activity);
                Object[] callbacks =
                        ActivityThread.currentActivityThread().getExt()
                                .collectActivityLifecycleCallbacks();
                if (callbacks != null) {
                    for (Object obj : callbacks) {
                        ((Application.ActivityLifecycleCallbacks) obj).onActivityStopped(activity);
                    }
                }
            }

            @Override
            public void onActivitySaveInstanceState(Activity activity, Bundle outState) {
                Log.d(TAG, "onActivitySaveInstanceState" + activity);
                Object[] callbacks =
                        ActivityThread.currentActivityThread().getExt()
                                .collectActivityLifecycleCallbacks();
                if (callbacks != null) {
                    for (Object obj : callbacks) {
                        ((Application.ActivityLifecycleCallbacks) obj)
                                .onActivitySaveInstanceState(activity, outState);
                    }
                }
            }

            @Override
            public void onActivityDestroyed(Activity activity) {
                Log.d(TAG, "onActivityDestroyed" + activity);
                Object[] callbacks =
                        ActivityThread.currentActivityThread().getExt()
                                .collectActivityLifecycleCallbacks();
                if (callbacks != null) {
                    for (Object obj : callbacks) {
                        ((Application.ActivityLifecycleCallbacks) obj)
                                .onActivityDestroyed(activity);
                    }
                }
            }
        });
    }
}
