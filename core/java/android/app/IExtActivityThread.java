// Copyright 2026 Picomisu contributors
// SPDX-License-Identifier: Apache-2.0
package android.app;

import android.content.pm.ApplicationInfo;
import android.content.res.CompatibilityInfo;
import android.content.res.Configuration;
import android.view.ViewRootImpl;
import com.pico.util.IExtBase;

/**
 * PICO activity-thread extension: VR force-render query, 2D virtual-display context and
 * display ID, launch loading UI, configuration hooks and WebView lifecycle callbacks.
 * @hide
 */
public interface IExtActivityThread extends IExtBase {
    ContextImpl adjustCreateBaseContextForActivity(ContextImpl appContext);
    Object[] collectActivityLifecycleCallbacks();
    int getDisplayId();
    void handleConfigurationChanged(Configuration config, CompatibilityInfo compat);
    boolean isActivityForceRender(ViewRootImpl viewRoot);
    boolean notifyAppLaunchStatus(ActivityThread.ActivityClientRecord acr);
    void onBindApplication(ApplicationInfo appInfo, ActivityThread.AppBindData data);
    void registerWebViewActivityLifecycleCallbacks(
            Application.ActivityLifecycleCallbacks callback);
    void unregisterWebViewActivityLifecycleCallbacks(
            Application.ActivityLifecycleCallbacks callback);
}
