// Copyright 2026 Picomisu contributors
// SPDX-License-Identifier: Apache-2.0

package com.android.internal.os;

import android.system.ErrnoException;
import android.system.Os;
import android.util.Log;
import android.util.Pair;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Detects file descriptor leaks behind Java and native crashes and logs the most opened fd
 * path (factory PICO OS 5.13.7 {@code com.android.internal.os.FdMonitor}; the factory builds the
 * leak report but sends it nowhere, and nothing in the factory jars calls
 * {@link #uploadFdLeakIfNeed}).
 *
 * @hide
 */
public class FdMonitor {
    private static final String TAG = "FdMonitor";
    private static final String FD_LEAK_EVENT = "fd_leak";
    private static final String PROCESS = "process";
    private static final String FD = "fd";
    private static final String COUNT = "count";
    private static final String TYPE = "type";
    private static final String JAVA = "java";
    private static final String NATIVE = "native";
    private static final int FD_LEAK_MAX_COUNT = 1000;

    private static String[] FD_LEAK_MSG = {
            "Too many open files",
            "Could not allocate JNI Env",
            "pthread_create",
            "Could not allocate ashmem pixel ref.",
            "Could not allocate java pixel ref.",
            "Could not allocate dup blob fd",
            "Could not read input channel file descriptors from parcel",
            "InputChannel is not initialized",
            "MessageQueue is not initialized.",
            "Could not open input channel pair",
            "status=-24",
            "status=-31",
            "status=-40",
            "FileDescriptor must not be null"
    };

    public static void uploadFdLeakIfNeed(String eventType, String fileContents,
            String reportProcessName, int pid, String exceptionMessage) {
        if ("native_crash".equals(eventType)) {
            reportNativeFdLeakIfNeed(reportProcessName, fileContents);
        } else {
            reportJavaFdLeak(exceptionMessage, reportProcessName, pid);
        }
    }

    private static void reportNativeFdLeakIfNeed(final String reportProcessName,
            final String fileContents) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                Pair<String, Integer> pair = getNativeCrashFdleak(fileContents);
                if (pair != null) {
                    String reportfd = pair.first;
                    int reportCount = pair.second;
                    Log.e(TAG, "fd leak processName:" + reportProcessName + " fd:" + reportfd
                            + ", open count:" + reportCount + ", type:" + NATIVE);
                    reportFdLeak(reportProcessName, reportfd, reportCount, NATIVE);
                }
            }
        }).start();
    }

    private static void reportJavaFdLeak(String exceptionMessage, String reportProcessName,
            int pid) {
        if (exceptionMessage != null && isFdLeakEvent(exceptionMessage)) {
            String reportfd;
            int reportCount;
            Pair<String, Integer> pair = getJavaCrashFdleak(pid);
            if (pair != null) {
                reportfd = pair.first;
                reportCount = pair.second;
            } else {
                reportfd = exceptionMessage.replaceAll("\\d+", "X");
                reportCount = 1024;
            }
            String type = JAVA;
            Log.e(TAG, "fd leak processName:" + reportProcessName + " fd:" + reportfd
                    + ", open count:" + reportCount + ", type:" + type);
            reportFdLeak(reportProcessName, reportfd, reportCount, type);
        }
    }

    private static void reportFdLeak(String processName, String fd, int count, String type) {
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put(PROCESS, processName);
            jsonObject.put(FD, fd);
            jsonObject.put(COUNT, count);
            jsonObject.put(TYPE, type);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private static Pair<String, Integer> getNativeCrashFdleak(String fileContents) {
        if (fileContents != null && fileContents.length() > 400) {
            String fileTop = fileContents.subSequence(0, 400).toString();
            Pattern pattern = Pattern.compile(
                    "Too many open files:(.*),\\sopen count:(\\d+),\\susetime:(\\d+)ms");
            Matcher matcher = pattern.matcher(fileTop);
            if (matcher.find() && matcher.groupCount() == 3) {
                String filePath = matcher.group(1);
                int openCount = Integer.valueOf(matcher.group(2));
                return new Pair<>(filePath, openCount);
            }
        }
        return null;
    }

    private static Pair<String, Integer> getJavaCrashFdleak(int pid) {
        return getLeakFd(pid);
    }

    private static boolean isFdLeakEvent(String exceptionMessage) {
        for (String exceptionMsg : FD_LEAK_MSG) {
            if (exceptionMessage != null && exceptionMessage.contains(exceptionMsg)) {
                return true;
            }
        }
        return false;
    }

    private static Pair<String, Integer> getLeakFd(int pid) {
        File[] fds;
        if (pid != 0) {
            fds = new File("/proc/" + pid + "/fd").listFiles();
        } else {
            fds = new File("/proc/self/fd").listFiles();
        }
        if (fds == null || fds.length == 0) {
            Log.e(TAG, "failed to read fds!");
            return null;
        }
        HashMap<String, Integer> fdMap = new HashMap<>();
        if (fds.length >= FD_LEAK_MAX_COUNT) {
            for (File fd : fds) {
                String fd_path = fd.getAbsolutePath();
                String linkPath = null;
                try {
                    linkPath = Os.readlink(fd_path);
                    linkPath = linkPath.replaceAll("\\d+", "X");
                    if (fdMap.containsKey(linkPath)) {
                        int count = fdMap.get(linkPath);
                        fdMap.put(linkPath, count + 1);
                    } else {
                        fdMap.put(linkPath, 0);
                    }
                } catch (ErrnoException e) {
                    e.printStackTrace();
                }
            }
        }
        return getLeakFd(fdMap);
    }

    private static Pair<String, Integer> getLeakFd(HashMap<String, Integer> fdMap) {
        String fdString = null;
        int fdCount = 0;
        for (Map.Entry<String, Integer> entry : fdMap.entrySet()) {
            Integer val = entry.getValue();
            if (val > fdCount) {
                fdString = entry.getKey();
                fdCount = val;
            }
        }
        if (fdCount != 0 && fdString != null) {
            return new Pair<>(fdString, fdCount);
        }
        return null;
    }
}
