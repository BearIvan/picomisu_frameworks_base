// Copyright 2026 Picomisu contributors
// SPDX-License-Identifier: Apache-2.0

package com.android.server.wm;

/**
 * Reconstructed from the PICO OS 5.13.7 factory services.
 *
 * @hide
 */
public class TaskRecordSmtBase extends ConfigurationContainerSmtBase {
    private TaskRecord task;
    private boolean taskInVisible;

    public TaskRecordSmtBase(ConfigurationContainer configurationContainer) {
        super(configurationContainer);
        this.task = (TaskRecord) configurationContainer;
    }

    public boolean isTaskInVisible() {
        return this.taskInVisible;
    }

    public void setTaskInVisible(boolean isVisible) {
        this.taskInVisible = isVisible;
    }

    public boolean isSystemTask() {
        TaskRecord taskRecord = this.task;
        return (taskRecord == null || taskRecord.getRootActivity() == null || this.task.getRootActivity().info == null || this.task.getRootActivity().info.applicationInfo == null || (this.task.getRootActivity().info.applicationInfo.flags & 129) == 0) ? false : true;
    }
}
