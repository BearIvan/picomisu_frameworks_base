// Copyright 2026 Picomisu contributors
// SPDX-License-Identifier: Apache-2.0

package android.app.job;

import android.util.Log;

/**
 * Smartisan extension of {@link JobInfo}: screen-off delay and parallel (charging / battery
 * not low) constraints. Reconstructed from the PICO OS 5.13.7 factory framework.
 *
 * @hide
 */
public class JobInfoSmtEx {
    static final String TAG = "JobInfoSmtEx";

    public static final int CONSTRAINT_FLAG_SCREEN_OFF_DELAY = 1 << 4;

    public static final int CONSTRAINT_PARALLEL_SMTFLAG_CHARGING = 1 << 0;
    public static final int CONSTRAINT_PARALLEL_SMTFLAG_BATTERY_NOT_LOW = 1 << 1;
    public static final int CONSTRAINT_PARALLEL_SMTFLAG_ALL =
            CONSTRAINT_PARALLEL_SMTFLAG_CHARGING | CONSTRAINT_PARALLEL_SMTFLAG_BATTERY_NOT_LOW;

    private final int constraintParallelFlags;
    private final int constraintFlagsExt;
    private final long screenOffTime;

    public JobInfoSmtEx(BuilderSmtEx bEx) {
        constraintParallelFlags = bEx.mConstraintParallelFlags;
        constraintFlagsExt = bEx.mConstraintFlagsExt;
        screenOffTime = bEx.mScreenOffTime;
    }

    public boolean isRequireScreenOffDelay() {
        return (constraintFlagsExt & CONSTRAINT_FLAG_SCREEN_OFF_DELAY) != 0;
    }

    public int getConstraintParallelFlags() {
        return constraintParallelFlags;
    }

    public int getConstraintFlagsExt() {
        return constraintFlagsExt;
    }

    public long getScreenOffDelay() {
        return screenOffTime;
    }

    public boolean hasParallelRequirement() {
        return constraintParallelFlags > 0;
    }

    /** @hide */
    public static final class BuilderSmtEx {
        // As on the factory, the last Builder is kept in a static field.
        private static JobInfo.Builder mBuilder;
        public static BuilderSmtEx mBuilderSmtEx;
        public int mConstraintParallelFlags = 0;
        public long mScreenOffTime = 0;
        public int mConstraintFlagsExt = 0;

        public BuilderSmtEx(JobInfo.Builder b) {
            mBuilder = b;
        }

        public BuilderSmtEx setScreenOffDelay(long time) {
            mConstraintFlagsExt = (mConstraintFlagsExt & ~CONSTRAINT_FLAG_SCREEN_OFF_DELAY)
                    | CONSTRAINT_FLAG_SCREEN_OFF_DELAY;
            if (time < 0) {
                return this;
            }
            mScreenOffTime = time;
            return this;
        }

        public BuilderSmtEx setSmtParallelRequire(int parallelRequirement) {
            boolean require = (parallelRequirement & ~CONSTRAINT_PARALLEL_SMTFLAG_ALL) == 0;
            if (!require) {
                Log.e(TAG, " smt parallel requirement is not support");
                return this;
            }
            mConstraintParallelFlags = parallelRequirement;
            return this;
        }

        public JobInfoSmtEx build() {
            return new JobInfoSmtEx(this);
        }
    }
}
