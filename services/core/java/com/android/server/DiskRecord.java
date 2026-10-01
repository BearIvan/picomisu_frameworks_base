// Copyright 2026 Picomisu contributors
// SPDX-License-Identifier: Apache-2.0

package com.android.server;

import android.os.Environment;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.util.Slog;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Reconstructed from the PICO OS 5.13.7 factory services.
 *
 * @hide
 */
public class DiskRecord {
    private static final boolean DBG;
    private static final String DISK_STATS_NODE = "/proc/diskstats";
    private static final int ONE_DAY_MILLIS = 82800000;
    private static final String TAG = "DiskRecord";
    private static final String VM_STATS_NODE = "/proc/vmstat";
    private static long bootTime;
    private static boolean entireDay;
    private static long[] oldStats;
    public long dataFreeSize;
    public long dataTotalSize;

    static {
        DBG = SystemProperties.getInt("ro.debuggable", 0) != 0;
        oldStats = new long[32];
        bootTime = 0L;
    }

    public String toString() {
        return this.dataTotalSize + "|" + this.dataFreeSize;
    }

    public JSONObject toJSON() {
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("data_size", this.dataTotalSize);
            jsonObject.put("data_free_size", this.dataFreeSize);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return jsonObject;
    }

    public static DiskRecord readDiskInfo() {
        DiskRecord diskRecord = new DiskRecord();
        diskRecord.dataTotalSize = Environment.getDataDirectory().getTotalSpace();
        diskRecord.dataFreeSize = Environment.getDataDirectory().getFreeSpace();
        return diskRecord;
    }

    static void init() {
        long[] rw = readDiskStatsFile();
        long[] vm = readVmStatFile();
        for (int i = 0; i < rw.length; i++) {
            oldStats[i] = rw[i];
        }
        int len = rw.length;
        for (int i2 = 0; i2 < vm.length; i2++) {
            oldStats[i2 + len] = vm[i2];
        }
        bootTime = SystemClock.elapsedRealtime();
    }

    static long[] readDiskRwStats() {
        long[] rwSectors = readDiskStatsFile();
        long[] vmStats = readVmStatFile();
        long[] ret = new long[16];
        if (!entireDay) {
            entireDay = SystemClock.elapsedRealtime() - bootTime >= 82800000;
        }
        for (int i = 0; i < rwSectors.length; i++) {
            if (entireDay) {
                ret[i] = rwSectors[i] - oldStats[i];
            }
            oldStats[i] = rwSectors[i];
        }
        int i2 = rwSectors.length;
        for (int i3 = 0; i3 < vmStats.length; i3++) {
            if (entireDay) {
                ret[i3 + i2] = vmStats[i3] - oldStats[i3 + i2];
            }
            oldStats[i3 + i2] = vmStats[i3];
        }
        if (DBG) {
            print(ret);
        }
        return ret;
    }

    private static void print(long[] data) {
        StringBuilder sb = new StringBuilder("disk stats: ");
        for (long j : data) {
            sb.append(j);
            sb.append(" ");
        }
        Slog.d(TAG, sb.toString());
    }

    private static long[] readVmStatFile() {
        long[] vmFields = new long[1];
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(VM_STATS_NODE)));
            while (true) {
                try {
                    String line = reader.readLine();
                    if (line == null) {
                        break;
                    }
                    boolean parsePswpOut = line.contains("pswpout ");
                    if (parsePswpOut) {
                        String line2 = line.trim();
                        if (DBG) {
                            Slog.w(TAG, "line" + line2);
                        }
                        String[] fields = line2.split("\\s+");
                        if (DBG) {
                            for (String field : fields) {
                                Slog.w(TAG, "field:" + field);
                            }
                        }
                        if (parsePswpOut) {
                            vmFields[0] = Long.valueOf(fields[1]).longValue();
                        }
                    }
                } catch (Throwable th) {
                    try {
                        throw th;
                    } catch (Throwable th2) {
                        $closeResource(th, reader);
                        throw th2;
                    }
                }
            }
            $closeResource(null, reader);
        } catch (Exception e) {
            Slog.w(TAG, "vm stats failed", e);
        }
        StringBuilder sb = new StringBuilder();
        for (long j : vmFields) {
            sb.append(j);
            sb.append(" ");
        }
        Slog.i(TAG, sb.toString());
        return vmFields;
    }

    private static void $closeResource(Throwable x0, AutoCloseable x1) throws Exception {
        if (x0 == null) {
            x1.close();
            return;
        }
        try {
            x1.close();
        } catch (Throwable th) {
            x0.addSuppressed(th);
        }
    }

    private static long[] readDiskStatsFile() {
        long[] diskFields = new long[3];
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(DISK_STATS_NODE)));
            while (true) {
                try {
                    String line = reader.readLine();
                    if (line == null) {
                        break;
                    }
                    boolean parseSda = line.contains("sda ");
                    boolean parseZram = line.contains("zram0");
                    if (parseSda || parseZram) {
                        String line2 = line.trim();
                        if (DBG) {
                            Slog.w(TAG, "line" + line2);
                        }
                        String[] fields = line2.split("\\s+");
                        if (DBG) {
                            for (String field : fields) {
                                Slog.w(TAG, "field:" + field);
                            }
                        }
                        if (parseSda) {
                            diskFields[0] = Long.valueOf(fields[5]).longValue();
                            diskFields[1] = Long.valueOf(fields[9]).longValue();
                        } else if (parseZram) {
                            diskFields[2] = Long.valueOf(fields[9]).longValue();
                        }
                    }
                } catch (Throwable th) {
                    try {
                        throw th;
                    } catch (Throwable th2) {
                        $closeResource(th, reader);
                        throw th2;
                    }
                }
            }
            $closeResource(null, reader);
        } catch (Exception e) {
            Slog.w(TAG, "disk stats failed", e);
        }
        StringBuilder sb = new StringBuilder();
        for (long j : diskFields) {
            sb.append(j);
            sb.append(" ");
        }
        Slog.i(TAG, sb.toString());
        return diskFields;
    }
}
