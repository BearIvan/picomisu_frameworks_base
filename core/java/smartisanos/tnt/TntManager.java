// Copyright 2026 Picomisu contributors
// SPDX-License-Identifier: Apache-2.0

package smartisanos.tnt;

import android.app.ActivityManager;
import android.content.res.Configuration;
import android.pc.ISmtPCManager;
import android.view.View;

import java.util.List;

/**
 * TNT (Smartisan desktop mode) client; every method is a stub on the factory
 * (factory PICO OS 5.13.7 {@code smartisanos.tnt.TntManager}).
 *
 * @hide
 */
public class TntManager {
    public static final int PC_MODE_NONE = 0;

    public static final int RUNNING_TASK_FLAG_ONLY_TNT_TASK = 1;
    public static final int RUNNING_TASK_FLAG_ONLY_VISIBLE_TASK = 1 << 1;

    public @interface RunningTaskFlag {
    }

    private static TntManager sInstance = new TntManager();
    private static ISmtPCManager sService;

    public static TntManager getInstance() {
        return sInstance;
    }

    private TntManager() {
    }

    private static ISmtPCManager getTntService() {
        return null;
    }

    public boolean isPcMode(Configuration configuration) {
        return false;
    }

    public boolean setPinStack(View view, boolean pin) {
        return false;
    }

    public void setPinStack(int taskId, boolean pin) {
    }

    public boolean isInPinStack(View view) {
        return false;
    }

    public List<ActivityManager.RunningTaskInfo> getTntVisibleRunningTasks(int maxNum) {
        return null;
    }

    public List<ActivityManager.RunningTaskInfo> getRunningTasks(int maxNum,
            @RunningTaskFlag int flags) {
        return null;
    }
}
