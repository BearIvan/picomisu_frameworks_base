// Copyright 2026 Picomisu contributors
// SPDX-License-Identifier: Apache-2.0

package com.android.server.job.controllers;

import android.app.job.JobInfo;
import java.io.PrintWriter;

/**
 * Reconstructed from the PICO OS 5.13.7 factory services.
 *
 * @hide
 */
public class JobStatusOptEx {
    static final int CONSTRAINT_PARALLEL_SMTFLAG_ALL = 3;
    static final int CONSTRAINT_PARALLEL_SMTFLAG_BATTERY_NOT_LOW = 2;
    static final int CONSTRAINT_PARALLEL_SMTFLAG_CHARGING = 1;
    static final int CONSTRAINT_SCREEN_OFF_DELAY = 16;
    public static final int TRACKING_PARALLEL_BATTERY_NOT_LOW = 2;
    public static final int TRACKING_PARALLEL_CHARGING = 1;
    public static final int TRACKING_SCREEN_OFF_DELAY = 1024;
    private JobInfo job;
    private JobStatus jobStatus;
    private boolean mReadyScreenOffDelay;
    int requiredParallelConstraints;
    int satisfiedConstraintsParallel = 0;
    private int trackingParallelControllers;

    public JobStatusOptEx(JobInfo job, JobStatus jobStatus) {
        this.requiredParallelConstraints = 0;
        this.job = job;
        this.jobStatus = jobStatus;
        if (job.getJobInfoSmtEx() != null) {
            this.requiredParallelConstraints = job.getJobInfoSmtEx().getConstraintParallelFlags();
        }
    }

    public boolean isSatisfiedParallel() {
        if (!hasRequiredParallelConstrains()) {
            return true;
        }
        boolean isSatisfiedParallel = (this.satisfiedConstraintsParallel & this.requiredParallelConstraints) > 0;
        return isSatisfiedParallel;
    }

    public void appendToString(StringBuilder sb) {
        if (this.job.getJobInfoSmtEx() != null && this.job.getJobInfoSmtEx().isRequireScreenOffDelay()) {
            sb.append(" SCREENOFFDELAY");
        }
    }

    public void dumpConstraints(PrintWriter pw, int constraints) {
        if ((constraints & 16) != 0) {
            pw.print(" SCREEN_OFF_DELAY");
        }
    }

    public void dump(PrintWriter pw, String prefix) {
        pw.print(prefix);
        pw.print("Satisfied Parallel constraints:");
        dumpConstraintsParallel(pw, this.satisfiedConstraintsParallel);
        pw.println();
    }

    public void dumpTrackingParallelControllers(PrintWriter pw, String prefix) {
        if (this.trackingParallelControllers != 0) {
            pw.print(prefix);
            pw.print("Tracking Parallel by SMT:");
            if ((this.trackingParallelControllers & 1) != 0) {
                pw.print(" PARALLEL_CHARGING");
            }
            if ((this.trackingParallelControllers & 2) != 0) {
                pw.print(" PARALLEL_BATTERY_NOT_LOW");
            }
            pw.println();
        }
    }

    public boolean hasScreenOffDelayConstraint() {
        return (this.jobStatus.requiredConstraints & 16) != 0;
    }

    public boolean hasRequiredParallelConstrains() {
        return (this.requiredParallelConstraints & 3) > 0;
    }

    public boolean isScreenOffDelay() {
        if (this.job.getJobInfoSmtEx() == null) {
            return false;
        }
        return this.job.getJobInfoSmtEx().isRequireScreenOffDelay();
    }

    public boolean isRequireParallel(int require) {
        return (this.requiredParallelConstraints & require) > 0;
    }

    public long getScreenOffDelay() {
        if (this.job.getJobInfoSmtEx() == null) {
            return 0L;
        }
        return this.job.getJobInfoSmtEx().getScreenOffDelay();
    }

    boolean setScreenOffDelayConstraintSatisfied(boolean state) {
        if (this.jobStatus.setConstraintSatisfied(16, state)) {
            this.mReadyScreenOffDelay = state;
            return true;
        }
        return false;
    }

    boolean setConstraintParallelSatisfied(int constraint, boolean state) {
        boolean old = (this.satisfiedConstraintsParallel & constraint) != 0;
        if (old == state) {
            return false;
        }
        this.satisfiedConstraintsParallel = state ? this.satisfiedConstraintsParallel | constraint : this.satisfiedConstraintsParallel;
        return true;
    }

    boolean isConstraintParallelSatisfied(int constraint) {
        return (this.satisfiedConstraintsParallel & constraint) != 0;
    }

    boolean clearTrackingParallelController() {
        int i = this.trackingParallelControllers;
        int i2 = this.requiredParallelConstraints;
        if ((i & i2) != 0) {
            this.trackingParallelControllers = i & (~i2);
            return true;
        }
        return false;
    }

    void setTrackingParallelController() {
        this.trackingParallelControllers = this.requiredParallelConstraints;
    }

    void dumpConstraintsParallel(PrintWriter pw, int constraints) {
        if ((constraints & 1) != 0) {
            pw.print(" PARALLEL_CHARGING");
        }
        if ((constraints & 2) != 0) {
            pw.print(" PARALLEL_BATTERY_NOT_LOW");
        }
        if (constraints != 0) {
            pw.print(" [0x");
            pw.print(Integer.toHexString(constraints));
            pw.print("]");
        }
    }
}
