/*
 * Copyright (C) 2015 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.android.internal.os;

import android.system.Os;
import android.text.TextUtils;
import android.os.StrictMode;
import android.system.OsConstants;
import android.util.Slog;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.Arrays;

/**
 * Reads CPU time of a specific core spent at various frequencies and provides a delta from the
 * last call to {@link #readDelta}. Each line in the proc file has the format:
 *
 * freq time
 *
 * where time is measured in jiffies.
 */
public class KernelCpuSpeedReader {
    private static final String TAG = "KernelCpuSpeedReader";

    private final String mProcFile;
    private final int mNumSpeedSteps;
    private final long[] mLastSpeedTimesMs;
    private final long[] mDeltaSpeedTimesMs;

    // How long a CPU jiffy is in milliseconds.
    private final long mJiffyMillis;

    // Smartisan extension (PICO OS 5.13.7).
    private final KernelCpuSpeedReaderSmtEx mSmtEx = new KernelCpuSpeedReaderSmtEx(this);

    /**
     * @param cpuNumber The cpu (cpu0, cpu1, etc) whose state to read.
     */
    public KernelCpuSpeedReader(int cpuNumber, int numSpeedSteps) {
        mProcFile = String.format("/sys/devices/system/cpu/cpu%d/cpufreq/stats/time_in_state",
                cpuNumber);
        mNumSpeedSteps = numSpeedSteps;
        mLastSpeedTimesMs = new long[numSpeedSteps];
        mDeltaSpeedTimesMs = new long[numSpeedSteps];
        long jiffyHz = Os.sysconf(OsConstants._SC_CLK_TCK);
        mJiffyMillis = 1000/jiffyHz;
    }

    /**
     * The returned array is modified in subsequent calls to {@link #readDelta}.
     * @return The time (in milliseconds) spent at different cpu speeds since the last call to
     * {@link #readDelta}.
     */
    public long[] readDelta() {
        StrictMode.ThreadPolicy policy = StrictMode.allowThreadDiskReads();
        try (BufferedReader reader = new BufferedReader(new FileReader(mProcFile))) {
            TextUtils.SimpleStringSplitter splitter = new TextUtils.SimpleStringSplitter(' ');
            String line;
            int speedIndex = 0;
            while (speedIndex < mLastSpeedTimesMs.length && (line = reader.readLine()) != null) {
                splitter.setString(line);
                splitter.next();

                long time = Long.parseLong(splitter.next()) * mJiffyMillis;
                if (time < mLastSpeedTimesMs[speedIndex]) {
                    // The stats reset when the cpu hotplugged. That means that the time
                    // we read is offset from 0, so the time is the delta.
                    mDeltaSpeedTimesMs[speedIndex] = time;
                } else {
                    mDeltaSpeedTimesMs[speedIndex] = time - mLastSpeedTimesMs[speedIndex];
                }
                mLastSpeedTimesMs[speedIndex] = time;
                speedIndex++;
            }
        } catch (IOException e) {
            Slog.e(TAG, "Failed to read cpu-freq: " + e.getMessage());
            Arrays.fill(mDeltaSpeedTimesMs, 0);
        } finally {
            StrictMode.setThreadPolicy(policy);
        }
        return mDeltaSpeedTimesMs;
    }

    /**
     * @return The time (in milliseconds) spent at different cpu speeds. The values should be
     * monotonically increasing, unless the cpu was hotplugged.
     */
    public long[] readAbsolute() {
        StrictMode.ThreadPolicy policy = StrictMode.allowThreadDiskReads();
        long[] speedTimeMs = new long[mNumSpeedSteps];
        try (BufferedReader reader = new BufferedReader(new FileReader(mProcFile))) {
            TextUtils.SimpleStringSplitter splitter = new TextUtils.SimpleStringSplitter(' ');
            String line;
            int speedIndex = 0;
            while (speedIndex < mNumSpeedSteps && (line = reader.readLine()) != null) {
                splitter.setString(line);
                splitter.next();
                long time = Long.parseLong(splitter.next()) * mJiffyMillis;
                speedTimeMs[speedIndex] = time;
                speedIndex++;
            }
        } catch (IOException e) {
            Slog.e(TAG, "Failed to read cpu-freq: " + e.getMessage());
            Arrays.fill(speedTimeMs, 0);
        } finally {
            StrictMode.setThreadPolicy(policy);
        }
        return speedTimeMs;
    }

    /**
     * Smartisan constructor (PICO OS 5.13.7): the number of speed steps is the number of
     * scaling_available_frequencies entries (30 when they cannot be read). As on the factory,
     * mNumSpeedSteps is set to 1.
     * @hide
     */
    public KernelCpuSpeedReader(int cpuNumber) {
        mProcFile = String.format("/sys/devices/system/cpu/cpu%d/cpufreq/stats/time_in_state",
                cpuNumber);
        String file = String.format(
                "/sys/devices/system/cpu/cpu%d/cpufreq/scaling_available_frequencies",
                cpuNumber);
        int numSpeedSteps = 0;
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] tmp = line.trim().split(" ");
                numSpeedSteps += tmp.length;
            }
        } catch (IOException e) {
            numSpeedSteps = 30;
            Slog.e(TAG, "Failed to read scaling_available_frequencies: " + e.getMessage());
        }
        mNumSpeedSteps = 1;
        mLastSpeedTimesMs = new long[numSpeedSteps];
        mDeltaSpeedTimesMs = new long[numSpeedSteps];
        long jiffyHz = Os.sysconf(OsConstants._SC_CLK_TCK);
        mJiffyMillis = 1000 / jiffyHz;
        getSmtEx().mSpeeds = new long[numSpeedSteps];
    }

    /** @hide */
    public KernelCpuSpeedReaderSmtEx getSmtEx() {
        return mSmtEx;
    }
}
