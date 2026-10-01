// Copyright 2026 Picomisu contributors
// SPDX-License-Identifier: Apache-2.0

package com.android.internal.os;

import android.text.TextUtils;
import android.util.Slog;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

/**
 * Smartisan extension of {@link KernelCpuSpeedReader}. Reconstructed from the PICO OS 5.13.7
 * factory framework.
 *
 * @hide
 */
public class KernelCpuSpeedReaderSmtEx {
    private static final String TAG = "KernelCpuSpeedReader";

    private KernelCpuSpeedReader mKernelCpuSpeedReader;
    long[] mSpeeds;

    public KernelCpuSpeedReaderSmtEx(KernelCpuSpeedReader kernelCpuSpeedReader) {
        mKernelCpuSpeedReader = kernelCpuSpeedReader;
    }

    public long[] getSpeeds() {
        return mSpeeds;
    }

    public static int getNumberofCurCores() {
        try (BufferedReader reader = new BufferedReader(
                new FileReader("/sys/devices/system/cpu/online"))) {
            String line = reader.readLine();
            if (line == null) {
                return -1;
            }
            if (line.contains(",")) {
                TextUtils.SimpleStringSplitter splitterComma =
                        new TextUtils.SimpleStringSplitter(',');
                splitterComma.setString(line);
                String front = splitterComma.next();
                int startFront = 0;
                int endFront = 0;
                if (front.contains("-")) {
                    TextUtils.SimpleStringSplitter splitterLine =
                            new TextUtils.SimpleStringSplitter('-');
                    splitterLine.setString(front);
                    startFront = Integer.parseInt(splitterLine.next());
                    endFront = Integer.parseInt(splitterLine.next());
                }
                String behind = splitterComma.next();
                int startBehind = 0;
                int endBehind = 0;
                if (behind.contains("-")) {
                    TextUtils.SimpleStringSplitter splitterLine =
                            new TextUtils.SimpleStringSplitter('-');
                    splitterLine.setString(behind);
                    startBehind = Integer.parseInt(splitterLine.next());
                    endBehind = Integer.parseInt(splitterLine.next());
                }
                return (endFront - startFront + 1) + (endBehind - startBehind + 1);
            } else {
                TextUtils.SimpleStringSplitter splitterLine =
                        new TextUtils.SimpleStringSplitter('-');
                splitterLine.setString(line);
                int start = Integer.parseInt(splitterLine.next());
                int end = Integer.parseInt(splitterLine.next());
                return end - start + 1;
            }
        } catch (Exception e) {
            Slog.e(TAG, "Failed to read online cpus: " + e.getMessage());
            return -1;
        }
    }

    public static int getNumberofCores() {
        try (BufferedReader reader = new BufferedReader(
                new FileReader("/sys/devices/system/cpu/possible"))) {
            TextUtils.SimpleStringSplitter splitter = new TextUtils.SimpleStringSplitter('-');
            String line;
            if ((line = reader.readLine()) != null) {
                splitter.setString(line);
                Integer.parseInt(splitter.next());
                return Integer.parseInt(splitter.next()) + 1;
            }
        } catch (IOException e) {
            Slog.e(TAG, "Failed to read possible cpus: " + e.getMessage());
        }
        return 1;
    }
}
