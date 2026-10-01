// Copyright 2026 Picomisu contributors
// SPDX-License-Identifier: Apache-2.0

package com.android.server;

/**
 * Reconstructed from the PICO OS 5.13.7 factory services.
 *
 * @hide
 */
public class CpuUsageRecord extends UploadEvent {
    public String name;
    public long user = 0;
    public long nice = 0;
    public long system = 0;
    public long idle = 0;
    public long iowait = 0;
    public long irq = 0;
    public long softirq = 0;
    public long dailyUser = 0;
    public long dailyNice = 0;
    public long dailySystem = 0;
    public long dailyIdle = 0;
    public long dailyIowait = 0;
    public long dailyIrq = 0;
    public long dailySoftirq = 0;

    public CpuUsageRecord(String name) {
        this.name = name;
    }

    public void updateCpuUsage(long user, long nice, long system, long idle, long iowait, long irq, long softirq) {
        this.dailyUser = user - this.user;
        this.dailyNice = nice - this.nice;
        this.dailySystem = system - this.system;
        this.dailyIdle = idle - this.idle;
        this.dailyIowait = iowait - this.iowait;
        this.dailyIrq = irq - this.irq;
        this.dailySoftirq = softirq - this.dailySoftirq;
        this.user = user;
        this.nice = nice;
        this.system = system;
        this.idle = idle;
        this.iowait = iowait;
        this.irq = irq;
        this.softirq = softirq;
    }

    public String toString() {
        return this.name + "|" + this.dailyUser + "|" + this.dailyNice + "|" + this.dailySystem + "|" + this.dailyIdle + "|" + this.dailyIowait + "|" + this.dailyIrq + "|" + this.dailySoftirq + "|" + this.user + "|" + this.nice + "|" + this.system + "|" + this.idle + "|" + this.iowait + "|" + this.irq + "|" + this.softirq;
    }
}
